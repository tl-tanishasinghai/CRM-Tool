package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseRpsDTO {
  private Integer repaymentEvery;
  private Double proposedPrincipal;
  private List<Double> expectedFirstRepaymentOnDate;
  private LoanType loanType;
  private LoanAdditionalDetailsData loanAdditionalDetailsData;
  private Boolean considerAllDisbursementsInSchedule;
  private Boolean isInterestRecalculationEnabled;
  private Boolean deferPaymentsForHalfTheLoanTerm;
  private Double approvedPrincipal;
  private Boolean brokenPeriodInterestCollectAtDisbursement;
  private List<Double> anchors;
  private String transactionProcessingStrategyName;
  private Double actualNumberOfRepayments;
  private String transactionProcessingStrategyCode;
  private Double principal;
  private Boolean isUpfrontInterestEnabled;
  private Boolean isLoanProductLinkedToFloatingRate;
  private LoanProductInterestRateData loanProductInterestRateData;
  private String accountNo;
  private Boolean isLocked;
  private Boolean isTopup;
  private Currency currency;
  private Boolean isCancellationAllowed;
  private Double id;
  private Boolean isFloatingInterestRate;
  private Double loanProductId;
  private Double feeChargesAtDisbursementCharged;
  private String productType;
  private Double numberOfRepayments;
  private Boolean isClientVerified;
  private Double principalNetDisbursed;
  private Boolean considerFutureDisbursmentsInSchedule;
  private BrokenPeriodMethodType brokenPeriodMethodType;
  private List<Double> eventBasedCharges;
  private Boolean isVariableInstallmentsAllowed;
  private Boolean allowsDisbursementToGroupBankAccounts;
  private Double transactionProcessingStrategyId;
  private Boolean multiDisburseLoan;
  private Boolean isDpConfigured;
  private Boolean canDisburse;
  private Timeline timeline;
  private List<Double> emiAmountVariations;
  private RepaymentSchedule repaymentSchedule;
  private String loanProductName;
  private Status status;
  private Double brokenPeriodInterest;
  private Double currentInterestRate;
  private OriginalSchedule originalSchedule;
}
