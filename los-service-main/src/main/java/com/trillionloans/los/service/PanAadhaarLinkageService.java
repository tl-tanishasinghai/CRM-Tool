package com.trillionloans.los.service;

import com.trillionloans.los.model.entity.PanAadhaarLinkageEntity;
import com.trillionloans.los.repository.PanAadhaarLinkageRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PanAadhaarLinkageService {

  private final PanAadhaarLinkageRepository panAadhaarLinkageRepository;

  /**
   * Saves the PAN-Aadhaar linkage entity to the database.
   *
   * @param pan masked PAN number
   * @param aadhaar Aadhaar ID or prefix
   * @param productCode product code
   * @param clientId client ID
   * @param loanId loan application ID
   * @param linked linkage status
   * @param kycType KYC type (e.g., OKYC, DIGIO)
   * @return Mono<PanAadhaarLinkageEntity> the saved entity
   */
  public Mono<PanAadhaarLinkageEntity> savePanAadhaarLinkage(
      String pan,
      String aadhaar,
      String productCode,
      String clientId,
      String loanId,
      String linked,
      String kycType) {

    PanAadhaarLinkageEntity entity =
        PanAadhaarLinkageEntity.builder()
            .pan(pan)
            .aadhaar(aadhaar)
            .productCode(productCode)
            .clientId(clientId)
            .loanId(loanId)
            .linked(linked)
            .kycType(kycType)
            .createdAt(LocalDateTime.now())
            .build();

    return panAadhaarLinkageRepository
        .save(entity)
        .doOnSuccess(
            savedEntity ->
                log.info(
                    "[PAN_AADHAAR_LINKAGE] Successfully saved PAN-Aadhaar linkage for loanId: {},"
                        + " clientId: {}, kycType: {}, linked: {}",
                    loanId,
                    clientId,
                    kycType,
                    linked));
  }
}
