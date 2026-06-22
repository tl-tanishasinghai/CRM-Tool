package com.trillionloans.los.service.ckyc;

import com.trillionloans.los.api.partner.SyntizenApi;
import com.trillionloans.los.constant.RelationshipType;
import com.trillionloans.los.model.partner.m2p.M2pFamilyDetailsDTO;
import com.trillionloans.los.model.request.ckyc.SyntizenCkycSearchRequest;
import com.trillionloans.los.model.response.ckyc.SyntizenCkycSearchResponse;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Service
@AllArgsConstructor
public class SyntizenCkycService {

  private final SyntizenApi syntizenApi;
  private final AadhaarCkycService aadhaarCkycService;

  public Mono<SyntizenCkycSearchResponse> getCkycSearchResponse(String documentKey) {
    String encodedId = Base64.getEncoder().encodeToString(documentKey.getBytes());

    return syntizenApi.generateCkycSearchResponse(
        SyntizenCkycSearchRequest.builder()
            .rrn(UUID.randomUUID())
            .idType("PAN")
            .idNumber(encodedId)
            .build());
  }

  public Mono<Optional<M2pFamilyDetailsDTO>> getFatherName(
      String documentKey, Context reactorContext) {
    return getCkycSearchResponse(documentKey)
        .contextWrite(reactorContext)
        .map(
            response -> {
              if (response != null) {
                String fatherName = response.getFather_name();
                if (fatherName != null && !fatherName.isBlank()) {
                  List<String> fathersName = aadhaarCkycService.splitName(fatherName);
                  return Optional.of(
                      M2pFamilyDetailsDTO.builder()
                          .firstName(fathersName.get(0))
                          .lastName(fathersName.get(1))
                          .relationship(RelationshipType.FATHER.getDisplayName())
                          .build());
                }
                log.warn("[SYNTIZEN_CKYC_SEARCH] dependent details not found");
              }
              return Optional.<M2pFamilyDetailsDTO>empty();
            })
        .defaultIfEmpty(Optional.empty());
  }
}
