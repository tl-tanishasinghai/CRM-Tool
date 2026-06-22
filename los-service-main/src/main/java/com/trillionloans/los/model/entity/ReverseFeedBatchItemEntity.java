package com.trillionloans.los.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * entity representing an individual item from a reverse feed batch. each row in the bank response
 * excel maps to one ReverseFeedBatchItem. same reference_id1 can exist in multiple batches
 * (idempotency handled by m2p).
 *
 * <p>unified naming convention across products: - reference_id1: loan_application_id (pl/cl) or
 * transaction_id (other products) - reference_id2: loan_account_number (pl/cl) or line_id (other
 * products)
 *
 * <p>the "loan account number" column from excel is stored based on product type: - for line
 * products: stored in referenceId1 (it's actually transaction_id) - for non-line products: stored
 * in referenceId2 (it's loan_account_number)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("reverse_feed_batch_item")
public class ReverseFeedBatchItemEntity {

  @Id private Long id;

  @Column("batch_id")
  private UUID batchId;

  /**
   * primary reference identifier. maps to: loan_application_id (pl/cl) or transaction_id (other
   * products)
   */
  @Column("reference_id1")
  private String referenceId1;

  /**
   * secondary reference identifier - this comes from excel. maps to: loan_account_number (pl/cl) or
   * line_id (other products)
   */
  @Column("reference_id2")
  private String referenceId2;

  @Column("transaction_status")
  private String transactionStatus;

  @Column("utr_number")
  private String utrNumber;

  @Column("transaction_rejection_reason")
  private String transactionRejectionReason;

  @Column("amount")
  private BigDecimal amount;

  @Column("transaction_date")
  private LocalDateTime transactionDate;

  @Column("sync_status")
  private String syncStatus;

  @Column("m2p_response")
  private String m2pResponse;

  @Column("error_message")
  private String errorMessage;

  @Column("processed_at")
  private LocalDateTime processedAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
