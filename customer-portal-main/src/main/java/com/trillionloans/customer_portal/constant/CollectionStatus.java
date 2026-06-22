package com.trillionloans.customer_portal.constant;

import lombok.Getter;

@Getter
public enum CollectionStatus {
  INITIATED("INITIATED"),
  EXCESS_AMOUNT("EXCESS_AMOUNT"),
  PAID("PAID");

  private final String displayName;

  CollectionStatus(String displayName) {
    this.displayName = displayName;
  }
}