package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RepaymentSchedule {
  private Double totalPenaltyChargesCharged;
  private Double totalFeeChargesCharged;
  private Double loanTermInDays;
  private Double totalOutstanding;
  private Double totalInterestCharged;
  private Double totalRepaymentExpected;
  private Double totalWrittenOff;
  private Double totalPrincipalExpected;
  private Double totalPrincipalDisbursed;
  private Double totalPaidLate;
  private Double totalPrincipalPaid;
  private Double totalPaidInAdvance;
  private Double totalAdvancePayment;
  private Double totalWaived;
  private List<PeriodsItem> periods;
  private Currency currency;
  private Double totalRepayment;
}
