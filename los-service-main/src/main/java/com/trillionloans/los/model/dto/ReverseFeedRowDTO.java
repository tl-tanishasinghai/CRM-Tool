package com.trillionloans.los.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * dto representing a single row from the reverse feed excel file. excel columns: transaction date,
 * loan account number, transaction status, transaction rejection reason, utr reference number
 *
 * <p>note: "loan account number" column has different meanings based on product type: - for
 * line-based products: this is actually transaction_id (referenceId1) - for non-line products: this
 * is loan_account_number (referenceId2)
 *
 * <p>temporarily stored in loanAccountNumber field, then assigned to ref1 or ref2 during processing
 * based on product type.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseFeedRowDTO {

  /** transaction date from excel */
  private String transactionDate;

  /**
   * value from "loan account number" excel column. - for line products: this is transaction_id
   * (will be stored in referenceId1) - for non-line products: this is loan_account_number (will be
   * stored in referenceId2)
   */
  private String loanAccountNumber;

  /** transaction status: E (success) or R (rejected) */
  private String transactionStatus;

  /** rejection reason (populated when status is rejected) */
  private String transactionRejectionReason;

  /** utr reference number (populated when status is success) */
  private String utrReferenceNumber;

  /** amount (if present in excel) */
  private BigDecimal amount;
}
