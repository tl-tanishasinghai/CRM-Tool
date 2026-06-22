package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class M2pLoanApplicationTermsDTO {
  private Double maxEligibleAmount;
  private Integer numberOfRepayments;
  private Integer repayEvery;
  private Integer repaymentPeriodFrequencyEnum;
  private Integer termPeriodFrequencyEnum;
  private Integer termFrequency;
  private Double interestRatePerPeriod;

  private String dateFormat;
  private Integer graceOnPrincipalPayment;
  private Integer graceOnInterestCharged;
  private Double amountForUpfrontCollection;
  private String repaymentsStartingFromDate;
}
