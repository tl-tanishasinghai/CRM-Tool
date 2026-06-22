package com.trillionloans.los.mapper;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public enum SeedingStatus {
  Y("Y", "Operative PAN"),
  R("R", "Inoperative PAN"),
  NA("NA", "Not Applicable (Non-Individual PANs)"),

  UNKNOWN("UNKNOWN", "Unrecognized seeding status code");
  // Internal fallback for PAN status codes, not provided by the vendor

  private final String code;
  private final String description;
  private static final Map<String, SeedingStatus> CODE_MAP = new HashMap<>();

  SeedingStatus(String code, String description) {
    this.code = code;
    this.description = description;
  }

  static {
    for (SeedingStatus status : values()) {
      CODE_MAP.put(status.code, status);
    }
  }

  public static SeedingStatus fromCode(String code) {
    return CODE_MAP.getOrDefault(code, UNKNOWN);
  }
}
