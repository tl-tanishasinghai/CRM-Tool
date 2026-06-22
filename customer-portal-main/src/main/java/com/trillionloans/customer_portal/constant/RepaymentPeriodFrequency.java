package com.trillionloans.customer_portal.constant;

import lombok.Getter;

@Getter
public enum RepaymentPeriodFrequency {
  MONTHLY(0, "Day"),
  WEEKLY(1, "Week"),
  DAILY(2, "Month");

  private final int value;
  private final String periodName;

  RepaymentPeriodFrequency(int value, String periodName) {
    this.value = value;
    this.periodName = periodName;
  }

  public static RepaymentPeriodFrequency fromValue(int value) {
    for (RepaymentPeriodFrequency frequency : values()) {
      if (frequency.getValue() == value) {
        return frequency;
      }
    }
    throw new IllegalArgumentException("Unknown repayment period frequency value: " + value);
  }
}
