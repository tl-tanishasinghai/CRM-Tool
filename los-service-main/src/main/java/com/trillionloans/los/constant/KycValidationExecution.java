package com.trillionloans.los.constant;

public enum KycValidationExecution {
  PARALLEL("PARALLEL"),
  SEQUENCE("SEQUENCE");

  private final String displayName;

  KycValidationExecution(String displayName) {
    this.displayName = displayName;
  }
}
