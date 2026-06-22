package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum DocumentType {
  PASSPORT("Passport"),
  VOTER_ID("Voter Id"),
  AADHAAR("Aadhaar"),
  PAN("Pan"),
  AADHAAR_OKYC("Aadhaar_OKYC");

  private final String displayName;

  DocumentType(String displayName) {
    this.displayName = displayName;
  }
}
