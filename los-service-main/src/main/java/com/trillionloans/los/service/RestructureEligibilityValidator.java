package com.trillionloans.los.service;

import static com.trillionloans.los.constant.DocumentTag.RESCHEDULE_AGREEMENT;

import com.trillionloans.los.constant.DocumentTag;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.repository.LoanApplicationRestructureDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Validates that a loan/lead is eligible for RESTRUCTURE_LOAN_AGREEMENT Digio flow by checking
 * loan_application_restructure_details: lead = loanId, eligibility = true, restructure =
 * NOT_TRIGGERED.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RestructureEligibilityValidator implements TagEligibilityValidator {

  private static final String LOG_PREFIX = "[RESTRUCTURE_ELIGIBILITY]";
  private static final String RESTRUCTURE_NOT_TRIGGERED = "NOT_TRIGGERED";

  private final LoanApplicationRestructureDetailsRepository restructureDetailsRepository;

  @Override
  public DocumentTag getSupportedTag() {
    return RESCHEDULE_AGREEMENT;
  }

  @Override
  public Mono<Void> validateEligibility(String loanId, String productCode) {
    Long leadId = parseLeadId(loanId);
    return restructureDetailsRepository
        .findByLeadAndEligibilityAndRestructure(leadId, true, RESTRUCTURE_NOT_TRIGGERED)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "{}[FAIL] leadId={} not eligible: no row with eligibility=true,"
                          + " restructure={}",
                      LOG_PREFIX,
                      loanId,
                      RESTRUCTURE_NOT_TRIGGERED);
                  return Mono.error(
                      new BaseException(
                          "Loan is not eligible for restructure",
                          "Lead not found with eligibility=true and restructure=NOT_TRIGGERED",
                          HttpStatus.BAD_REQUEST));
                }))
        .flatMap(
            entity -> {
              log.info("{}[PASS] leadId={} eligible for restructure", LOG_PREFIX, loanId);
              return Mono.<Void>empty();
            });
  }

  private Long parseLeadId(String loanId) {
    try {
      return Long.parseLong(loanId);
    } catch (NumberFormatException e) {
      throw new BaseException(
          "Invalid lead/loan id: " + loanId, "Lead id must be numeric", HttpStatus.BAD_REQUEST);
    }
  }
}
