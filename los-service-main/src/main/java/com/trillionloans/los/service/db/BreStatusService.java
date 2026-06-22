package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.APPROVED;
import static com.trillionloans.los.constant.StringConstants.APPROVED_FI;
import static com.trillionloans.los.constant.StringConstants.BRE;
import static com.trillionloans.los.constant.StringConstants.COMPLETED;
import static com.trillionloans.los.constant.StringConstants.DECLINED;
import static com.trillionloans.los.constant.StringConstants.ELIGIBLE;
import static com.trillionloans.los.constant.StringConstants.INELIGIBLE;
import static com.trillionloans.los.constant.StringConstants.REJECTED;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;

import com.trillionloans.los.constant.BreType;
import com.trillionloans.los.model.entity.BreStatus;
import com.trillionloans.los.repository.BreStatusRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class BreStatusService {
  private BreStatusRepository breStatusRepository;

  public Mono<BreStatus> save(BreStatus breStatus) {
    return breStatusRepository.save(breStatus);
  }

  public Mono<BreStatus> findByExternalIdAndBreTypeAndIsActive(
      String externalId, String type, boolean isActive) {
    return breStatusRepository.findByExternalIdAndBreTypeAndIsActive(externalId, type, isActive);
  }

  public Mono<BreStatus> findByExternalIdAndSuccessStatus(String externalId) {
    return breStatusRepository.findByExternalIdAndStatus(externalId, SUCCESS);
  }

  public Mono<BreStatus> findByExternalIdAndSuccessStatusAndApprovedStateOrFiState(
      String externalId, boolean offerDowngrade) {
    if (offerDowngrade) {
      return findByExternalIdAndSuccessStatus(externalId);
    }
    return breStatusRepository.findByExternalIdAndStatusAndScienapticStatusIn(
        externalId, SUCCESS, List.of(APPROVED, APPROVED_FI));
  }

  public Mono<BreStatus> findByExternalIdAndSuccessStatusAndOnlyApprovedState(String externalId) {
    return breStatusRepository.findByExternalIdAndStatusAndScienapticStatus(
        externalId, SUCCESS, APPROVED);
  }

  public Mono<BreStatus> findByExternalIdAndStatusAndScienapticEligibleStatus(String externalId) {
    return breStatusRepository.findByExternalIdAndStatusAndScienapticStatus(
        externalId, SUCCESS, ELIGIBLE);
  }

  public Mono<BreStatus> findByExternalIdAndBreType(String externalId, String type) {
    return breStatusRepository.findByExternalIdAndBreType(externalId, type);
  }

  public Mono<BreStatus> logBreProcessStatus(BreStatus breStatus) {
    log.info(
        "[{}] Checking if a record exists for loanId: {}",
        BRE + breStatus.getStage(),
        breStatus.getExternalId());
    return breStatusRepository
        .findByExternalIdAndBreType(breStatus.getExternalId(), BreType.SANCTION.getDisplayName())
        .flatMap(
            existingStatus -> {
              log.info(
                  "[{}] Existing record found. Updating record for loanId: {}",
                  BRE + breStatus.getStage(),
                  breStatus.getExternalId());
              existingStatus.setRequest(breStatus.getRequest());
              existingStatus.setResponse(breStatus.getResponse());
              existingStatus.setStage(breStatus.getStage());
              existingStatus.setStatus(breStatus.getStatus());
              existingStatus.setActive(breStatus.isActive());
              existingStatus.setCallbackId(breStatus.getCallbackId());
              existingStatus.setScienapticStatus(breStatus.getScienapticStatus());
              existingStatus.setRetryCount(
                  COMPLETED.equals(breStatus.getStage())
                      ? existingStatus.getRetryCount() + 1
                      : existingStatus.getRetryCount());
              existingStatus.setRejectedCount(
                  COMPLETED.equals(breStatus.getStage())
                          && (REJECTED.equals(breStatus.getScienapticStatus())
                              || DECLINED.equals(breStatus.getScienapticStatus())
                              || INELIGIBLE.equals(breStatus.getScienapticStatus()))
                      ? existingStatus.getRejectedCount() + 1
                      : existingStatus.getRejectedCount());
              existingStatus.setUpdatedAt(LocalDateTime.now());
              return save(existingStatus);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[{}] No existing record found. Creating a new record for loanId: {} ",
                      BRE + breStatus.getStage(),
                      breStatus.getExternalId());
                  BreStatus newStatus =
                      BreStatus.builder()
                          .externalId(breStatus.getExternalId())
                          .request(breStatus.getRequest())
                          .response(breStatus.getResponse())
                          .breType(BreType.SANCTION.getDisplayName())
                          .stage(breStatus.getStage())
                          .status(breStatus.getStatus())
                          .isActive(breStatus.isActive())
                          .productCode(breStatus.getProductCode())
                          .callbackId(breStatus.getCallbackId())
                          .retryCount(COMPLETED.equals(breStatus.getStage()) ? 1L : 0L)
                          .scienapticStatus(breStatus.getScienapticStatus())
                          .rejectedCount(
                              COMPLETED.equals(breStatus.getStage())
                                      && (REJECTED.equals(breStatus.getScienapticStatus())
                                          || DECLINED.equals(breStatus.getScienapticStatus())
                                          || INELIGIBLE.equals(breStatus.getScienapticStatus()))
                                  ? 1L
                                  : 0L)
                          .createdAt(LocalDateTime.now())
                          .updatedAt(LocalDateTime.now())
                          .build();
                  return save(newStatus);
                }))
        .doOnSuccess(
            savedStatus ->
                log.info(
                    "[{}] Status successfully updated/saved for loanId: {}",
                    BRE + breStatus.getStage(),
                    breStatus.getExternalId()))
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] Error in logBreProcessStatus: {}",
                  BRE + breStatus.getStage(),
                  error.getMessage());
              return Mono.empty();
            });
  }

  public Mono<BreStatus> findByExternalIdOfCompletedAndEligibleBre(String externalId) {
    return breStatusRepository.findByExternalIdAndStatusAndScienapticStatusAndStage(
        externalId, SUCCESS, ELIGIBLE, COMPLETED);
  }
}
