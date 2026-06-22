package com.trillionloans.crm.integration;

import java.math.BigInteger;

public final class LoanStatusMapper {

  private LoanStatusMapper() {}

  public static String mapStatus(BigInteger statusCode) {
    if (statusCode == null) {
      return "Unknown";
    }
    return switch (statusCode.intValue()) {
      case 300 -> "Active";
      case 600 -> "Closed";
      case 601 -> "Written Off";
      case 700 -> "Closed";
      default -> "Unknown";
    };
  }

  public static boolean isExcluded(BigInteger statusCode) {
    if (statusCode == null) {
      return true;
    }
    int code = statusCode.intValue();
    return code == 200;
  }
}
