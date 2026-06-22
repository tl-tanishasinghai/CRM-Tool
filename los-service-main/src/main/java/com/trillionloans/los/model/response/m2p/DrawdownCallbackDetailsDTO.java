package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for DrawdownCallbackDetails report from M2P. Used to fetch disbursal details after drawdown
 * approval.
 *
 * <p>Sample response: [{"drawdownTransactionId":"72ff123c-7bce-4b55-a769-d03c64c8b1ef",
 * "loanAccountNumber":15455, "approvedAmount":10000.000000, "netDisbursement":9460.000000,
 * "disbursedDate":"Feb 5, 2026"}]
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawdownCallbackDetailsDTO {

  /** The transaction ID of the drawdown */
  private String drawdownTransactionId;

  /** The loan account number (LAN) */
  private Long loanAccountNumber;

  /** The approved amount for the drawdown */
  private BigDecimal approvedAmount;

  /** The net disbursement amount after charges */
  private BigDecimal netDisbursement;

  /** The date of disbursement (format: "MMM d, yyyy" e.g., "Feb 5, 2026") */
  private String disbursedDate;
}
