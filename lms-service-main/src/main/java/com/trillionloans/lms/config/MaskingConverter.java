package com.trillionloans.lms.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/** CompositeConverter class extension for masking PII data in logs like name, bank details, etc. */
public class MaskingConverter extends CompositeConverter<ILoggingEvent> {
  public String transform(ILoggingEvent event, String in) {
    in = in.replaceAll("\"mobileNo\":\"(\\d{5})(\\d+?)\"", "\"mobileNo\":\"XXXXX$2\"");
    in = in.replaceAll("\"pancard\":\"(\\w{5})([^\"]+)\"", "\"pancard\":\"XXXXX$2\"");
    in = in.replaceAll("\"sms\":\"(\\d{5})(\\d+?)\"", "\"sms\":\"XXXXX$2\"");
    in = in.replaceAll("mobile: (\\d{5})(\\d+?)\"", "mobile: XXXXX$2");
    in = in.replaceAll("mobile: (\\d{5})(\\d+)", "mobile: XXXXX$2");
    in = in.replaceAll("\"Mobile_Number\":\"(\\d{5})(\\d+?)\"", "\"Mobile_Number\":\"XXXXX$2\"");
    in = in.replaceAll("\"ipAddress\":\"[^\"]+\"", "\"ipAddress\":\"XXXXX\"");

    in = in.replaceAll("\"clientName\":\"[^\"]+\"", "\"clientName\":\"XXXXX\"");
    in =
        in.replaceAll(
            "\"bankAccountNumber\":\"(\\d+)(\\d{4})\"", "\"bankAccountNumber\":\"XXXXX$2\"");
    in = in.replaceAll("\"url\":\"[^\"]+\"", "\"url\":\"XXXXX\"");
    in = in.replaceAll("\"customerNumber\":\"(\\d+)(\\d{4})\"", "\"customerNumber\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"integratedNumber\":\"(\\d+)(\\d{4})\"", "\"integratedNumber\":\"XXXXX$2\"");
    in =
        in.replaceAll(
            "\"accountManagerEmailId\":\"([^@\"]+)@([^\"]+)\"",
            "\"accountManagerEmailId\":\"XXXXX@$2\"");
    in = in.replaceAll("\"emailId\":\"([^@\"]+)@([^\"]+)\"", "\"emailId\":\"XXXXX@$2\"");
    in =
        in.replaceAll(
            "\\\\\"link\\\\\":\\\\\"[^\\\\\"]+\\\\\"", "\\\\\"link\\\\\":\\\\\"XXXXX\\\\\"");

    return in;
  }
}
