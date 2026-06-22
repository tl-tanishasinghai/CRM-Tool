package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum MaritalStatus {
  UNMARRIED("Unmarried"),
  MARRIED("Married"),
  WIDOWED("Widowed"),
  DIVORCED("Divorced");

  private final String displayName;

  MaritalStatus(String displayName) {
    this.displayName = displayName;
  }
}
