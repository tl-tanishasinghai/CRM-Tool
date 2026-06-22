package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum PartnershipType {
  CO_LENDING("Co-lending"),
  CHANNEL("Channel");

  private final String displayName;

  PartnershipType(String displayName) {
    this.displayName = displayName;
  }

  public static boolean isValid(String partnershipType) {
    for (PartnershipType type : values()) {
      if (type.getDisplayName().equalsIgnoreCase(partnershipType)) {
        return true;
      }
    }
    return false;
  }
}
