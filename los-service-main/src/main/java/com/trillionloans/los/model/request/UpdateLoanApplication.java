package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Loan update request body")
public class UpdateLoanApplication {
  private String loanAmountRequested;
  private String tenure;
  private String rateOfInterest;
  private String expectedDisbursementDate;
  private String dateFormat;
  private String repaymentsStartingFromDate;
  private Boolean ploBreOfferAcceptance;
  private Boolean isInsuranceOpted;
}
