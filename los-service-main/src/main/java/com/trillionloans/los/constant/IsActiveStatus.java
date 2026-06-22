package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum IsActiveStatus {
  I("I"),
  A("A");

  private final String displayName;

  IsActiveStatus(String displayName) {
    this.displayName = displayName;
  }
}
