package com.trillionloans.customer_portal.constant;

import lombok.Getter;

@Getter
public enum PaymentStatus {
  INITIATED("INITIATED"),
  PAID("PAID"),
  PARTIALLY_PAID("PARTIALLY_PAID"),
  EXPIRED("EXPIRED"),
  FAILED("FAILED"),
  CANCELLED("CANCELLED");

  private final String displayName;

  PaymentStatus(String displayName) {
    this.displayName = displayName;
  }
}