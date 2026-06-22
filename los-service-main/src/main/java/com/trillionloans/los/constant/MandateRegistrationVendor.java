package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum MandateRegistrationVendor {
  DIGIO("DIGIO");

  private final String displayName;

  MandateRegistrationVendor(String displayName) {
    this.displayName = displayName;
  }
}
