package com.trillionloans.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record EmiReportRow(
    long loanId,
    int period,
    @JsonFormat(pattern = "MMM d, uuuu", locale = "en") LocalDate fromDate,
    @JsonFormat(pattern = "MMM d, uuuu", locale = "en") LocalDate dueDate,
    @JsonFormat(pattern = "MMM d, uuuu", locale = "en") LocalDate obligationsMetOnDate,
    boolean complete,
    BigDecimal principalDue,
    BigDecimal principalPaid,
    BigDecimal principalWrittenOff,
    BigDecimal interestDue,
    BigDecimal interestPaid,
    BigDecimal interestWaived,
    BigDecimal interestWrittenOff,
    BigDecimal feeChargesDue,
    BigDecimal feeChargesPaid,
    BigDecimal feeChargesWaived,
    BigDecimal feeChargesWrittenOff,
    BigDecimal penaltyChargesDue,
    BigDecimal penaltyChargesPaid,
    BigDecimal penaltyChargesWaived,
    BigDecimal penaltyChargesWrittenOff,
    BigDecimal totalPaidInAdvanceForPeriod,
    BigDecimal totalPaidLateForPeriod,
    BigDecimal totalOutstanding,
    boolean isNpa,
    String productCode) {

  public String paymentStatus() {
    BigDecimal paidOrWritten =
        safe(principalPaid)
            .add(safe(principalWrittenOff))
            .add(safe(interestPaid))
            .add(safe(interestWrittenOff));
    BigDecimal outstanding = safe(principalDue).add(safe(interestDue)).subtract(paidOrWritten);
    if (outstanding.signum() <= 0) {
      return "FULLY PAID";
    } else if (paidOrWritten.signum() > 0) {
      return "PARTIAL PAID";
    }
    return "NOT PAID";
  }

  private static BigDecimal safe(BigDecimal val) {
    return val == null ? BigDecimal.ZERO : val;
  }
}
