package com.trillionloans.los.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * dto for reverse feed batch item detail (for download).
 *
 * <p>unified naming convention: - referenceId1: loan_application_id (pl/cl) or transaction_id
 * (other products) - referenceId2: loan_account_number (pl/cl) or line_id (other products)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseFeedBatchDetailDTO {

  private String referenceId1;

  private String referenceId2;

  private String transactionStatus;

  private String utrNumber;

  private String transactionRejectionReason;

  private BigDecimal amount;

  private String syncStatus;

  private String m2pResponse;

  private String errorMessage;

  private LocalDateTime processedAt;
}
