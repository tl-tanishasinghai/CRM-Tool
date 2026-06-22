package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for preview RPS schedule API. Pass-through to M2P get-rps-without-loan; same
 * structure for upstream and M2P. No validation applied in LOS.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreviewRpsRequest {

  private Integer productId;
  private Integer principal;
  private Integer numberOfRepayments;
  private Integer repaymentEvery;
  private Integer loanTermFrequencyType;
  private Integer repaymentFrequencyType;
  private Integer amortizationType;
  private Integer interestType;
  private Integer interestCalculationPeriodType;
  private List<PreviewRpsCharge> charges;
  private Double interestRatePerPeriod;
  private Integer loanTermFrequency;
  private String locale;
  private String dateFormat;
  private String loanType;
  private String expectedDisbursementDate;
  private String submittedOnDate;
  private String transactionProcessingStrategyId;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PreviewRpsCharge {
    private Double amount;
    private Integer chargeId;
    private Boolean canAddChargeToPrincipalForComputation;
    private Boolean canLendCharge;
  }
}
