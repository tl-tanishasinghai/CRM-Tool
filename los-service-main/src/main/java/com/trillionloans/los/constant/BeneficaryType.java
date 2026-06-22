package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum BeneficaryType {
  SELF("Self"),
  THIRD_PARTY("Third Party"),
  MERCHANT("Merchant");

  private final String value;

  BeneficaryType(String value) {
    this.value = value;
  }
}
