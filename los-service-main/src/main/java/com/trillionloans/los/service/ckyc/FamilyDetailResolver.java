package com.trillionloans.los.service.ckyc;

import static com.trillionloans.los.constant.StringConstants.*;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.dto.AadhaarXmlDetailsDTO;
import com.trillionloans.los.model.dto.ClientDependentDTO;
import com.trillionloans.los.model.partner.m2p.M2pFamilyDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/*
    This service will act as a facade to fetch Family Details required for CKYC.
*/

@Service
@Slf4j
@AllArgsConstructor
public class FamilyDetailResolver {

  private final AadhaarCkycService aadhaarCkycService;

  private final SyntizenCkycService syntizenCkycService;

  private final KarzaCkycService karzaCkycService;

  private final Environment environment;

  private final M2PWrapperApi m2PWrapperApi;

  private boolean isEnabled() {
    return "enable".equals(environment.getProperty("store.codependent.flag"));
  }

  public Mono<Optional<M2pFamilyDetailsDTO>> resolve(
      AadhaarXmlDetailsDTO aadhaarXmlDetailsDTO, String clientId) {

    if (!isEnabled()) {
      return Mono.just(Optional.empty());
    }

    String traceId = MDC.get(TRACE_ID);
    String partnerId = MDC.get(PARTNER_ID);
    Context reactorContext = buildReactorContext(traceId, partnerId);

    return Mono.defer(
            () -> {
              try {
                Optional<M2pFamilyDetailsDTO> aadhaarCkyc =
                    aadhaarCkycService.fetchFamilyDetails(aadhaarXmlDetailsDTO);
                if (aadhaarCkyc.isPresent()) {
                  return buildLeadUpdateDTO(aadhaarCkyc.get(), clientId, reactorContext)
                      .thenReturn(aadhaarCkyc);
                }
              } catch (Exception e) {
                log.warn(
                    "{} extract family details from aadhaar failed for client {} : {}",
                    CKYC_UPDATE,
                    clientId,
                    e.getMessage(),
                    e);
              }
              return Mono.just(Optional.<M2pFamilyDetailsDTO>empty());
            })
        .flatMap(
            result -> {
              if (result.isPresent()) {
                return Mono.just(result);
              }
              return tryPanBasedLookups(clientId, reactorContext);
            })
        .doOnNext(
            result -> {
              if (result.isEmpty()) {
                log.error(
                    "{} failed to fetch family details for clientId: {}", CKYC_UPDATE, clientId);
              }
            });
  }

  /*
  Fallback after aadhaar parsing.
  Try Pan Based lookups via external vendors: Syntizen & Karza
  */
  private Mono<Optional<M2pFamilyDetailsDTO>> tryPanBasedLookups(
      String clientId, Context reactorContext) {
    return m2PWrapperApi
        .getPanAadhaarDetailsByClientId(clientId)
        .contextWrite(reactorContext)
        .flatMap(
            panNumberData -> {
              if (panNumberData == null) {
                log.warn("pan not found for clientId: {}", clientId);
                return Mono.just(Optional.<M2pFamilyDetailsDTO>empty());
              }
              return tryExternalVendors(panNumberData.getPanNumber(), clientId, reactorContext);
            })
        .onErrorResume(
            e -> {
              log.warn("pan resolution failed for client {} : {}", clientId, e.getMessage(), e);
              return Mono.just(Optional.empty());
            });
  }

  private Mono<Optional<M2pFamilyDetailsDTO>> tryExternalVendors(
      String panNumber, String clientId, Context reactorContext) {
    return syntizenCkycService
        .getFatherName(panNumber, reactorContext)
        .flatMap(
            syntizenResult -> {
              if (syntizenResult.isPresent()) {
                return buildLeadUpdateDTO(syntizenResult.get(), clientId, reactorContext)
                    .thenReturn(syntizenResult);
              }
              return Mono.just(Optional.<M2pFamilyDetailsDTO>empty());
              // return tryKarza(panNumber, clientId, reactorContext); // Karza disabled temporarily
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "{} fetch family details from syntizen failed for client {} : {}",
                  CKYC_UPDATE,
                  clientId,
                  e.getMessage(),
                  e);
              return Mono.just(Optional.empty());
              // return tryKarza(panNumber, clientId, reactorContext); // Karza disabled temporarily
            });
  }

  private Mono<Optional<M2pFamilyDetailsDTO>> tryKarza(
      String panNumber, String clientId, Context reactorContext) {
    return karzaCkycService
        .getFatherName(panNumber, reactorContext)
        .flatMap(
            karzaResult -> {
              if (karzaResult.isPresent()) {
                return buildLeadUpdateDTO(karzaResult.get(), clientId, reactorContext)
                    .thenReturn(karzaResult);
              }
              return Mono.just(Optional.<M2pFamilyDetailsDTO>empty());
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "{} fetch family details from karza failed for client {} : {}",
                  CKYC_UPDATE,
                  clientId,
                  e.getMessage(),
                  e);
              return Mono.just(Optional.empty());
            });
  }

  private Context buildReactorContext(String traceId, String partnerId) {
    Context context = Context.empty();
    if (traceId != null) {
      context = context.put(TRACE_ID, traceId);
    }
    if (partnerId != null) {
      context = context.put(PARTNER_ID, partnerId);
    }
    return context;
  }

  private Mono<Void> buildLeadUpdateDTO(
      M2pFamilyDetailsDTO familyDetails, String leadId, Context reactorContext) {
    M2pLeadUpdateDTO m2pLeadUpdateDTO =
        M2pLeadUpdateDTO.builder()
            .familyDetailsData(List.of(familyDetails))
            .locale("en")
            .dateFormat(DATE_FORMAT)
            .build();

    return updateFamilyDetail(m2pLeadUpdateDTO, familyDetails, leadId).contextWrite(reactorContext);
  }

  private Mono<Void> updateFamilyDetail(
      M2pLeadUpdateDTO m2pLeadUpdateDTO, M2pFamilyDetailsDTO familyDetails, String leadId) {

    ClientDependentDTO clientDependentDTO =
        ClientDependentDTO.builder()
            .dependentName(buildFullName(familyDetails.getFirstName(), familyDetails.getLastName()))
            .relationship(familyDetails.getRelationship())
            .creationDate(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .build();

    return m2PWrapperApi
        .updateLead(m2pLeadUpdateDTO, leadId)
        .flatMap(r -> m2PWrapperApi.addClientDependentDataTable(clientDependentDTO, leadId))
        .doOnSuccess(
            r ->
                log.info(
                    "[STORE_CLIENT_DEPENDENT] successfully stored co-dependent for lead {}",
                    leadId))
        .onErrorResume(
            ex -> {
              log.error(
                  "[STORE_CLIENT_DEPENDENT] failed to store co-dependent lead {}: {}",
                  leadId,
                  ex.getMessage());
              return Mono.empty();
            })
        .then();
  }

  private String buildFullName(String firstName, String lastName) {
    return Stream.of(firstName, lastName)
        .filter(Objects::nonNull)
        .filter(s -> !s.isBlank())
        .collect(Collectors.joining(" "));
  }
}
