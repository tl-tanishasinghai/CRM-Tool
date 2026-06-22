package com.trillionloans.lms.service.db;

import com.google.gson.Gson;
import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.model.entity.ReKycProductConfigEntity;
import com.trillionloans.lms.model.entity.ReKycTrackerEntity;
import com.trillionloans.lms.model.response.ReKycEligibleLoanDTO;
import com.trillionloans.lms.repository.ReKycProductConfigRepository;
import com.trillionloans.lms.repository.ReKycTrackerRepository;
import com.trillionloans.lms.util.EncryptionUtil;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ReKycTrackerService {
  private final M2PApi m2PApi;
  private final ReKycProductConfigRepository reKycProductConfigRepository;
  private final ReKycTrackerRepository reKycTrackerRepository;
  private final EncryptionUtil encryptionUtil;
  private final Gson gson;
  private final List<Long> dueDays;
  private final List<Long> overdueDays;

  public ReKycTrackerService(
      M2PApi m2PApi,
      ReKycProductConfigRepository reKycProductConfigRepository,
      ReKycTrackerRepository reKycTrackerRepository,
      EncryptionUtil encryptionUtil,
      @Value("${re-kyc.offsets.due-days}") List<Long> dueDays,
      @Value("${re-kyc.offsets.overdue-days}") List<Long> overdueDays) {
    this.m2PApi = m2PApi;
    this.reKycTrackerRepository = reKycTrackerRepository;
    this.reKycProductConfigRepository = reKycProductConfigRepository;
    this.encryptionUtil = encryptionUtil;
    this.gson = new Gson();
    this.dueDays = dueDays;
    this.overdueDays = overdueDays;
  }

  public Mono<Void> syncReKycSmsTracker() {
    return getWrittenOffProductConfig()
        .flatMapMany(
            configMap ->
                m2PApi
                    .fetchReKycNotificationEligibleLoans()
                    .filter(this::isValidLoan)
                    .flatMap(eligibleLoan -> upsertTracker(eligibleLoan, configMap), 500))
        .count()
        .flatMap(this::cleanupIfRequired)
        .doOnSuccess(
            v -> log.info("[SYNC_RE_KYC_TRACKER] re-kyc tracker sync completed successfully."))
        .doOnError(e -> log.error("[ERROR][SYNC_RE_KYC_TRACKER] failed to sync re-kyc tracker.", e))
        .then();
  }

  private Mono<Map<Integer, Integer>> getWrittenOffProductConfig() {
    return reKycProductConfigRepository
        .findAll()
        .collectMap(
            ReKycProductConfigEntity::getProductId, ReKycProductConfigEntity::getDpdWrittenOffDays);
  }

  private boolean isValidLoan(ReKycEligibleLoanDTO loan) {
    if (loan == null || loan.getProductId() == null) {
      log.error(
          "[ERROR][SYNC_RE_KYC_TRACKER] null loan or missing product code. Skipping notification.");
      return false;
    }
    return true;
  }

  private boolean isWrittenOff(ReKycEligibleLoanDTO loan, Map<Integer, Integer> configMap) {
    int writtenOffLimit = configMap.getOrDefault(loan.getProductId(), 120);

    return loan.getDpdDays() >= writtenOffLimit;
  }

  private Mono<Integer> upsertTracker(
      ReKycEligibleLoanDTO eligibleLoan, Map<Integer, Integer> configMap) {

    boolean isWrittenOff = isWrittenOff(eligibleLoan, configMap);
    boolean isActive = !isWrittenOff;

    return reKycTrackerRepository
        .upsertTracker(
            eligibleLoan.getClientId(),
            eligibleLoan.getLanId(),
            eligibleLoan.getProductId(),
            eligibleLoan.getClientName(),
            encryptionUtil.encrypt(eligibleLoan.getMobileNo()),
            eligibleLoan.getDisbursalDate(),
            eligibleLoan.getKycDueDate(),
            determineSmsCode(eligibleLoan.getDaysDiff()),
            isActive,
            isWrittenOff,
            eligibleLoan.getDpdDays())
        .onErrorResume(
            e -> {
              log.error(
                  "[ERROR][SYNC_RE_KYC_TRACKER] failed to sync tracker for client {}",
                  eligibleLoan.getClientId(),
                  e);
              return Mono.empty();
            });
  }

  private String determineSmsCode(long diff) {
    if (dueDays.contains(diff)) {
      return "SMS_" + diff + "D";
    }
    if (overdueDays.contains(diff)) {
      return "SMS_OVERDUE_" + Math.abs(diff);
    }
    return null;
  }

  private Mono<Void> cleanupIfRequired(Long processedCount) {
    if (processedCount > 0) {
      log.info(
          "[SYNC_RE_KYC_TRACKER] processed {} records. Cleaning up stale entries.", processedCount);
      return reKycTrackerRepository.deactivateOldRecords().then();
    }

    log.error("[ERROR][SYNC_RE_KYC_TRACKER] fetched 0 eligible loans. Skipping cleanup.");
    return Mono.empty();
  }

  public Flux<ReKycTrackerEntity> findAllByIsActiveTrue() {
    return reKycTrackerRepository.findAllByIsActiveTrue();
  }

  public Mono<Integer> updateTriggerStatus(String clientId, String code) {
    return reKycTrackerRepository.updateTriggerStatus(clientId, code);
  }
}
