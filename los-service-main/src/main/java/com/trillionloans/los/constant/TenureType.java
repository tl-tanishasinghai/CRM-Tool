package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum TenureType {
  DAYS("DAYS"),
  MONTHS("MONTHS");

  private final String value;

  TenureType(String value) {
    this.value = value;
  }
}
