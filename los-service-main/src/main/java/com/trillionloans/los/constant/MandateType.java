package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum MandateType {
  CREATE("CREATE");

  private final String displayName;

  MandateType(String displayName) {
    this.displayName = displayName;
  }
}
