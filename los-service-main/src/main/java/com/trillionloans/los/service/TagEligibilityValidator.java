package com.trillionloans.los.service;

import com.trillionloans.los.constant.DocumentTag;
import reactor.core.publisher.Mono;

/**
 * Validates whether a loan/entity is eligible for Digio e-signing flow for a specific document tag.
 * Each implementation is responsible for one tag and performs tag-specific eligibility checks (e.g.
 * loan exists in restructure table, drawdown table, etc.) before any Digio API calls.
 */
public interface TagEligibilityValidator {

  /** Returns the document tag this validator supports. */
  DocumentTag getSupportedTag();

  /**
   * Validates eligibility for the given loan. Returns Mono.empty() on success, or Mono.error() if
   * not eligible.
   *
   * @param loanId loan identifier
   * @param productCode product code
   * @return Mono completing when validation passes, or error if not eligible
   */
  Mono<Void> validateEligibility(String loanId, String productCode);
}
