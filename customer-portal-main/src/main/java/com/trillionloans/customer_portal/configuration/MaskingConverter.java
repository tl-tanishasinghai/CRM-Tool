package com.trillionloans.customer_portal.configuration;

import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.DOB_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.EMAIL_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.GENERIC_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.NAME_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.OTP_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.TOKEN_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.UCIC_MASK_PREFIX;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/** CompositeConverter class extension for masking PII data in logs like name, bank details, etc. */
public class MaskingConverter extends CompositeConverter<ILoggingEvent> {

  public String transform(ILoggingEvent event, String in) {
    // Mask mobile numbers
    in =
        in.replaceAll(
            "\"mobileNo\":\"(\\d{5})(\\d+?)\"", "\"mobileNo\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"mobileNumber\":\"(\\d{5})(\\d+?)\"", "\"mobileNumber\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"alternateMobileNo\":\"(\\d{5})(\\d+?)\"",
            "\"alternateMobileNo\":\"" + GENERIC_MASK + "$2\"");

    // Mask document keys and identifiers
    in =
        in.replaceAll(
            "\"documentKey\":\"(\\w{5})([^\"]+)\"", "\"documentKey\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"accountNumber\":\"(\\w{5})([^\"]+)\"",
            "\"accountNumber\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"bankAccountNumber\":\"(\\w{5})([^\"]+)\"",
            "\"bankAccountNumber\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"loanAccountNumber\":\"(\\w{5})([^\"]+)\"",
            "\"loanAccountNumber\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"loanApplicationId\":\"[^\"]+\"", "\"loanApplicationId\":\"" + GENERIC_MASK + "\"");
    in = in.replaceAll("\"clientId\":\\s*\"?\\d+\"?", "\"clientId\":\"" + GENERIC_MASK + "\"");
    in =
        in.replaceAll(
            "\"externalId\":\"(\\w{5})([^\"]+)\"", "\"externalId\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"receiptNumber\":\"(\\w{5})([^\"]+)\"",
            "\"receiptNumber\":\"" + GENERIC_MASK + "$2\"");

    // Mask email
    in = in.replaceAll("\"email\":\"[^\"]+\"", "\"email\":\"" + EMAIL_MASK + "\"");

    // Mask names
    in = in.replaceAll("\"firstName\":\"[^\"]+\"", "\"firstName\":\"" + NAME_MASK + "\"");
    in = in.replaceAll("\"middleName\":\"[^\"]+\"", "\"middleName\":\"" + NAME_MASK + "\"");
    in = in.replaceAll("\"lastName\":\"[^\"]+\"", "\"lastName\":\"" + NAME_MASK + "\"");
    in = in.replaceAll("\"clientName\":\"[^\"]+\"", "\"clientName\":\"" + NAME_MASK + "\"");

    // Mask DOB
    in = in.replaceAll("\"dateOfBirth\":\"[^\"]+\"", "\"dateOfBirth\":\"" + DOB_MASK + "\"");

    // Mask address
    in = in.replaceAll("\"landmark\":\"[^\"]+\"", "\"landmark\":\"" + NAME_MASK + "\"");
    in = in.replaceAll("\"addressLineOne\":\"[^\"]+\"", "\"addressLineOne\":\"" + NAME_MASK + "\"");
    in = in.replaceAll("\"addressLineTwo\":\"[^\"]+\"", "\"addressLineTwo\":\"" + NAME_MASK + "\"");
    in = in.replaceAll("\"postalCode\":\"[^\"]+\"", "\"postalCode\":\"" + GENERIC_MASK + "\"");

    // Mask ucic (keep last 4 digits)
    in = in.replaceAll("\"ucic\":\"\\w{0,}?(\\w{4})\"", "\"ucic\":\"" + UCIC_MASK_PREFIX + "$1\"");

    // Mask usernames in Timeline
    String[] timelineFields = {
      "submittedByUsername", "submittedByFirstname", "submittedByLastname",
      "approvedByUsername", "approvedByFirstname", "approvedByLastname",
      "disbursedByUsername", "disbursedByFirstname", "disbursedByLastname"
    };
    for (String field : timelineFields) {
      in = in.replaceAll("\"" + field + "\":\"[^\"]+\"", "\"" + field + "\":\"" + NAME_MASK + "\"");
    }

    // Mask amounts
    in = in.replaceAll("\"amount\":\\d+(\\.\\d+)?", "\"amount\":" + GENERIC_MASK);

    // Mask accountNo and clientAccountNo
    in =
        in.replaceAll(
            "\"accountNo\":\"(\\w{5})([^\"]+)\"", "\"accountNo\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"clientAccountNo\":\"(\\w{5})([^\"]+)\"",
            "\"clientAccountNo\":\"" + GENERIC_MASK + "$2\"");

    // Mask OTP and token
    in = in.replaceAll("\"otp\":\"(\\d{1,10})\"", "\"otp\":\"" + OTP_MASK + "\"");
    in = in.replaceAll("\"token\":\"[^\"]+\"", "\"token\":\"" + TOKEN_MASK + "\"");

    // Mask PAN, account holder names, IFSC, and external references
    in =
        in.replaceAll(
            "\"clientPandocumentkey\":\"(\\w{5})([^\"]+)\"",
            "\"clientPandocumentkey\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"externalReferenceNumber\":\"(\\w{5})([^\"]+)\"",
            "\"externalReferenceNumber\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"externalRefernceNumber\":\"(\\w{5})([^\"]+)\"",
            "\"externalRefernceNumber\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"ifscCode\":\"(\\w{5})([^\"]+)\"", "\"ifscCode\":\"" + GENERIC_MASK + "$2\"");
    in = in.replaceAll("\"ifsc\":\"(\\w{5})([^\"]+)\"", "\"ifsc\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"bankAccountHolderName\":\"(\\w{5})([^\"]+)\"",
            "\"bankAccountHolderName\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll(
            "\"accountHolderName\":\"(\\w{5})([^\"]+)\"",
            "\"accountHolderName\":\"" + GENERIC_MASK + "$2\"");
    in =
        in.replaceAll("\"pancard\":\"(\\w{5})([^\"]+)\"", "\"pancard\":\"" + GENERIC_MASK + "$2\"");

    // Mask lead info path segment
    in = in.replaceAll("(/lead/info/)(\\d{5})(\\d+)", "$1" + GENERIC_MASK + "$3");

    // Mask any generic long numeric sequence (e.g., IDs) with 10+ digits
    in = in.replaceAll("(\\D)(\\d{5})(\\d{5,})", "$1" + GENERIC_MASK + "$3");

    in = in.replaceAll("\"name\":\"[^\"]+\"", "\"name\":\"" + NAME_MASK + "\"");

    in = in.replaceAll("\"ipAddress\":\"[^\"]+\"", "\"ipAddress\":\"XXXXX\"");

    return in;
  }
}
