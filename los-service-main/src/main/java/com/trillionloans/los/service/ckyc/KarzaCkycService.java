package com.trillionloans.los.service.ckyc;

import com.trillionloans.los.api.partner.KarzaApi;
import com.trillionloans.los.constant.RelationshipType;
import com.trillionloans.los.model.partner.m2p.M2pFamilyDetailsDTO;
import com.trillionloans.los.model.request.ckyc.KarzaCkycSearchRequest;
import com.trillionloans.los.model.response.ckyc.KarzaCkycSearchResponse;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Service
@AllArgsConstructor
public class KarzaCkycService {

  private final KarzaApi karzaApi;
  private final AadhaarCkycService aadhaarCkycService;

  private Mono<KarzaCkycSearchResponse> getCkycSearchResponse(String documentKey) {
    return karzaApi.fetchPanProfile(KarzaCkycSearchRequest.builder().pan(documentKey).build());
  }

  public Mono<Optional<M2pFamilyDetailsDTO>> getFatherName(String documentKey) {
    return getFatherName(documentKey, Context.empty());
  }

  public Mono<Optional<M2pFamilyDetailsDTO>> getFatherName(
      String documentKey, Context reactorContext) {
    return getCkycSearchResponse(documentKey)
        .contextWrite(reactorContext)
        .map(
            response -> {
              if (response != null && response.getResult() != null) {
                String fatherName = response.getResult().getFatherName();

                if (fatherName != null && !fatherName.isBlank()) {
                  List<String> fathersName = aadhaarCkycService.splitName(fatherName);
                  return Optional.of(
                      M2pFamilyDetailsDTO.builder()
                          .firstName(fathersName.get(0))
                          .lastName(fathersName.get(1))
                          .relationship(RelationshipType.FATHER.getDisplayName())
                          .build());
                }
                log.warn("KARZA_PAN_PROFILE - Dependent details not found");
              }
              log.warn("KARZA_PAN_PROFILE - Data not found");

              return Optional.<M2pFamilyDetailsDTO>empty();
            })
        .defaultIfEmpty(Optional.empty());
  }
}
