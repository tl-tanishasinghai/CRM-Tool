package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodsItem {
  private Double totalInstallmentAmountForPeriod;
  private Double feeChargesWaived;
  private Double feeChargesPaid;
  private List<Integer> dueDate;
  private Boolean recalculatedInterestComponent;
  private Double interestAccruable;
  private Double interestWaived;
  private Double advancePaymentAmount;
  private Double penaltyChargesPaid;
  private Double feeChargesOutstanding;
  private Double totalWrittenOffForPeriod;
  private Double daysInPeriod;
  private Double interestWrittenOff;
  private Double totalWaivedForPeriod;
  private Double penaltyChargesWaived;
  private Double totalOriginalDueForPeriod;
  private Double totalOutstandingForPeriod;
  private Double interestOutstanding;
  private Double totalPaidLateForPeriod;
  private Double interestOriginalDue;
  private Double interestPaid;
  private Integer period;
  private Double principalOutstanding;
  private Double principalLoanBalanceOutstanding;
  private Double principalDue;
  private Double interestAdjustedDueToGrace;
  private Double totalActualCostOfLoanForPeriod;
  private Double penaltyChargesDue;
  private Double penaltyChargesOutstanding;
  private Double feeChargesWrittenOff;
  private Double principalPaid;
  private Double totalPaidForPeriod;
  private List<Integer> fromDate;
  private Double principalOriginalDue;
  private Double feeChargesDue;
  private Double totalDueForPeriod;
  private Double totalPaidInAdvanceForPeriod;
  private Double interestDue;
  private Double principalWrittenOff;
  private Double penaltyChargesWrittenOff;
  private Boolean complete;
  private Double totalOverdue;
  private Double principalDisbursed;
  private List<Integer> obligationsMetOnDate;
  private List<Integer> emiClearedDate;
}
