package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum KycValidationVendors {
  KARZA("KARZA"),
  TRILLION("TRILLION");

  private final String displayName;

  KycValidationVendors(String displayName) {
    this.displayName = displayName;
  }
}
