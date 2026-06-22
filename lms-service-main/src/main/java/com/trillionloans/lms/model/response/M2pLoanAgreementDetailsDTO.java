package com.trillionloans.lms.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class M2pLoanAgreementDetailsDTO {
  private String loanId;
  private String documentId;
  private String clientName;
  private String startDate;
  private String mobileNumber;
  private String bankAccountNumber;
  private double disbursedAmount;
}
