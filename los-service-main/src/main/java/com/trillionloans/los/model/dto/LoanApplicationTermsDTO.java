package com.trillionloans.los.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Terms for a loan application")
public class LoanApplicationTermsDTO {
  private Double maxEligibleAmount;
  private Integer numberOfRepayments;
  private Integer repayEvery;
  private Integer repaymentPeriodFrequencyEnum;
  private Integer termPeriodFrequencyEnum;
  private Integer termFrequency;

  @NotNull(message = "[leadApplicationTerms] interestRatePerPeriod is required")
  private Double interestRatePerPeriod;

  private Integer graceOnPrincipalPayment;
  private Integer graceOnInterestCharged;
  private Double amountForUpfrontCollection;
  private String repaymentsStartingFromDate;
}
