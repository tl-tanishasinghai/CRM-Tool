package com.trillionloans.customer_portal.constant;

import lombok.Getter;

@Getter
public enum OtpErrorMessage {
  OTP_EXPIRED_1("otp_expired"),
  OTP_EXPIRED_2("otp expired"),
  OTP_NOT_MATCH("otp not match"),
  MOBILE_ALREADY_VERIFIED("mobile no. already verified"),
  OTP_ALREADY_VERIFIED("otp already verified");

  private final String message;

  OtpErrorMessage(String message) {
    this.message = message;
  }

  /** Checks if the input string contains any of the defined error messages. Case-insensitive. */
  public static boolean containsMessage(String input) {
    if (input == null) return false;
    String lowerInput = input.toLowerCase();
    for (OtpErrorMessage errMsg : values()) {
      if (lowerInput.contains(errMsg.getMessage())) {
        return true;
      }
    }
    return false;
  }
}
