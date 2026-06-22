package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum CreditLineStatus {
  PENDING("PENDING"),
  REJECTED("REJECTED"),
  OPS_APPROVED("OPS_APPROVED"),
  CREATED("CREATED"),
  APPROVED("APPROVED"),
  ACTIVE("ACTIVE");

  private final String value;

  CreditLineStatus(String value) {
    this.value = value;
  }
}
