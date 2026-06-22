package com.trillionloans.los.constant;

import lombok.Getter;

@Getter
public enum BreStage {
  INITIATED("INITIATED"),
  BUREAU_HARD_PULL("BUREAU_HARD_PULL"),
  SCIENAPTIC("SCIENAPTIC"),
  M2P_UPDATE("M2P_UPDATE"),
  COMPLETED("COMPLETED");

  private final String displayName;

  BreStage(String displayName) {
    this.displayName = displayName;
  }
}
