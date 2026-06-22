package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum BreType {
  SANCTION("SANCTION"),
  TRANSACTION("TRANSACTION"),
  ;

  private final String displayName;

  BreType(String displayName) {
    this.displayName = displayName;
  }
}
