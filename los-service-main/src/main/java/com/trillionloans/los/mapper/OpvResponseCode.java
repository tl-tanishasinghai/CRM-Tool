package com.trillionloans.los.mapper;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public enum OpvResponseCode {
  SUCCESS("1", "Success"),
  SYSTEM_ERROR("2", "System Error"),
  AUTHENTICATION_FAILURE("3", "Authentication Failure"),
  USER_NOT_AUTHORIZED("4", "User not authorized"),
  NO_PANS_OR_LIMIT_EXCEEDED("5", "No PANs Entered or Number of PANs exceeds the limit (5)"),
  USER_VALIDITY_EXPIRED("6", "User validity has expired"),
  NOT_ENOUGH_BALANCE("8", "Not enough balance"),
  NOT_HTTPS_REQUEST("9", "Not an HTTPS request"),
  POST_METHOD_NOT_USED("10", "POST method not used"),
  INVALID_VERSION("12", "Invalid version number entered"),
  INVALID_USER_ID(
      "15",
      "Valid User ID not sent in Input request and only PAN sent or User ID is greater than 12"
          + " characters or contains special characters"),
  CRL_EXPIRED("16", "Certificate Revocation List issued by the Certifying Authorities is expired"),
  USER_ID_DEACTIVATED("17", "User ID Deactivated"),
  CERTIFICATE_MISMATCH(
      "18", "The Certificate used for signing does not match the certificate in the database"),
  BLANK_SIGNATURE("19", "Signature sent in input request is blank"),
  MISSING_USER_ID_AND_PAN("20", "User ID and PAN not sent in Input request"),
  BLANK_INPUT("21", "No value sent in Input request"),
  INVALID_PAN_LENGTH("22", "PAN Number is more than 10 characters or value is Null"),
  SYSTEM_FAILURE("23", "System Failure or common error message for request"),
  DUPLICATE_TRANSACTION("24", "Duplicate Transaction ID entered"),
  JSON_PARSE_EXCEPTION("25", "Parse Exception in JSON"),
  RECORD_COUNT_MISMATCH(
      "26",
      "Records Count Passed from the header value does not match with the Records Count present in"
          + " the JSON Input Array"),
  INVALID_PAN_NAME(
      "27",
      "Name of PAN holder/Name on card is greater than 85 characters or value is null or contains ~"
          + " ^ special characters"),
  INVALID_FATHER_NAME(
      "28",
      "Father Name field is greater than 75 characters or value is null or contains ~ ^ special"
          + " characters"),
  INVALID_DOB_FORMAT(
      "29",
      "Date of Birth format is incorrect; it should be in DD/MM/YYYY format separated by slash"
          + " (/)"),
  INVALID_REQUEST_TIME("30", "Request Time is greater than 30 characters or value is Null"),
  INVALID_TRANSACTION_ID("31", "Transaction ID is greater than 50 characters or value is Null"),
  INVALID_RECORD_COUNT("32", "Record count is blank or contains alphabets or special characters"),
  INVALID_REQUEST_TIME_RANGE(
      "33",
      "Request Time cannot be a future date/time and cannot be older than the last half an hour"),

  UNKNOWN(
      "UNKNOWN",
      "Unknown response code"); // Internal fallback for PAN status codes, not provided by
  // the vendor

  private final String code;
  private final String description;

  private static final Map<String, OpvResponseCode> CODE_MAP = new HashMap<>();

  static {
    for (OpvResponseCode responseCode : values()) {
      CODE_MAP.put(responseCode.code, responseCode);
    }
  }

  OpvResponseCode(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public static OpvResponseCode fromCode(String code) {
    return CODE_MAP.getOrDefault(code, UNKNOWN);
  }
}
