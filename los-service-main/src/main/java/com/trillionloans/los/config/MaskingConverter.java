package com.trillionloans.los.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/** CompositeConverter class extension for masking PII data in logs like name, bank details, etc. */
public class MaskingConverter extends CompositeConverter<ILoggingEvent> {
  public String transform(ILoggingEvent event, String in) {
    in = in.replaceAll("\"mobileNo\":\"(\\d{5})(\\d+?)\"", "\"mobileNo\":\"XXXXX$2\"");
    in = in.replaceAll("\"mobile\":\"(\\d{5})(\\d+?)\"", "\"mobile\":\"XXXXX$2\"");
    in = in.replaceAll("\"phone\":\"(\\d{5})(\\d+?)\"", "\"phone\":\"XXXXX$2\"");

    in = in.replaceAll("\"phoneNumber\":\"(\\d{5})(\\d{5})\"", "\"phoneNumber\":\"XXXXX$2\"");
    in = in.replaceAll("\"email\":\"(\\w{2})[^\"]*(@[^\"]+)\"", "\"email\":\"$1****$2\"");

    in = in.replaceAll("(mobile=)(\\d{5})(\\d+)", "$1XXXXX$3");
    in =
        in.replaceAll(
            "\"alternateMobileNo\":\"(\\d{5})(\\d+?)\"", "\"alternateMobileNo\":\"XXXXX$2\"");
    in = in.replaceAll("\"documentKey\":\"(\\w{5})([^\"]+)\"", "\"documentKey\":\"XXXXX$2\"");
    in = in.replaceAll("\"fdocumentKey\":\"(\\w{5})([^\"]+)\"", "\"fdocumentKey\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"clientPandocumentkey\":\"(\\w{5})([^\"]+)\"",
            "\"clientPandocumentkey\":\"XXXXX$2\"");
    in = in.replaceAll("\"accountNumber\":\"(\\w{5})([^\"]+)\"", "\"accountNumber\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"bankAccountNumber\":\"(\\w{5})([^\"]+)\"", "\"bankAccountNumber\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"externalReferenceNumber\":\"(\\w{5})([^\"]+)\"",
            "\"externalReferenceNumber\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"externalRefernceNumber\":\"(\\w{5})([^\"]+)\"",
            "\"externalRefernceNumber\":\"XXXXX$2\"");
    in = in.replaceAll("\"ifscCode\":\"(\\w{5})([^\"]+)\"", "\"ifscCode\":\"XXXXX$2\"");
    in = in.replaceAll("\"ifsc\":\"(\\w{5})([^\"]+)\"", "\"ifsc\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"bankAccountHolderName\":\"(\\w{5})([^\"]+)\"",
            "\"bankAccountHolderName\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"accountHolderName\":\"(\\w{5})([^\"]+)\"", "\"accountHolderName\":\"XXXXX$2\"");
    in = in.replaceAll("\"pancard\":\"(\\w{5})([^\"]+)\"", "\"pancard\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "(\"documentType\":\\s*\"PAN\",\\s*\"id\":\")(\\w{5})(\\w+)(\")", "$1XXXXX$3$4");
    in = in.replaceAll("\"pan\":\"(\\w{5})([^\"]+)\"", "\"pan\":\"XXXXX$2\"");

    in =
        in.replaceAll(
            "\"pan_number\"\\s*:\\s*\"([A-Z]{5})([0-9]{4}[A-Z])\"", "\"pan_number\":\"XXXXX$2\"");

    in =
        in.replaceAll(
            "\"aadhaarxmlnamematch\":\"(\\w{2})([^\"]+)\"", "\"aadhaarxmlnamematch\":\"$1****\"");

    in = in.replaceAll("\"bestMatchName\":\"(\\w{2})([^\"]+)\"", "\"bestMatchName\":\"$1****\"");
    in =
        in.replaceAll(
            "\"corporate_config_id\":\"(\\w{5})([^\"]+)\"", "\"corporate_config_id\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"customer_identifier\":\"(\\d{5})(\\d+)\"", "\"customer_identifier\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"customer_account_number\":\"(\\w{2})([^\"]+)\"",
            "\"customer_account_number\":\"XX$2\"");
    in = in.replaceAll("\"customer_name\":\"([^\"]*?)(\\w{2})\"", "\"customer_name\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"destination_bank_id\":\"(\\w{4})([^\"]+)\"", "\"destination_bank_id\":\"XXXX$2\"");
    in =
        in.replaceAll(
            "\"destination_bank_name\":\"([^\"]*?)(\\w{2})\"",
            "\"destination_bank_name\":\"XXXXX$2\"");
    in = in.replaceAll("\"customer_mobile\":\"(\\d{5})(\\d+)\"", "\"customer_mobile\":\"XXXXX$2\"");

    in = in.replaceAll("\"name\":\"[^\"]+\"", "\"name\":\"XXXXX\"");
    in = in.replaceAll("\"customerName\":\"[^\"]+\"", "\"customerName\":\"XXXXX\"");
    in =
        in.replaceAll(
            "\"customerMobileNo\":\"(\\d{5})(\\d+?)\"", "\"customerMobileNo\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"protectionPlanId\":\"(\\w{5})([^\"]+)\"", "\"protectionPlanId\":\"XXXXX$2\"");

    in =
        in.replaceAll(
            "\"loanAccountNumber\":\"(\\w{5})([^\"]+)\"", "\"loanAccountNumber\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"panNumber\"\\s*:\\s*\"([A-Z]{5})([0-9]{4}[A-Z])\"", "\"panNumber\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"bank_account_number\":\"(\\w{5})([^\"]+)\"", "\"bank_account_number\":\"XXXXX$2\"");
    in = in.replaceAll("\"ifsc_code\":\"(\\w{5})([^\"]+)\"", "\"ifsc_code\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"account_holder_name\":\"(\\w{5})([^\"]+)\"", "\"account_holder_name\":\"XXXXX$2\"");
    in.replaceAll(
        "\"panNumber\"\\s*:\\s*\"([A-Z]{5})([0-9]{4}[A-Z])\"", "\"panNumber\":\"XXXXX$2\"");

    String dEq = "(?:=|\\\\u003d)";
    String urlPrefix = "(?i)https?://(?:[a-z0-9-]+-)?vkyc\\.trillionloans\\.com/d" + dEq;
    String tokenChar = "[^\\s\"'&?#]";
    in =
        in.replaceAll("(" + urlPrefix + ")(" + tokenChar + "{5})(" + tokenChar + "*)", "$1XXXXX$3");
    in.replaceAll(
        "\"panNumber\"\\s*:\\s*\"([A-Z]{5})([0-9]{4}[A-Z])\"", "\"panNumber\":\"XXXXX$2\"");
    in = in.replaceAll("\"bankIfscCode\":\"(\\w{5})([^\"]+)\"", "\"bankIfscCode\":\"XXXXX$2\"");
    in = in.replaceAll("\"sms\":\"(\\d{5})(\\d+?)\"", "\"sms\":\"XXXXX$2\"");
    in = in.replaceAll("(\"value\"\\s*:\\s*\")([^\"]*)(\")", "$1XXXXX$3");
    return in;
  }
}
