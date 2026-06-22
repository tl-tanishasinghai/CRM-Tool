package com.trillionloans.los.constant;

public enum RepaymentFrequency {
  DAILY("0", "Daily"),
  WEEKLY("1", "Weekly"),
  MONTHLY("2", "Monthly");

  private final String code;
  private final String description;

  RepaymentFrequency(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public static String fromCode(String code) {
    if (code == null) return null;
    for (RepaymentFrequency freq : values()) {
      if (freq.code.equals(code)) {
        return freq.description;
      }
    }
    return code;
  }
}
