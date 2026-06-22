package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for callback sent to partner after drawdown approval/rejection.
 *
 * <p>Callback body format:
 * {"lineId":"","transactionId":"","lanId":"","status":"","receiptNumber":"",
 * "approvedAmount":"","netDisbursement":"","disbursementDate":""}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawdownPartnerCallbackDTO {

  /** The credit line ID */
  private String lineId;

  /** Drawdown Id Request Id */
  private String drawdownRequestId;

  /** The drawdown transaction ID */
  private String transactionId;

  /** The loan account number (LAN ID) */
  private String lanId;

  /** The status of the drawdown (SUCCESS, OPS_REJECTED, etc.) */
  private String status;

  /** Receipt number from approval */
  private String receiptNumber;

  /** The approved amount */
  private BigDecimal approvedAmount;

  /** The net disbursement amount after charges */
  private BigDecimal netDisbursement;

  /** The date of disbursement */
  private String disbursementDate;

  /** Rejection reason (populated only for rejections) */
  private String rejectionReason;
}
