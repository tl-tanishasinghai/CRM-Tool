package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RPSResponseWithDPD {
  public Long id;
  public String accountNo;
  public Status status;
  public Long clientId;
  public String clientAccountNo;
  public String clientName;
  public Long clientOfficeId;
  public Long loanProductId;
  public String loanProductName;
  public Timeline timeline;
  public RepaymentSchedule repaymentSchedule;
  public Integer dpdDays;

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Status {
    public Integer id;
    public String code;
    public String value;
    public Boolean pendingApproval;
    public Boolean waitingForDisbursal;
    public Boolean active;
    public Boolean closedObligationsMet;
    public Boolean closedWrittenOff;
    public Boolean closedRescheduled;
    public Boolean closed;
    public Boolean overpaid;
    public Boolean transferInProgress;
    public Boolean transferOnHold;
    public Boolean underTransfer;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CodeValue {
    public Integer id;
    public String code;
    public String value;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Currency {
    public String code;
    public String name;
    public Integer decimalPlaces;
    public Integer inMultiplesOf;
    public String displaySymbol;
    public String nameCode;
    public String displayLabel;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Timeline {
    public List<Integer> submittedOnDate;
    public String submittedByUsername;
    public String submittedByFirstname;
    public String submittedByLastname;
    public List<Integer> approvedOnDate;
    public String approvedByUsername;
    public String approvedByFirstname;
    public String approvedByLastname;
    public List<Integer> expectedDisbursementDate;
    public List<Integer> actualDisbursementDate;
    public String disbursedByUsername;
    public String disbursedByFirstname;
    public String disbursedByLastname;
    public List<Integer> expectedMaturityDate;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RepaymentSchedule {
    public Currency currency;
    public Integer loanTermInDays;
    public Double totalPrincipalDisbursed;
    public Double totalPrincipalExpected;
    public Double totalPrincipalPaid;
    public Double totalInterestCharged;
    public Double totalFeeChargesCharged;
    public Double totalPenaltyChargesCharged;
    public Double totalWaived;
    public Double totalWrittenOff;
    public Double totalRepaymentExpected;
    public Double totalRepayment;
    public Double totalPaidInAdvance;
    public Double totalPaidLate;
    public Double totalOutstanding;
    public Double totalAdvancePayment;
    public List<RepaymentPeriod> periods;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RepaymentPeriod {
    public Integer period;
    public List<Integer> fromDate;
    public List<Integer> dueDate;
    public List<Integer> obligationsMetOnDate;
    public Boolean complete;
    public Integer daysInPeriod;
    public Double principalDisbursed;
    public Double principalOriginalDue;
    public Double principalDue;
    public Double principalPaid;
    public Double principalWrittenOff;
    public Double principalOutstanding;
    public Double principalLoanBalanceOutstanding;
    public Double interestOriginalDue;
    public Double totalDueForPeriod;
    public Double totalPaidForPeriod;
    public Double totalPaidInAdvanceForPeriod;
    public Double totalPaidLateForPeriod;
    public Double totalWaivedForPeriod;
    public Double totalWrittenOffForPeriod;
    public Double totalOutstandingForPeriod;
    public Double totalOverdue;
  }
}
