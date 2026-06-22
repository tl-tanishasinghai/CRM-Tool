package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum QcCheckStatus {
  APPROVED("APPROVED"),
  REJECTED("REJECTED"),
  PENDING("PENDING");

  private final String value;

  QcCheckStatus(String value) {
    this.value = value;
  }
}
