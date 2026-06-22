package com.trillionloans.customer_portal.util;

import static com.trillionloans.customer_portal.constant.StringConstants.YYYY_MM_DD;

import com.trillionloans.customer_portal.exception.CustomisedException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class OtpUtils {
  private OtpUtils() {}

  public static void validateFields(String mobileNumber, String loggerHeader) {
    if (mobileNumber == null || mobileNumber.isBlank()) {
      throw new CustomisedException(
          "MobileNumber must not be null or blank!", HttpStatus.BAD_REQUEST, loggerHeader);
    }

    if (!mobileNumber.matches("^\\d{10}$")) {
      throw new CustomisedException(
          "Invalid mobile number. It must have 10 digits.", HttpStatus.BAD_REQUEST, loggerHeader);
    }
  }

  public static void validateFieldsMobileWithCountryCode(String mobileNumber, String loggerHeader) {
    if (mobileNumber == null || mobileNumber.isBlank()) {
      throw new CustomisedException(
          "MobileNumber must not be null or blank!", HttpStatus.BAD_REQUEST, loggerHeader);
    }

    if (!mobileNumber.matches("^91\\d{10}$")) {
      throw new CustomisedException(
          "Invalid mobile number. It must start with 91 and have 10 digits.",
          HttpStatus.BAD_REQUEST,
          loggerHeader);
    }
  }

  public static void validateFieldsDOB(
      String mobileNumber, String dateOfBirth, String loggerHeader) {
    validateFields(mobileNumber, loggerHeader);

    if (dateOfBirth == null || dateOfBirth.isBlank()) {
      throw new CustomisedException(
          "Date of birth must not be null or blank!", HttpStatus.BAD_REQUEST, loggerHeader);
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYY_MM_DD);
    try {
      LocalDate.parse(dateOfBirth, formatter);
    } catch (DateTimeParseException e) {
      throw new CustomisedException(
          "Invalid date of birth format (expected YYYY-MM-DD): " + dateOfBirth,
          HttpStatus.BAD_REQUEST,
          loggerHeader);
    }
  }

  public static void validateFields(String mobileNumber, String otp, String loggerHeader) {
    validateFields(mobileNumber, loggerHeader);
    if (otp == null || otp.isBlank()) {
      throw new CustomisedException(
          "Otp must not be null or blank!", HttpStatus.BAD_REQUEST, loggerHeader);
    }

    if (!otp.matches("^\\d{4}$")) {
      throw new CustomisedException(
          "Invalid otp. It must have exactly 4 digits.", HttpStatus.BAD_REQUEST, loggerHeader);
    }
  }

  public static void validateFieldsPAN(String panLast4Digits, String loggerHeader) {
    if (panLast4Digits == null || panLast4Digits.trim().isEmpty()) {
      throw new CustomisedException(
          "PAN last 4 digits must not be null or blank.", HttpStatus.BAD_REQUEST, loggerHeader);
    }
    if (panLast4Digits.length() != 4) {
      throw new CustomisedException(
          "PAN last 4 digits must be exactly 4 characters long.",
          HttpStatus.BAD_REQUEST,
          loggerHeader);
    }
    if (!panLast4Digits.matches("\\d{3}[A-Z]")) {
      throw new CustomisedException(
          "PAN last 4 digits must be 3 numbers followed by an uppercase alphabet. Redirecting to"
              + " login page.",
          HttpStatus.BAD_REQUEST,
          loggerHeader);
    }
  }
}
