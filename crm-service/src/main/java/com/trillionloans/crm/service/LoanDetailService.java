package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.LoanRepaymentSchedule;
import com.trillionloans.crm.model.CrmModels.LoanSummary;
import com.trillionloans.crm.model.CrmModels.LoanTransactionHistory;
import com.trillionloans.crm.model.CrmModels.LoanTransactionRow;
import com.trillionloans.crm.model.CrmModels.RepaymentScheduleRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LoanDetailService {

  private static final DateTimeFormatter DISPLAY_DATE =
      DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

  private final ExternalDataService externalDataService;

  public LoanDetailService(ExternalDataService externalDataService) {
    this.externalDataService = externalDataService;
  }

  public LoanRepaymentSchedule getRepaymentSchedule(String leadId, String loanAccountNumber) {
    return buildMockSchedule(leadId, loanAccountNumber, "REPAYMENT");
  }

  public LoanRepaymentSchedule getOriginalSchedule(String leadId, String loanAccountNumber) {
    return buildMockSchedule(leadId, loanAccountNumber, "ORIGINAL");
  }

  public LoanTransactionHistory getTransactions(String leadId, String loanAccountNumber) {
    return buildMockTransactions(leadId, loanAccountNumber);
  }

  private LoanSummary findLoan(String leadId, String loanAccountNumber) {
    return externalDataService.getLoanSummaries(leadId).stream()
        .filter(loan -> loan.loanAccountNumber().equals(loanAccountNumber))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Loan not found for lead"));
  }

  private LoanRepaymentSchedule buildMockSchedule(
      String leadId, String loanAccountNumber, String scheduleType) {
    LoanSummary loan = findLoan(leadId, loanAccountNumber);
    List<RepaymentScheduleRow> periods = new ArrayList<>();
    LocalDate disbursementDate = loan.disbursementDate();
    BigDecimal feeAtDisbursement =
        loan.principal().multiply(BigDecimal.valueOf(0.01414)).setScale(0, RoundingMode.HALF_UP);

    periods.add(
        new RepaymentScheduleRow(
            null,
            null,
            formatDate(disbursementDate),
            "",
            "",
            BigDecimal.ZERO,
            loan.principal(),
            BigDecimal.ZERO,
            feeAtDisbursement,
            BigDecimal.ZERO,
            feeAtDisbursement,
            feeAtDisbursement,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO));

    BigDecimal outstanding = loan.principal();
    LocalDate dueDate = loan.nextDueDate() != null ? loan.nextDueDate() : disbursementDate.plusMonths(1);
    int installments = Math.max(loan.tenureMonths(), 1);

    for (int period = 1; period <= installments; period++) {
      BigDecimal interest =
          outstanding
              .multiply(loan.interestRate())
              .divide(BigDecimal.valueOf(1200), 0, RoundingMode.HALF_UP);
      BigDecimal principalDue = loan.emi().subtract(interest).max(BigDecimal.ZERO);
      if (period == installments) {
        principalDue = outstanding;
      }
      BigDecimal due = principalDue.add(interest);
      BigDecimal paid =
          loan.status().equalsIgnoreCase("Closed") || period <= 2
              ? due
              : BigDecimal.ZERO;
      outstanding = outstanding.subtract(principalDue).max(BigDecimal.ZERO);

      periods.add(
          new RepaymentScheduleRow(
              period,
              30,
              formatDate(dueDate),
              paid.compareTo(BigDecimal.ZERO) > 0 ? formatDate(dueDate) : "",
              paid.compareTo(BigDecimal.ZERO) > 0 ? formatDate(dueDate) : "",
              principalDue,
              outstanding,
              interest,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              due,
              paid,
              due.subtract(paid),
              BigDecimal.ZERO,
              BigDecimal.ZERO));

      dueDate = dueDate.plusMonths(1);
    }

    return new LoanRepaymentSchedule(loanAccountNumber, scheduleType, periods);
  }

  private LoanTransactionHistory buildMockTransactions(String leadId, String loanAccountNumber) {
    LoanSummary loan = findLoan(leadId, loanAccountNumber);
    List<LoanTransactionRow> rows = new ArrayList<>();
    LocalDate txnDate = loan.disbursementDate();
    BigDecimal feeAtDisbursement =
        loan.principal().multiply(BigDecimal.valueOf(0.01414)).setScale(0, RoundingMode.HALF_UP);

    rows.add(
        new LoanTransactionRow(
            "Head Office",
            formatDate(txnDate),
            "Repayment (at time of disbursement)",
            feeAtDisbursement,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            feeAtDisbursement,
            BigDecimal.ZERO,
            loan.principal(),
            "Real Time",
            BigDecimal.ZERO));

    rows.add(
        new LoanTransactionRow(
            "Head Office",
            formatDate(txnDate),
            "Disbursement",
            feeAtDisbursement,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            loan.principal(),
            "Real Time",
            BigDecimal.ZERO));

    BigDecimal disbursedAmount = loan.principal().subtract(feeAtDisbursement);
    rows.add(
        new LoanTransactionRow(
            "Head Office",
            formatDate(txnDate),
            "Disbursement",
            disbursedAmount,
            disbursedAmount,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            loan.principal().subtract(disbursedAmount),
            "Real Time",
            BigDecimal.ZERO));

    if (loan.paidAmount().compareTo(BigDecimal.ZERO) > 0) {
      rows.add(
          new LoanTransactionRow(
              "Head Office",
              formatDate(txnDate.plusMonths(1)),
              "Repayment",
              loan.emi(),
              loan.emi().subtract(BigDecimal.valueOf(1200)).max(BigDecimal.ZERO),
              BigDecimal.valueOf(1200),
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              loan.outstanding(),
              "Real Time",
              BigDecimal.ZERO));
    }

    return new LoanTransactionHistory(loanAccountNumber, rows);
  }

  private String formatDate(LocalDate date) {
    return date.format(DISPLAY_DATE);
  }
}
