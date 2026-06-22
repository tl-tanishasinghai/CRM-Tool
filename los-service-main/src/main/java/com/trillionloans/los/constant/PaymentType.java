package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum PaymentType {
  ONLINE_TRANSFER("online transfer"),
  MANUAL("Manual");

  private final String displayName;

  PaymentType(String displayName) {
    this.displayName = displayName;
  }
}
