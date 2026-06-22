package com.trillionloans.los.service;

import com.trillionloans.los.model.entity.KycQcEntity;
import com.trillionloans.los.repository.KycQcRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Service
@Slf4j
public class KycQcService {
  private final KycQcRepository kycQcRepository;

  public Mono<KycQcEntity> save(KycQcEntity kycQcEntity) {
    return kycQcRepository.save(kycQcEntity);
  }

  public Mono<KycQcEntity> findByLoanIdWithFinalizedStatuses(String loanId) {
    log.info("[KYC_QC] Fetching KycQc record with finalized statuses for loanId: {}", loanId);
    return kycQcRepository
        .findByLoanIdWithFinalizedStatuses(loanId)
        .doOnNext(
            entity ->
                log.info(
                    "[KYC_QC] Found existing KycQc record for loanId: {}, nameStatus: {},"
                        + " faceStatus: {}",
                    loanId,
                    entity.getFinalNameMatchStatus(),
                    entity.getFinalFaceMatchStatus()))
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC] Error fetching KycQc record for loanId: {}: {}",
                    loanId,
                    error.getMessage()));
  }
}
