package com.trillionloans.crm.model;

import com.trillionloans.crm.model.CrmModels.LoanRepaymentSchedule;
import com.trillionloans.crm.model.CrmModels.LoanTransactionHistory;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class WrapperModels {

  private WrapperModels() {}

  public enum ApiStatus {
    SUCCESS,
    FAILURE
  }

  public enum PiiField {
    MOBILE,
    EMAIL,
    ADDRESS
  }

  public record ApiResponse<T>(ApiStatus status, T data, String errorCode, String errorMessage) {

    public static <T> ApiResponse<T> success(T data) {
      return new ApiResponse<>(ApiStatus.SUCCESS, data, null, null);
    }

    public static <T> ApiResponse<T> failure(String errorCode, String errorMessage) {
      return new ApiResponse<>(ApiStatus.FAILURE, null, errorCode, errorMessage);
    }
  }

  public record MaskedIdentity(
      String name,
      String mobileMasked,
      String emailMasked,
      String addressMasked,
      String addressShort,
      String panLast4,
      String dateOfBirth,
      String leadId,
      String ucic,
      String clientId,
      String dataSource) {}

  public record LoanMiniSummary(
      String loanAccountNumber,
      String loanApplicationId,
      String product,
      String status,
      String lenderName,
      BigDecimal sanctionedAmount,
      BigDecimal disbursedAmount,
      BigDecimal netDisbursedAmount,
      BigDecimal processingFee,
      Integer tenureMonths,
      BigDecimal roiPercent,
      String disbursementDate,
      Integer dpdDays,
      BigDecimal outstanding,
      BigDecimal emi) {}

  /** FR1: customer summary by leadId or mobile aggregate. */
  public record CustomerSummaryResponse(
      boolean customerFound,
      String leadId,
      MaskedIdentity identity,
      List<LoanMiniSummary> activeLoans,
      List<LoanMiniSummary> closedLoans,
      List<LoanMiniSummary> loanAccounts,
      String dataSource) {}

  /** Profile card payload for CRM overview. */
  public record CustomerProfileResponse(
      MaskedIdentity identity, List<LoanMiniSummary> loanAccounts, String dataSource) {}

  public record PiiRevealRequest(@NotNull PiiField field, String reason) {}

  public record PiiRevealResponse(
      PiiField field, String value, String auditId, Instant revealedAt) {}

  public record PiiRevealAudit(
      String id,
      String leadId,
      String agentId,
      PiiField field,
      String reason,
      Instant createdAt) {}

  public record AmountBreakup(
      BigDecimal principal, BigDecimal interest, BigDecimal charges, BigDecimal penalties) {}

  public record LoanSummaryBlock(
      BigDecimal sanctionedAmount,
      BigDecimal disbursedAmount,
      BigDecimal netDisbursedAmount,
      BigDecimal processingFeeAndCharges,
      Integer tenureMonths,
      BigDecimal roiPercent,
      String disbursementDate) {}

  public record CurrentPositionBlock(
      BigDecimal principalOutstanding,
      BigDecimal totalOutstanding,
      String nextInstallmentDate,
      BigDecimal nextInstallmentAmount,
      AmountBreakup nextInstallmentBreakup,
      BigDecimal overdueAmount,
      AmountBreakup overdueBreakup,
      Integer dpdDays) {}

  public record LifetimeTotalsBlock(
      BigDecimal principalPaid,
      BigDecimal interestPaid,
      BigDecimal chargesPaid,
      BigDecimal penaltiesPaid,
      BigDecimal totalPaid) {}

  public record RecentPaymentRow(
      String date, BigDecimal amount, String type, String status) {}

  public record ForeclosureQuoteBlock(
      BigDecimal totalDue,
      AmountBreakup breakup,
      String validUntil,
      boolean available) {}

  public record ApplicationStatusBlock(
      String loanApplicationId, String receivedDate, String lspName, String stage) {}

  /** FR2: full loan detail for agent / bot consumption. */
  public record LoanDetailResponse(
      String loanAccountNumber,
      String productName,
      String lenderName,
      String status,
      String normalizedStatus,
      Integer dpdDays,
      LoanSummaryBlock summary,
      CurrentPositionBlock currentPosition,
      LifetimeTotalsBlock lifetimeTotals,
      List<RecentPaymentRow> recentPayments,
      ForeclosureQuoteBlock foreclosureQuote,
      ApplicationStatusBlock applicationStatus,
      List<String> documentsAvailable,
      LoanRepaymentSchedule repaymentSchedule,
      LoanTransactionHistory transactions,
      String dataSource) {}
}