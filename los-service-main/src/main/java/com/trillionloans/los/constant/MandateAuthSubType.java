package com.trillionloans.los.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MandateAuthSubType {
  NET_BANKING("net_banking"),
  DEBIT("debit"),
  AADHAAR("aadhaar"),
  OTP("otp"),
  OTHER("other");

  private final String displayName;

  MandateAuthSubType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static MandateAuthSubType fromString(String value) {
    for (MandateAuthSubType type : MandateAuthSubType.values()) {
      if (type.getDisplayName().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown enum value: " + value);
  }
}
