package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum LeadDocumentTag {
  ADDRESS_PROOF("ADDRESS_PROOF"),
  ADDITIONAL_ADDRESS_PROOF("ADDITIONAL_ADDRESS_PROOF"),
  PAN("PAN"),
  BUSINESS_PROOF("BUSINESS_PROOF"),
  PAN_PDF_KARZA("PAN_PDF_KARZA"),
  OTHER_DOCUMENT("OTHER_DOCUMENT"),
  PAN_DIGILOCKER("PAN_DIGILOCKER"),
  INSURANCE_DOCUMENT("INSURANCE_DOCUMENT");

  private final String displayName;

  LeadDocumentTag(String displayName) {
    this.displayName = displayName;
  }
}
