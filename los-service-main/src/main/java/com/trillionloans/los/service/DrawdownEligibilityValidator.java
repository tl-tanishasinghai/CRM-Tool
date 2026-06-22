package com.trillionloans.los.service;

import static com.trillionloans.los.constant.DocumentTag.DRAWDOWN_AGREEMENT;

import com.trillionloans.los.constant.DocumentTag;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.entity.CreditLineEntity;
import com.trillionloans.los.repository.CreditLineRepository;
import com.trillionloans.los.repository.drawdown.DrawdownRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Validates that a lead is eligible for DRAWDOWN_AGREEMENT Digio e-sign: credit line exists for the
 * lead (optionally scoped by product), M2P line id is present, and at least one drawdown exists for
 * that line.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DrawdownEligibilityValidator implements TagEligibilityValidator {

  private static final String LOG_PREFIX = "[DRAWDOWN_ELIGIBILITY]";

  private final CreditLineRepository creditLineRepository;
  private final DrawdownRepository drawdownRepository;

  @Override
  public DocumentTag getSupportedTag() {
    return DRAWDOWN_AGREEMENT;
  }

  @Override
  public Mono<Void> validateEligibility(String loanId, String productCode) {
    parseLeadId(loanId);
    return resolveCreditLine(loanId, productCode)
        .flatMap(
            creditLine -> {
              String lineId = creditLine.getM2pCreditLineId();
              if (StringUtils.isBlank(lineId)) {
                log.warn("{}[FAIL] loanId={} has no m2p credit line id", LOG_PREFIX, loanId);
                return Mono.error(
                    new BaseException(
                        "Credit line has no line id for drawdown e-sign",
                        "m2p_credit_line_id is blank",
                        HttpStatus.BAD_REQUEST));
              }
              return drawdownRepository
                  .findByLineId(lineId)
                  .take(1)
                  .next()
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.warn(
                                "{}[FAIL] loanId={}, lineId={}: no drawdown found",
                                LOG_PREFIX,
                                loanId,
                                lineId);
                            return Mono.error(
                                new BaseException(
                                    "No drawdown found for this credit line",
                                    "At least one drawdown must exist for DRAWDOWN_AGREEMENT"
                                        + " e-sign",
                                    HttpStatus.BAD_REQUEST));
                          }))
                  .doOnSuccess(
                      ignored ->
                          log.info(
                              "{}[PASS] loanId={} eligible for drawdown agreement e-sign",
                              LOG_PREFIX,
                              loanId))
                  .then();
            });
  }

  private Mono<CreditLineEntity> resolveCreditLine(String leadId, String productCode) {
    Mono<CreditLineEntity> mono =
        StringUtils.isNotBlank(productCode)
            ? creditLineRepository.findFirstByLeadIdAndProductCodeOrderByCreatedAtDesc(
                leadId, productCode)
            : creditLineRepository.findByLeadId(leadId);
    return mono.switchIfEmpty(
        Mono.defer(
            () ->
                Mono.error(
                    new BaseException(
                        "Credit line not found for drawdown",
                        "No credit line for lead"
                            + (StringUtils.isNotBlank(productCode) ? " and product" : ""),
                        HttpStatus.BAD_REQUEST))));
  }

  private void parseLeadId(String loanId) {
    try {
      Long.parseLong(loanId);
    } catch (NumberFormatException e) {
      throw new BaseException(
          "Invalid lead/loan id: " + loanId, "Lead id must be numeric", HttpStatus.BAD_REQUEST);
    }
  }
}
