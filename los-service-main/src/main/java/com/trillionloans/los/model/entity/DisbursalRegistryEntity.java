package com.trillionloans.los.model.entity;

import com.trillionloans.los.constant.DisbursalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing the disbursal registry. This is the Universal Registry & Lock table that
 * prevents double-disbursals. The reference_id1 acts as the primary lock mechanism via unique
 * constraint.
 *
 * <p>Unified naming convention across products: - reference_id1: loan_application_id (PL/CL) OR
 * transaction_id (other products) - reference_id2: loan_account_number (PL/CL) OR line_id (other
 * products)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("disbursal_registry")
public class DisbursalRegistryEntity {

  @Id private Long id;

  /**
   * Primary reference identifier - acts as the unique lock identifier. Maps to: loan_application_id
   * (PL/CL) OR transaction_id (other products)
   */
  @Column("reference_id1")
  private String referenceId1;

  /** Client/Lead ID associated with this transaction */
  @Column("client_id")
  private String clientId;

  /** Client name */
  @Column("client_name")
  private String clientName;

  /**
   * Secondary reference identifier. Maps to: loan_account_number (PL/CL) OR line_id (other
   * products)
   */
  @Column("reference_id2")
  private String referenceId2;

  /** Product code identifier (e.g., PL, CL) */
  @Column("product_code")
  private String productCode;

  /** Disburse type: AUTO or MANUAL */
  @Column("disburse_type")
  private String disburseType;

  /** Disburse status following the state machine: MANUAL_INI, MANUAL_BATCHED, SUCCESS, FAILED */
  @Column("disburse_status")
  private DisbursalStatus disburseStatus;

  /** Transaction date */
  @Column("transaction_date")
  private String transactionDate;

  /** Foreign key to disbursal_batch (nullable for auto-disbursals) */
  @Column("batch_id")
  private UUID batchId;

  /** Bank IFSC code - populated via async hydration from M2P */
  @Column("ifsc_code")
  private String ifscCode;

  /** Bank account holder name - populated via async hydration from M2P */
  @Column("bank_holder_name")
  private String bankHolderName;

  /** Bank name */
  @Column("bank_name")
  private String bankName;

  /** Bank name number */
  @Column("bank_name_number")
  private String bankNameNumber;

  /** Gross disbursal amount */
  @Column("gross_disbursal_amount")
  private BigDecimal grossDisbursalAmount;

  /** The net amount to be transferred */
  @Column("net_disbursal_amount")
  private BigDecimal netDisbursalAmount;

  /** Balance transfer outstanding amount */
  @Column("balance_transfer_outstanding")
  private BigDecimal balanceTransferOutstanding;

  /** Partner identifier */
  @Column("partner")
  private String partner;

  /** Balance transfer customer existing loan ID */
  @Column("balance_transfer_customer_existing_loan_id")
  private String balanceTransferCustomerExistingLoanId;

  /** Unique Transaction Reference - seeded after bank confirmation */
  @Column("utr_number")
  private String utrNumber;

  /** Bank code for the target bank (e.g., HDFC, ICICI) */
  @Column("bank_code")
  private String bankCode;

  /** Bank account ID from M2P */
  @Column("bank_account_id")
  private String bankAccountId;

  /** Bank account number */
  @Column("bank_account_number")
  private String bankAccountNumber;

  /** Anchor ID */
  @Column("anchor_id")
  private String anchorId;

  /** Failure reason if the disbursal failed */
  @Column("failure_reason")
  private String failureReason;

  /** Failure reason if the disbursal failed */
  @Column("secondary_failure_reason")
  private String secondaryFailureReason;

  /** Flag indicating if hydration is complete */
  @Column("is_hydrated")
  private Boolean isHydrated;

  /** Timestamp when hydration was started */
  @Column("hydrated_started_at")
  private LocalDateTime hydratedStartedAt;

  /** Timestamp when hydration was completed */
  @Column("hydrated_completed_at")
  private LocalDateTime hydratedCompletedAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  /** Optimistic locking version - auto-incremented on updates by R2DBC */
  @Version
  @Column("version")
  private Integer version;

  /** Soft delete flag */
  @Column("is_deleted")
  private Boolean isDeleted;
}
