package com.trillionloans.los.mapper;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KarzaInternalStatusEnum {
  CODE_0("0", "", ""),
  CODE_101("101", "Valid Authentication", "Successful OCR"),
  CODE_102("102", "Invalid ID number or combination of inputs", "No KYC Document identified"),
  CODE_103(
      "103",
      "No records found for the given ID or combination of inputs",
      "Image Format Not Supported OR Size Exceeds 6MB"),
  CODE_104("104", "Max retries exceeded", "N/A"),
  CODE_105("105", "Missing Consent", "N/A"),
  CODE_106("106", "Multiple Records Exist", "N/A"),
  CODE_107("107", "Not Supported", "N/A"),
  CODE_108("108", "Internal Resource Unavailable", "N/A"),
  CODE_109("109", "Too many records Found", "N/A"),
  UNKNOWN("000", "Unknown Code", "Unknown Code");

  private final String code;
  private final String authDescription;
  private final String ocrDescription;

  private static final Map<String, KarzaInternalStatusEnum> CODE_MAP = new HashMap<>();

  static {
    for (KarzaInternalStatusEnum status : values()) {
      CODE_MAP.put(status.code, status);
    }
  }

  public static KarzaInternalStatusEnum fromCode(String code) {
    return CODE_MAP.getOrDefault(code, UNKNOWN);
  }
}
