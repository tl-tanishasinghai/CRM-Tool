package com.trillionloans.los.model.request.m2p;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class M2PUpdateLoanRequest {
  private String loanAmountRequested;
  private String tenure;
  private String rateOfInterest;
  private String expectedDisbursementDate;
  private String dateFormat;
  private String repaymentsStartingFromDate;
}
