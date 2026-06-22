package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.BRE_EXPIRY_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.BRE_OFFER_EXPIRED;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.config.RejectionReasonCodeFactory;
import com.trillionloans.los.model.dto.BreApprovedLoanDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.OfferExpiryRejectionEntity;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.repository.OfferExpiryRejectionRepository;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanOfferExpiryService {

  @Value("${throttle.per-second}")
  private int throttlePerSecond;

  private final ProductConfigMasterService productConfigMasterService;
  private final OfferExpiryRejectionRepository offerExpiryRejectionRepository;
  private final M2PWrapperApi m2PWrapperApi;
  private final RejectionReasonCodeFactory reasonCodeFactory;

  public Mono<Void> runExpiryJob() {
    final String cronRunId = UUID.randomUUID().toString();
    final LocalDate today = LocalDate.now();
    log.info("[OFFER_EXPIRY] starting offer expiry job. cronRunId: {}", cronRunId);

    return m2PWrapperApi
        .getBreApprovedLoans()
        .flatMap(loan -> getIfLoanIsExpired(loan, today))
        .buffer(throttlePerSecond)
        .delayElements(Duration.ofSeconds(1))
        .flatMap(batch -> processBatch(batch, cronRunId, today))
        .then()
        .doOnSuccess(
            v ->
                log.info(
                    "[OFFER_EXPIRY] scheduled loan expiry job finished successfully. cronRunId:"
                        + " {}",
                    cronRunId))
        .doOnError(
            e ->
                log.error(
                    "[ERROR][OFFER_EXPIRY] scheduled loan expiry job failed. cronRunId: {}",
                    cronRunId,
                    e));
  }

  private Mono<Void> processBatch(
      List<BreApprovedLoanDTO> loanBatch, String cronRunId, LocalDate today) {
    if (loanBatch.isEmpty()) {
      return Mono.empty();
    }
    log.info("[OFFER_EXPIRY] processing micro-batch of {} loans.", loanBatch.size());
    return Flux.fromIterable(loanBatch)
        .flatMap(loan -> processRejection(loan, cronRunId, today))
        .then();
  }

  /**
   * Asynchronously checks if a single loan is expired by fetching its product config. Returns
   * Mono<BreApprovedLoanDTO> if expired, Mono.empty() otherwise.
   */
  private Mono<BreApprovedLoanDTO> getIfLoanIsExpired(BreApprovedLoanDTO loan, LocalDate today) {
    String breDateString = loan.getBreOfferApprovedOn();
    if (breDateString == null || breDateString.isEmpty()) {
      log.error(
          "[ERROR][OFFER_EXPIRY] Loan {} has null or empty bre_offer_approved_on date, skipping.",
          loan.getLoanApplicationId());
      return Mono.empty();
    }
    if (loan.getProductCode() == null || loan.getProductCode().isEmpty()) {
      log.warn(
          "[OFFER_EXPIRY] loan {} has null or empty productCode, skipping.",
          loan.getLoanApplicationId());
      return Mono.empty();
    }
    LocalDate breApprovedDate;
    try {
      LocalDateTime breDateTime =
          LocalDateTime.parse(
              breDateString, DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH));
      breApprovedDate = breDateTime.toLocalDate();
    } catch (DateTimeParseException e) {
      log.error(
          "[ERROR][OFFER_EXPIRY] failed to parse bre_offer_approved_on '{}' for loan {}, skipping.",
          breDateString,
          loan.getLoanApplicationId(),
          e);
      return Mono.empty();
    }
    return productConfigMasterService
        .getProductConfigMasterData(loan.getProductCode())
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[ERROR][OFFER_EXPIRY] no product configs found for productCode {}, skipping"
                          + " loan {}.",
                      loan.getProductCode(),
                      loan.getLoanApplicationId());
                  return Mono.empty();
                }))
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), BRE_EXPIRY_IDENTIFIER);

              if (flowData == null
                  || !flowData.getRejectOnExpiry()
                  || flowData.getOfferEligibilityTenureDays() == null) {
                log.error(
                    "[ERROR][OFFER_EXPIRY] false or missing offer expiry rules found for {}",
                    loan.getProductCode());
                return Mono.empty();
              }

              long offerAgeDays = ChronoUnit.DAYS.between(breApprovedDate, today);
              if (offerAgeDays > flowData.getOfferEligibilityTenureDays()) {
                return Mono.just(loan);
              } else {
                return Mono.empty();
              }
            })
        .doOnError(
            e ->
                log.error(
                    "[ERROR][OFFER_EXPIRY] failed to find or process offer expiry config for"
                        + " product {}: {}",
                    loan.getProductCode(),
                    e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  private Mono<Void> processRejection(BreApprovedLoanDTO loan, String cronRunId, LocalDate today) {
    LoanReject loanReject =
        LoanReject.builder()
            .reasonCode(reasonCodeFactory.getBreOfferExpiryCode())
            .description(BRE_OFFER_EXPIRED)
            .build();
    return m2PWrapperApi
        .rejectLoanApplication(loanReject, loan.getLoanApplicationId())
        .then(logRejectionToDb(loan, cronRunId, today))
        .doOnSuccess(
            v ->
                log.info(
                    "[OFFER_EXPIRY] successfully processed rejection for {}",
                    loan.getLoanApplicationId()))
        .doOnError(
            e ->
                log.error(
                    "[ERROR][OFFER_EXPIRY] failed to process rejection for {}: {}",
                    loan.getLoanApplicationId(),
                    e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  private Mono<Void> logRejectionToDb(BreApprovedLoanDTO loan, String cronRunId, LocalDate today) {
    OfferExpiryRejectionEntity rejectionLog =
        OfferExpiryRejectionEntity.builder()
            .clientId(loan.getClientId())
            .loanApplicationId(loan.getLoanApplicationId())
            .productName(loan.getProductCode())
            .breOfferApprovedOn(loan.getBreOfferApprovedOn())
            .loanRejectionDate(today)
            .rejectionReason(BRE_OFFER_EXPIRED)
            .cronRunId(cronRunId)
            .build();
    return offerExpiryRejectionRepository.save(rejectionLog).then();
  }
}
