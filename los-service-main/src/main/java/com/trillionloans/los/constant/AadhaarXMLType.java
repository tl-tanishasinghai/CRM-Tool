package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum AadhaarXMLType {
  DIGI_LOCKER("DIGIO"),
  OKYC("OKYC");

  private final String displayName;

  AadhaarXMLType(String xmlType) {
    this.displayName = xmlType;
  }
}
