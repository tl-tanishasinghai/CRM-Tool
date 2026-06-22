package com.trillionloans.los.model.entity;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents a log entry for a loan offer that was rejected due to expiry. This entity maps to the
 * 'offer_expiry_rejection_table'.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "offer_expiry_rejection_table")
public class OfferExpiryRejectionEntity {

  @Id private Long id;

  @Column("client_id")
  private String clientId;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("product_name")
  private String productName;

  @Column("bre_offer_approved_on")
  private String breOfferApprovedOn;

  @Column("loan_rejection_date")
  private LocalDate loanRejectionDate;

  @Column("rejection_reason")
  private String rejectionReason;

  @Column("cron_run_id")
  private String cronRunId;
}
