package com.trillionloans.los.model.response.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class M2pGetLoanApplicationsResponseDTO {
  public Double loanApplicationReferenceId;
  public String loanApplicationReferenceNo;
  public Double loanId;
  public String clientName;
  public Status status;
  public AccountType accountType;
  public Double loanProductId;
  public String loanProductName;
  public Double loanAmountRequested;
  public Double termFrequency;
  public List<Double> submittedOnDate;
  public Double interestRatePerPeriod;
  public Boolean isCoApplicant;
  public Boolean isStalePeriodExceeded;
  public Boolean isWorkflowArchived;
  public Boolean allowUpfrontCollection;
  public Boolean allowsDisbursementToGroupBankAccounts;
  public Boolean isFlatInterestRate;
  public Boolean synRepaymentWithMeeting;
  public Boolean isTopup;
  public Double subStatus;
  public String leadType;
  public String losProductKey;
  public Boolean isRepaymentDayRuleConfigured;
  public Boolean onHold;

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class Status {
    public Double id;
    public String code;
    public String value;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class AccountType {
    public Double id;
    public String code;
    public String value;
  }
}
