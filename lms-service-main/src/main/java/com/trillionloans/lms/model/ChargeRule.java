package com.trillionloans.lms.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record ChargeRule(
    String name,
    String shortCode,
    Type type,
    BigDecimal value,
    Boolean gstApplicable,
    Long m2pChargeTypeId,
    Integer offsetDays,
    Trigger trigger,
    List<String> paymentStatusAllowed,
    List<DailyRateSlab> dailyRates,
    String postingDateMode,
    Boolean postingEnabled) {

  public enum Type {
    FLAT,
    PCT_PI_REMAINING
  }

  public enum Trigger {
    NONE,
    DPD_EQUALS,
    MONTH_END_OVERDUE
  }

  public boolean isPercent() {
    return type == Type.PCT_PI_REMAINING;
  }

  public String getPostingDateMode() {
    return (postingDateMode != null && !postingDateMode.trim().isEmpty())
        ? postingDateMode.trim().toUpperCase()
        : "RUN_DATE";
  }

  public boolean isValidPostingMode() {
    String mode = getPostingDateMode();
    return "RUN_DATE".equals(mode) || "EMI_DUE_DATE".equals(mode);
  }

  public boolean isPostingEnabled() {
    return postingEnabled == null || postingEnabled;
  }
}
