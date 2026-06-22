package com.trillionloans.crm.integration.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LmsRpsWithDpdDto(
    String accountNo, Integer dpdDays, RepaymentSchedule repaymentSchedule, Status status) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Status(String value, Boolean active, Boolean closed, Boolean overpaid) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RepaymentSchedule(
      Double totalPrincipalDisbursed,
      Double totalPrincipalPaid,
      Double totalInterestCharged,
      Double totalFeeChargesCharged,
      Double totalPenaltyChargesCharged,
      Double totalRepayment,
      Double totalOutstanding,
      List<RepaymentPeriod> periods) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RepaymentPeriod(
      Integer period,
      List<Integer> dueDate,
      List<Integer> obligationsMetOnDate,
      Boolean complete,
      Double principalDue,
      Double principalPaid,
      Double principalOutstanding,
      Double interestOriginalDue,
      Double totalDueForPeriod,
      Double totalPaidForPeriod,
      Double totalOutstandingForPeriod,
      Double totalOverdue) {}
}
