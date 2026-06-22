package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum BatchStatus {
  COMPLETED("COMPLETED"),
  IN_PROGRESS("IN_PROGRESS"),
  FAILED("FAILED");

  private final String displayName;

  BatchStatus(String displayName) {
    this.displayName = displayName;
  }
}
