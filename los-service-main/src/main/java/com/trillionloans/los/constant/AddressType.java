package com.trillionloans.los.constant;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum AddressType {
  PERMANENT("Permanent Address"),
  RESIDENTIAL("Residential Address"),
  VOTER("Voter Address"),
  AADHAAR("Aadhaar Address"),
  KYC("kycAddress"),
  OFFICE("Office Address"),
  BUSINESS("Business Address");

  private final String displayName;

  AddressType(String displayName) {
    this.displayName = displayName;
  }

  public static String getDisplayName(String type) {
    if (type == null) return null;
    return Arrays.stream(AddressType.values())
        .filter(e -> e.name().equalsIgnoreCase(type))
        .map(AddressType::getDisplayName)
        .findFirst()
        .orElse(type);
  }
}
