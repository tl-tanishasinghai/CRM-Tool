package com.trillionloans.crm.service;

import com.trillionloans.crm.integration.lms.LmsDueDetailsDto;
import com.trillionloans.crm.integration.lms.LmsForeclosureDto;
import com.trillionloans.crm.integration.lms.LmsLoanDetailsDto;
import com.trillionloans.crm.integration.lms.LmsRpsWithDpdDto;
import com.trillionloans.crm.integration.lms.LmsTransactionDetailDto;
import com.trillionloans.crm.integration.los.LosLoanApplicationDto;
import com.trillionloans.crm.model.CrmModels.LoanRepaymentSchedule;
import com.trillionloans.crm.model.CrmModels.LoanSummary;
import com.trillionloans.crm.model.CrmModels.LoanTransactionHistory;
import com.trillionloans.crm.model.CrmModels.LoanTransactionRow;
import com.trillionloans.crm.model.CrmModels.RepaymentScheduleRow;
import com.trillionloans.crm.model.WrapperModels.AmountBreakup;
import com.trillionloans.crm.model.WrapperModels.ApplicationStatusBlock;
import com.trillionloans.crm.model.WrapperModels.CurrentPositionBlock;
import com.trillionloans.crm.model.WrapperModels.ForeclosureQuoteBlock;
import com.trillionloans.crm.model.WrapperModels.LifetimeTotalsBlock;
import com.trillionloans.crm.model.WrapperModels.LoanDetailResponse;
import com.trillionloans.crm.model.WrapperModels.LoanSummaryBlock;
import com.trillionloans.crm.model.WrapperModels.RecentPaymentRow;
import com.trillionloans.crm.integration.LoanStatusMapper;
import com.trillionloans.crm.integration.ProductNameMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LoanAggregationService {

  private static final DateTimeFormatter DISPLAY =
      DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
  private static final Set<Long> PAYMENT_TXN_TYPES = Set.of(2L, 9L, 17L);

  private final ExternalDataService externalDataService;
  private final LmsIntegrationService lmsIntegrationService;
  private final LosIntegrationService losIntegrationService;
  private final LoanDetailService loanDetailService;

  public LoanAggregationService(
      ExternalDataService externalDataService,
      LmsIntegrationService lmsIntegrationService,
      LosIntegrationService losIntegrationService,
      LoanDetailService loanDetailService) {
    this.externalDataService = externalDataService;
    this.lmsIntegrationService = lmsIntegrationService;
    this.losIntegrationService = losIntegrationService;
    this.loanDetailService = loanDetailService;
  }

  public LoanDetailResponse getLoanDetail(String leadId, String loanAccountNumber) {
    Optional<LmsLoanDetailsDto> lmsLoan =
        lmsIntegrationService.fetchLoansByLeadId(leadId).stream()
            .filter(loan -> loanAccountNumber.equals(loan.loanAccountNumber()))
            .findFirst();

    LoanSummary fallback =
        externalDataService.getLoanSummaries(leadId).stream()
            .filter(loan -> loan.loanAccountNumber().equals(loanAccountNumber))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Loan not found for lead"));

    String dataSource = lmsLoan.isPresent() ? "LMS" : fallback.message().contains("mock") ? "MOCK" : "CRM";

    Optional<LmsRpsWithDpdDto> rpsOpt = lmsIntegrationService.fetchRpsWithDpd(loanAccountNumber);
    Optional<LmsDueDetailsDto> dueOpt =
        lmsIntegrationService.fetchDueAsOnDate(loanAccountNumber, LocalDate.now());
    Optional<LmsTransactionDetailDto> txnOpt =
        lmsIntegrationService.fetchTransactions(loanAccountNumber);
    Optional<LmsForeclosureDto> foreclosureOpt =
        lmsIntegrationService.fetchForeclosure(loanAccountNumber, LocalDate.now());

    int dpd = rpsOpt.map(LmsRpsWithDpdDto::dpdDays).orElse(fallback.dpd() != null ? fallback.dpd() : 0);
    String baseStatus = lmsLoan.map(l -> LoanStatusMapper.mapStatus(l.status())).orElse(fallback.status());
    String normalizedStatus = normalizeStatus(baseStatus, dpd, dueOpt);

    LoanSummaryBlock summary = buildSummaryBlock(lmsLoan.orElse(null), fallback);
    CurrentPositionBlock currentPosition = buildCurrentPosition(lmsLoan.orElse(null), fallback, dueOpt, rpsOpt, dpd);
    LifetimeTotalsBlock lifetimeTotals = buildLifetimeTotals(rpsOpt, dueOpt, fallback);
    List<RecentPaymentRow> recentPayments = buildRecentPayments(txnOpt);
    ForeclosureQuoteBlock foreclosureQuote = buildForeclosureQuote(foreclosureOpt);
    ApplicationStatusBlock applicationStatus = buildApplicationStatus(leadId, lmsLoan.orElse(null), fallback);
    List<String> documents = buildDocuments(baseStatus);

    LoanRepaymentSchedule schedule =
        rpsOpt.map(rps -> mapRpsSchedule(loanAccountNumber, rps))
            .orElseGet(() -> loanDetailService.getRepaymentSchedule(leadId, loanAccountNumber));

    LoanTransactionHistory transactions =
        txnOpt.map(txn -> mapTransactions(loanAccountNumber, txn))
            .orElseGet(() -> loanDetailService.getTransactions(leadId, loanAccountNumber));

    String product =
        lmsLoan
            .map(
                l ->
                    ProductNameMapper.resolveProductName(
                        l.productCode(), l.productId() != null ? l.productId() : null))
            .orElse(fallback.product());
    String lender =
        lmsLoan.map(LmsLoanDetailsDto::officeName).filter(s -> s != null && !s.isBlank())
            .orElse(fallback.lenderName());

    return new LoanDetailResponse(
        loanAccountNumber,
        product,
        lender,
        baseStatus,
        normalizedStatus,
        dpd,
        summary,
        currentPosition,
        lifetimeTotals,
        recentPayments,
        foreclosureQuote,
        applicationStatus,
        documents,
        schedule,
        transactions,
        dataSource);
  }

  private String normalizeStatus(
      String baseStatus, int dpd, Optional<LmsDueDetailsDto> dueOpt) {
    if ("Active".equalsIgnoreCase(baseStatus)) {
      BigDecimal overdue = dueOpt.map(d -> decimal(d.totalDue())).orElse(BigDecimal.ZERO);
      if (dpd > 0 || overdue.compareTo(BigDecimal.ZERO) > 0) {
        return "Overdue";
      }
      return "Active (No dues)";
    }
    if ("Closed".equalsIgnoreCase(baseStatus)) {
      return "Foreclosed/Closed";
    }
    if ("Written Off".equalsIgnoreCase(baseStatus)) {
      return "Written Off";
    }
    return baseStatus;
  }

  private LoanSummaryBlock buildSummaryBlock(LmsLoanDetailsDto lms, LoanSummary fallback) {
    if (lms != null) {
      BigDecimal principal = decimal(lms.loanAmount());
      BigDecimal net = decimal(lms.netDisbursementAmount());
      return new LoanSummaryBlock(
          principal,
          principal,
          net,
          principal.subtract(net).max(BigDecimal.ZERO),
          lms.tenure() != null ? lms.tenure().intValue() : fallback.tenureMonths(),
          BigDecimal.valueOf(lms.rateOfInterest()).setScale(2, RoundingMode.HALF_UP),
          lms.disbursementDate());
    }
    return new LoanSummaryBlock(
        fallback.principal(),
        fallback.principal(),
        fallback.principal().subtract(fallback.excessAdjusted()).max(BigDecimal.ZERO),
        fallback.excessAdjusted(),
        fallback.tenureMonths(),
        fallback.interestRate(),
        fallback.disbursementDate() != null ? fallback.disbursementDate().toString() : null);
  }

  private CurrentPositionBlock buildCurrentPosition(
      LmsLoanDetailsDto lms,
      LoanSummary fallback,
      Optional<LmsDueDetailsDto> dueOpt,
      Optional<LmsRpsWithDpdDto> rpsOpt,
      int dpd) {
    BigDecimal principalOutstanding =
        lms != null
            ? decimal(lms.totalPrincipalOutstanding())
            : fallback.outstanding();
    BigDecimal totalOutstanding =
        dueOpt.map(d -> decimal(d.totalAmountOutstanding())).orElse(principalOutstanding);
    BigDecimal nextEmi = lms != null ? decimal(lms.emiAmount()) : fallback.emi();

    AmountBreakup nextBreakup =
        dueOpt
            .map(
                d ->
                    new AmountBreakup(
                        decimal(d.prinicpalDue()),
                        decimal(d.interestDue()),
                        decimal(d.feeChargesDue()),
                        decimal(d.penaltyChargesDue())))
            .orElse(new AmountBreakup(nextEmi, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

    BigDecimal overdueAmount = dueOpt.map(d -> decimal(d.totalDue())).orElse(BigDecimal.ZERO);
    AmountBreakup overdueBreakup = nextBreakup;

    String nextDueDate =
        fallback.nextDueDate() != null
            ? DISPLAY.format(fallback.nextDueDate())
            : findNextDueDate(rpsOpt);

    return new CurrentPositionBlock(
        principalOutstanding,
        totalOutstanding,
        nextDueDate,
        nextEmi,
        nextBreakup,
        overdueAmount,
        overdueBreakup,
        dpd);
  }

  private LifetimeTotalsBlock buildLifetimeTotals(
      Optional<LmsRpsWithDpdDto> rpsOpt,
      Optional<LmsDueDetailsDto> dueOpt,
      LoanSummary fallback) {
    if (rpsOpt.isPresent() && rpsOpt.get().repaymentSchedule() != null) {
      var schedule = rpsOpt.get().repaymentSchedule();
      BigDecimal principalPaid = decimal(schedule.totalPrincipalPaid());
      BigDecimal interestPaid = decimal(schedule.totalInterestCharged());
      BigDecimal chargesPaid = decimal(schedule.totalFeeChargesCharged());
      BigDecimal penaltiesPaid = decimal(schedule.totalPenaltyChargesCharged());
      BigDecimal total =
          dueOpt
              .map(d -> decimal(d.totalAmountPaid()))
              .orElse(principalPaid.add(interestPaid).add(chargesPaid).add(penaltiesPaid));
      return new LifetimeTotalsBlock(
          principalPaid, interestPaid, chargesPaid, penaltiesPaid, total);
    }
    return new LifetimeTotalsBlock(
        fallback.paidAmount(),
        BigDecimal.ZERO,
        fallback.excessAdjusted(),
        BigDecimal.ZERO,
        fallback.paidAmount());
  }

  private List<RecentPaymentRow> buildRecentPayments(Optional<LmsTransactionDetailDto> txnOpt) {
    if (txnOpt.isEmpty() || txnOpt.get().transactions() == null) {
      return List.of();
    }
    return txnOpt.get().transactions().stream()
        .filter(Objects::nonNull)
        .filter(txn -> txn.manuallyReversed() == null || !txn.manuallyReversed())
        .filter(txn -> txn.type() != null && PAYMENT_TXN_TYPES.contains(txn.type().id()))
        .sorted(
            Comparator.comparing(
                (LmsTransactionDetailDto.Transaction txn) -> txnDateKey(txn.date()),
                Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(5)
        .map(
            txn ->
                new RecentPaymentRow(
                    formatDateList(txn.date()),
                    decimal(txn.amount()),
                    "Repayment",
                    txn.txnValueDateStatus() != null ? txn.txnValueDateStatus().value() : "Posted"))
        .toList();
  }

  private ForeclosureQuoteBlock buildForeclosureQuote(Optional<LmsForeclosureDto> foreclosureOpt) {
    if (foreclosureOpt.isEmpty()) {
      return new ForeclosureQuoteBlock(null, null, null, false);
    }
    LmsForeclosureDto quote = foreclosureOpt.get();
    BigDecimal total =
        quote.netForeclosureAmount() != null
            ? decimal(quote.netForeclosureAmount())
            : quote.amount() != null ? decimal(quote.amount()) : null;
    AmountBreakup breakup =
        new AmountBreakup(
            decimalOrZero(quote.principalPortion()),
            decimalOrZero(quote.interestPortion()),
            decimalOrZero(quote.feeChargesPortion()),
            decimalOrZero(quote.penaltyChargesPortion()));
    return new ForeclosureQuoteBlock(
        total,
        breakup,
        DISPLAY.format(LocalDate.now().plusDays(7)),
        total != null);
  }

  private ApplicationStatusBlock buildApplicationStatus(
      String leadId, LmsLoanDetailsDto lms, LoanSummary fallback) {
    List<LosLoanApplicationDto> apps = losIntegrationService.fetchLoanApplications(leadId);
    String appId =
        lms != null && lms.loanApplicationId() != null
            ? lms.loanApplicationId().toString()
            : fallback.loanApplicationId();

    LosLoanApplicationDto match =
        apps.stream()
            .filter(app -> appId != null && appId.equals(String.valueOf(app.loanApplicationId())))
            .findFirst()
            .orElse(apps.isEmpty() ? null : apps.get(0));

    String lspName = resolveLspName(lms, match, fallback);
    if (match != null) {
      return new ApplicationStatusBlock(
          match.loanApplicationId() != null ? match.loanApplicationId().toString() : appId,
          match.submittedOnDate(),
          lspName,
          defaultText(match.status(), fallback.applicationStatus()));
    }

    return new ApplicationStatusBlock(
        appId,
        fallback.disbursementDate() != null ? DISPLAY.format(fallback.disbursementDate()) : null,
        lspName,
        fallback.applicationStatus());
  }

  private String resolveLspName(
      LmsLoanDetailsDto lms, LosLoanApplicationDto application, LoanSummary fallback) {
    if (application != null
        && application.productCode() != null
        && !application.productCode().isBlank()) {
      return ProductNameMapper.resolveLspName(application.productCode(), null);
    }
    if (lms != null) {
      return ProductNameMapper.resolveLspName(
          lms.productCode(), lms.productId() != null ? lms.productId() : null);
    }
    return defaultText(fallback.product(), "—");
  }

  private List<String> buildDocuments(String status) {
    List<String> docs = new ArrayList<>(List.of("SOA", "LOAN_AGREEMENT"));
    if ("Closed".equalsIgnoreCase(status)) {
      docs.add("NOC");
    }
    return docs;
  }

  private LoanRepaymentSchedule mapRpsSchedule(String loanAccountNumber, LmsRpsWithDpdDto rps) {
    if (rps.repaymentSchedule() == null || rps.repaymentSchedule().periods() == null) {
      return new LoanRepaymentSchedule(loanAccountNumber, "REPAYMENT", List.of());
    }
    List<RepaymentScheduleRow> rows =
        rps.repaymentSchedule().periods().stream()
            .map(
                period ->
                    new RepaymentScheduleRow(
                        period.period(),
                        null,
                        formatDateList(period.dueDate()),
                        formatDateList(period.obligationsMetOnDate()),
                        formatDateList(period.obligationsMetOnDate()),
                        decimalOrZero(period.principalDue()),
                        decimalOrZero(period.principalOutstanding()),
                        decimalOrZero(period.interestOriginalDue()),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        decimalOrZero(period.totalDueForPeriod()),
                        decimalOrZero(period.totalPaidForPeriod()),
                        decimalOrZero(period.totalOutstandingForPeriod()),
                        BigDecimal.ZERO,
                        decimalOrZero(period.totalOverdue())))
            .toList();
    return new LoanRepaymentSchedule(loanAccountNumber, "REPAYMENT", rows);
  }

  private LoanTransactionHistory mapTransactions(
      String loanAccountNumber, LmsTransactionDetailDto txnDto) {
    if (txnDto.transactions() == null) {
      return new LoanTransactionHistory(loanAccountNumber, List.of());
    }
    List<LoanTransactionRow> rows =
        txnDto.transactions().stream()
            .filter(Objects::nonNull)
            .filter(txn -> txn.manuallyReversed() == null || !txn.manuallyReversed())
            .map(
                txn ->
                    new LoanTransactionRow(
                        "—",
                        formatDateList(txn.date()),
                        mapTxnType(txn.type()),
                        decimalOrZero(txn.amount()),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        txn.txnValueDateStatus() != null ? txn.txnValueDateStatus().value() : "Posted",
                        BigDecimal.ZERO))
            .toList();
    return new LoanTransactionHistory(loanAccountNumber, rows);
  }

  private String findNextDueDate(Optional<LmsRpsWithDpdDto> rpsOpt) {
    if (rpsOpt.isEmpty()
        || rpsOpt.get().repaymentSchedule() == null
        || rpsOpt.get().repaymentSchedule().periods() == null) {
      return null;
    }
    return rpsOpt.get().repaymentSchedule().periods().stream()
        .filter(period -> period.complete() == null || !period.complete())
        .map(period -> formatDateList(period.dueDate()))
        .filter(date -> date != null && !date.isBlank())
        .findFirst()
        .orElse(null);
  }

  private String mapTxnType(LmsTransactionDetailDto.Type type) {
    if (type == null || type.id() == null) {
      return "Transaction";
    }
    return switch (type.id().intValue()) {
      case 2, 9, 17 -> "Repayment";
      case 1 -> "Disbursement";
      default -> "Transaction";
    };
  }

  private String formatDateList(List<Integer> parts) {
    if (parts == null || parts.size() < 3) {
      return null;
    }
    try {
      return DISPLAY.format(LocalDate.of(parts.get(0), parts.get(1), parts.get(2)));
    } catch (Exception ex) {
      return null;
    }
  }

  private String txnDateKey(List<Integer> parts) {
    if (parts == null || parts.size() < 3) {
      return null;
    }
    return String.format("%04d-%02d-%02d", parts.get(0), parts.get(1), parts.get(2));
  }

  private BigDecimal decimal(Double value) {
    return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal decimalOrZero(Double value) {
    return value == null ? BigDecimal.ZERO : decimal(value);
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
