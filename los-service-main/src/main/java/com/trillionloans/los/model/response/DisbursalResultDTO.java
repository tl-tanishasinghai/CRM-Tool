package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * generic dto for reverse feed disbursal result after approval/rejection. used only in
 * NexusManualDisbursalService for both: - line-based products (drawdown approval/rejection) -
 * non-line products (loan disbursement marking/rejection)
 *
 * <p>unified field mapping: - referenceId1: loan_application_id (non-line) or transaction_id (line)
 * - referenceId2: loan_account_number (non-line) or line_id (line)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisbursalResultDTO {

  /** reference id 1: loan_application_id (non-line) or transaction_id (line) */
  private String referenceId1;

  /** reference id 2: loan_account_number (non-line) or line_id (line) */
  private String referenceId2;

  /** line id (for line-based products only, null for non-line) */
  private String lineId;

  /** status of the operation (SUCCESS, OPS_REJECTED, FAILED, etc.) */
  private String status;

  /** utr / receipt number from approval */
  private String receiptNumber;

  /** the approved/disbursed gross amount */
  private BigDecimal approvedAmount;

  /** the net disbursement amount after charges */
  private BigDecimal netDisbursement;

  /** the date of disbursement */
  private String disbursementDate;

  /** rejection reason (populated only for rejections) */
  private String rejectionReason;

  /** whether this is a line-based product */
  private Boolean isLineBasedProduct;

  /** m2p api response (for storing in db) */
  private String m2pResponse;
}
