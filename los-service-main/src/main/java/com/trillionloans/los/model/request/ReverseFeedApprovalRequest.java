package com.trillionloans.los.model.request;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * generic request for reverse feed approval. used for both line-based products (drawdown approval)
 * and non-line-based products (loan disbursement marking).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReverseFeedApprovalRequest {

  /** transaction date from the reverse feed file */
  private LocalDateTime transactionDate;

  /** notes for the approval */
  private String notes;

  /** payment type (e.g., "online transfer") */
  private String paymentType;

  /** utr / reference number for the payment */
  private String referenceNumber;
}
