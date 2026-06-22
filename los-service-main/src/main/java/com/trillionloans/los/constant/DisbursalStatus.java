package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum DisbursalStatus {
  INIT("INIT"),
  AUTO_INI("AUTO_INI"),
  MANUAL_INI("MANUAL_INI"),
  MANUAL_M2P("MANUAL_M2P"),
  MANUAL_BATCHED("MANUAL_BATCHED"),
  SUCCESS("SUCCESS"),
  REJECTED("REJECTED");

  private final String displayName;

  DisbursalStatus(String displayName) {
    this.displayName = displayName;
  }
}
