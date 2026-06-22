package com.trillionloans.los.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing additional details for a drawdown after approval/rejection. Populated after
 * OPS approval/rejection from OPS_APPROVAL_PENDING state.
 */
@Getter
@Setter
@Builder
@Table("drawdown_additional_details")
@NoArgsConstructor
@AllArgsConstructor
public class DrawdownAdditionalDetails {

  @Id private Long id;

  @Column("drawdown_id")
  private Long drawdownId;

  @Column("loan_account_number")
  private Long loanAccountNumber;

  @Column("approved_amount")
  private BigDecimal approvedAmount;

  @Column("net_disbursed_amount")
  private BigDecimal netDisbursedAmount;

  @Column("disbursed_date")
  private LocalDate disbursedDate;

  @Column("receipt_number")
  private String receiptNumber;

  @Column("rejection_reason")
  private String rejectionReason;
}
