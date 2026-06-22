package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum MandateFrequency {
  ADHOC("Adhoc"),
  INTRA_DAY("IntraDay"),
  DAILY("Daily"),

  WEEKLY("Weekly"),
  MONTHLY("Monthly"),
  BI_MONTHLY("BiMonthly"),

  QUARTERLY("Quarterly"),
  SEMI_ANNUALLY("Semiannually"),
  YEARLY("Yearly");

  private final String displayName;

  MandateFrequency(String displayName) {
    this.displayName = displayName;
  }
}
