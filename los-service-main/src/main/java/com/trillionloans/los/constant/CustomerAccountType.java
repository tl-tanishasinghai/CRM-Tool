package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum CustomerAccountType {
  SAVINGS("SAVINGS"),
  CURRENT("CURRENT"),
  OTHER("OTHER");

  private final String displayName;

  CustomerAccountType(String displayName) {
    this.displayName = displayName;
  }
}
