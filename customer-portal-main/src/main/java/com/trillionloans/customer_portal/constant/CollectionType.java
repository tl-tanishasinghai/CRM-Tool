package com.trillionloans.customer_portal.constant;

import lombok.Getter;

@Getter
public enum CollectionType {
  FULL_AMOUNT("FULL_AMOUNT"),
  NEXT_DUE_AMOUNT("NEXT_DUE_AMOUNT"),
  CURRENT_DUE_AMOUNT("CURRENT_DUE_AMOUNT");
  private final String displayName;

  CollectionType(String displayName) {
    this.displayName = displayName;
  }
}