package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum MandateAuthMode {
  API("API"),
  ESIGN("ESIGN"),
  PHYSICAL("PHYSICAL");

  private final String displayName;

  MandateAuthMode(String displayName) {
    this.displayName = displayName;
  }
}
