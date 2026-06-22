package com.trillionloans.lms.model.response;

import com.trillionloans.lms.model.dto.*;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response class representing the repayment schedule information for a loan. */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentScheduleResponse {

  private Long id;
  private String accountNo;
  private StatusDTO status;
  private GeneralResponseDTO subStatus;
  private int loanProductId;
  private String loanProductName;
  private boolean isLoanProductLinkedToFloatingRate;
  private GeneralResponseDTO loanType;
  private CurrencyDTO currency;
  private double principal;
  private double approvedPrincipal;
  private double proposedPrincipal;
  private double brokenPeriodInterest;
  private int numberOfRepayments;
  private int repaymentEvery;
  private boolean isFloatingInterestRate;
  private int transactionProcessingStrategyId;
  private String transactionProcessingStrategyName;
  private String transactionProcessingStrategyCode;
  private boolean isCancellationAllowed;
  private TimelineDTO timeline;
  private RepaymentScheduleDTO repaymentSchedule;
  private RepaymentScheduleDTO originalSchedule;
  private LoanScheduleHistoryDataDTO loanScheduleHistoryData;
  private double feeChargesAtDisbursementCharged;
  private boolean multiDisburseLoan;
  private boolean canDisburse;
  private List<Objects> emiAmountVariations;
  private boolean isTopup;
  private boolean isInterestRecalculationEnabled;
  private boolean isVariableInstallmentsAllowed;
  private GeneralResponseDTO brokenPeriodMethodType;
  private boolean considerFutureDisbursmentsInSchedule;
  private boolean considerAllDisbursementsInSchedule;
  private boolean isLocked;
  private boolean deferPaymentsForHalfTheLoanTerm;
  private boolean isClientVerified;
  private boolean allowsDisbursementToGroupBankAccounts;
  private boolean isDpConfigured;
  private boolean brokenPeriodInterestCollectAtDisbursement;
  private boolean isUpfrontInterestEnabled;
  private List<LoanScheduleHistoryDataDTO> scheduleHistoryDataList;
  private List<Objects> eventBasedCharges;
  private List<Objects> anchors;
  private String productType;
  private double currentInterestRate;
  private LoanAdditionalDetailsDataDTO loanAdditionalDetailsData;
}
