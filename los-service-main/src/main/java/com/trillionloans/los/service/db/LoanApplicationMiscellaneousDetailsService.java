package com.trillionloans.los.service.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.model.entity.LoanApplicationMiscellaneousDetails;
import com.trillionloans.los.repository.LoanApplicationMiscellaneousDetailsRepository;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@AllArgsConstructor
@Slf4j
public class LoanApplicationMiscellaneousDetailsService {

  private final LoanApplicationMiscellaneousDetailsRepository
      loanApplicationMiscellaneousDetailsRepository;
  private final ObjectMapper objectMapper;

  /**
   * Saves miscellaneous details for a given loan application asynchronously. If an entry already
   * exists for the loan application ID, it will be skipped. This method fires and forgets - it
   * doesn't block the caller. MDC context is preserved across thread boundaries.
   *
   * @param loanApplicationId the loan application ID
   * @param clientId the client ID
   * @param productCode the product code
   * @param miscellaneousDetails map of key-value pairs to save
   */
  public void saveMiscellaneousDetailsAsync(
      Integer loanApplicationId,
      Integer clientId,
      String productCode,
      Map<String, String> miscellaneousDetails) {
    if (miscellaneousDetails == null || miscellaneousDetails.isEmpty()) {
      log.info(
          "[LOAN_APPLICATION_MISC_DETAILS] No miscellaneous details to save for loanApplicationId:"
              + " {}",
          loanApplicationId);
      return;
    }

    // Capture MDC context from the current thread
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    log.info(
        "[LOAN_APPLICATION_MISC_DETAILS] Initiating async save of {} miscellaneous details for"
            + " loanApplicationId: {}, clientId: {}",
        miscellaneousDetails.size(),
        loanApplicationId,
        clientId);

    String detailsJson;
    try {
      detailsJson = objectMapper.writeValueAsString(miscellaneousDetails);
    } catch (JsonProcessingException e) {
      log.error(
          "[LOAN_APPLICATION_MISC_DETAILS] Failed to serialize miscellaneous details for"
              + " loanApplicationId: {}, error: {}",
          loanApplicationId,
          e.getMessage());
      return;
    }

    String finalDetailsJson = detailsJson;

    loanApplicationMiscellaneousDetailsRepository
        .findByLoanApplicationId(loanApplicationId)
        .hasElement()
        .flatMap(
            exists -> {
              if (Boolean.TRUE.equals(exists)) {
                setMdcContext(mdcContext);
                log.info(
                    "[LOAN_APPLICATION_MISC_DETAILS] Miscellaneous details already exist for"
                        + " loanApplicationId: {}, skipping save",
                    loanApplicationId);
                return Mono.empty();
              }
              LoanApplicationMiscellaneousDetails loanApplicationMiscellaneousDetails =
                  LoanApplicationMiscellaneousDetails.builder()
                      .loanApplicationId(loanApplicationId)
                      .clientId(clientId)
                      .productCode(productCode)
                      .details(finalDetailsJson)
                      .build();
              return loanApplicationMiscellaneousDetailsRepository.save(
                  loanApplicationMiscellaneousDetails);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(
            saved -> {
              setMdcContext(mdcContext);
              if (saved != null) {
                log.info(
                    "[LOAN_APPLICATION_MISC_DETAILS] Successfully saved miscellaneous details for"
                        + " loanApplicationId: {}, clientId: {}",
                    loanApplicationId,
                    clientId);
              }
            })
        .doOnError(
            error -> {
              setMdcContext(mdcContext);
              log.error(
                  "[LOAN_APPLICATION_MISC_DETAILS] Error saving miscellaneous details for"
                      + " loanApplicationId: {}, clientId: {}, error: {}",
                  loanApplicationId,
                  clientId,
                  error.getMessage());
            })
        .doFinally(signalType -> MDC.clear())
        .subscribe();
  }

  /**
   * Saves miscellaneous details for a given loan application synchronously. Returns a Mono that
   * completes when the save is done.
   *
   * @param loanApplicationId the loan application ID
   * @param clientId the client ID
   * @param productCode the product code
   * @param miscellaneousDetails map of key-value pairs to save
   * @return Mono<LoanApplicationMiscellaneousDetails> the saved entity, or empty if nothing to save
   */
  public Mono<LoanApplicationMiscellaneousDetails> saveMiscellaneousDetails(
      Integer loanApplicationId,
      Integer clientId,
      String productCode,
      Map<String, String> miscellaneousDetails) {
    if (miscellaneousDetails == null || miscellaneousDetails.isEmpty()) {
      log.info(
          "[LOAN_APPLICATION_MISC_DETAILS] No miscellaneous details to save for loanApplicationId:"
              + " {}",
          loanApplicationId);
      return Mono.empty();
    }

    log.info(
        "[LOAN_APPLICATION_MISC_DETAILS] Saving {} miscellaneous details for loanApplicationId:"
            + " {}, clientId: {}",
        miscellaneousDetails.size(),
        loanApplicationId,
        clientId);

    String detailsJson;
    try {
      detailsJson = objectMapper.writeValueAsString(miscellaneousDetails);
    } catch (JsonProcessingException e) {
      log.error(
          "[LOAN_APPLICATION_MISC_DETAILS] Failed to serialize miscellaneous details for"
              + " loanApplicationId: {}, error: {}",
          loanApplicationId,
          e.getMessage());
      return Mono.error(
          new RuntimeException("Failed to serialize loan application miscellaneous details", e));
    }

    LoanApplicationMiscellaneousDetails loanApplicationMiscellaneousDetails =
        LoanApplicationMiscellaneousDetails.builder()
            .loanApplicationId(loanApplicationId)
            .clientId(clientId)
            .productCode(productCode)
            .details(detailsJson)
            .build();

    return loanApplicationMiscellaneousDetailsRepository
        .save(loanApplicationMiscellaneousDetails)
        .doOnSuccess(
            saved ->
                log.info(
                    "[LOAN_APPLICATION_MISC_DETAILS] Successfully saved miscellaneous details for"
                        + " loanApplicationId: {}, clientId: {}",
                    loanApplicationId,
                    clientId))
        .doOnError(
            error ->
                log.error(
                    "[LOAN_APPLICATION_MISC_DETAILS] Error saving miscellaneous details for"
                        + " loanApplicationId: {}, clientId: {}, error: {}",
                    loanApplicationId,
                    clientId,
                    error.getMessage()));
  }

  /**
   * Sets the MDC context on the current thread.
   *
   * @param mdcContext the MDC context map to set
   */
  private void setMdcContext(Map<String, String> mdcContext) {
    if (mdcContext != null) {
      MDC.setContextMap(mdcContext);
    }
  }
}
