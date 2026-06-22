package com.trillionloans.los.mapper;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public enum PanStatus {
  E("E", "EXISTING AND VALID"),
  F("F", "Marked as Fake"),
  X("X", "Marked as Deactivated"),
  D("D", "Deleted"),
  N("N", "Record (PAN) Not Found in ITD Database / Invalid PAN"),
  EA("EA", "Existing and Valid but event marked as Amalgamation in ITD database"),
  EC("EC", "Existing and Valid but event marked as Acquisition in ITD database"),
  ED("ED", "Existing and Valid but event marked as Death in ITD database"),
  EI("EI", "Existing and Valid but event marked as Dissolution in ITD database"),
  EL("EL", "Existing and Valid but event marked as Liquidated in ITD database"),
  EM("EM", "Existing and Valid but event marked as Merger in ITD database"),
  EP("EP", "Existing and Valid but event marked as Partition in ITD database"),
  ES("ES", "Existing and Valid but event marked as Split in ITD database"),
  EU("EU", "Existing and Valid but event marked as Under Liquidation in ITD database"),
  UNKNOWN(
      "UNKNOWN",
      "Unrecognized PAN status code"); // Internal fallback for PAN status codes, not provided by
  // the vendor

  private final String code;
  private final String description;

  private static final Map<String, PanStatus> CODE_MAP = new HashMap<>();

  static {
    for (PanStatus status : values()) {
      CODE_MAP.put(status.code, status);
    }
  }

  PanStatus(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public static PanStatus fromCode(String code) {
    return CODE_MAP.getOrDefault(code, UNKNOWN);
  }
}
