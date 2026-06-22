package com.trillionloans.customer_portal.util;

import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.DOB_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.EMAIL_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.GENERIC_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.NAME_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.OTP_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.TOKEN_MASK;
import static com.trillionloans.customer_portal.constant.StringConstants.MaskConstants.UCIC_MASK_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trillionloans.customer_portal.configuration.MaskingConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaskingConverterTest {

  private MaskingConverter maskingConverter;

  @BeforeEach
  public void setUp() {
    maskingConverter = new MaskingConverter();
  }

  @Test
  public void testMobileNumberMasking() {
    String input =
        "{\"mobileNo\":\"9876543210\",\"mobileNumber\":\"9876543210\",\"alternateMobileNo\":\"9876543210\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"mobileNo\":\"" + GENERIC_MASK + "43210\""));
    assertTrue(result.contains("\"mobileNumber\":\"" + GENERIC_MASK + "43210\""));
    assertTrue(result.contains("\"alternateMobileNo\":\"" + GENERIC_MASK + "43210\""));
  }

  @Test
  public void testEmailMasking() {
    String input = "{\"email\":\"user@example.com\"}";
    String result = maskingConverter.transform(null, input);
    assertEquals("{\"email\":\"" + EMAIL_MASK + "\"}", result);
  }

  @Test
  public void testNameMasking() {
    String input =
        "{\"firstName\":\"John\",\"middleName\":\"A.\",\"lastName\":\"Doe\",\"clientName\":\"Full"
            + " Name\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"firstName\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"middleName\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"lastName\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"clientName\":\"" + NAME_MASK + "\""));
  }

  @Test
  public void testDOBMasking() {
    String input = "{\"dateOfBirth\":\"1990-01-01\"}";
    String result = maskingConverter.transform(null, input);
    assertEquals("{\"dateOfBirth\":\"" + DOB_MASK + "\"}", result);
  }

  @Test
  public void testUCICMasking() {
    String input = "{\"ucic\":\"123456789012\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"ucic\":\"" + UCIC_MASK_PREFIX + "9012\""));
  }

  @Test
  public void testTokenAndOTP() {
    String input = "{\"token\":\"abcdefg\",\"otp\":\"123456\"}";
    String result = maskingConverter.transform(null, input);
    assertEquals("{\"token\":\"" + TOKEN_MASK + "\",\"otp\":\"" + OTP_MASK + "\"}", result);
  }

  @Test
  public void testAmountMasking() {
    String input = "{\"amount\":5000.75}";
    String result = maskingConverter.transform(null, input);
    assertEquals("{\"amount\":" + GENERIC_MASK + "}", result);
  }

  @Test
  public void testAccountNumberMasking() {
    String input =
        "{\"accountNumber\":\"ABCDE123456\",\"bankAccountNumber\":\"ABCDE987654\",\"loanAccountNumber\":\"ABCDE111222\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"accountNumber\":\"" + GENERIC_MASK + "123456\""));
    assertTrue(result.contains("\"bankAccountNumber\":\"" + GENERIC_MASK + "987654\""));
    assertTrue(result.contains("\"loanAccountNumber\":\"" + GENERIC_MASK + "111222\""));
  }

  @Test
  public void testDocumentKeyAndReceiptNumberMasking() {
    String input = "{\"documentKey\":\"DOCXY112233\",\"receiptNumber\":\"RCPXY998877\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"documentKey\":\"" + GENERIC_MASK + "112233\""));
    assertTrue(result.contains("\"receiptNumber\":\"" + GENERIC_MASK + "998877\""));
  }

  @Test
  public void testExternalIdAndClientIdMasking() {
    String input = "{\"externalId\":\"ABCDE999999\",\"clientId\":\"1234567890\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"externalId\":\"" + GENERIC_MASK + "999999\""));
    assertTrue(result.contains("\"clientId\":\"" + GENERIC_MASK + "\""));
  }

  @Test
  public void testLoanApplicationIdMasking() {
    String input = "{\"loanApplicationId\":\"APP123456789\"}";
    String result = maskingConverter.transform(null, input);
    assertEquals("{\"loanApplicationId\":\"" + GENERIC_MASK + "\"}", result);
  }

  @Test
  public void testAddressMasking() {
    String input =
        "{\"landmark\":\"Near Park\",\"addressLineOne\":\"Street 1\",\"addressLineTwo\":\"Area"
            + " 2\",\"postalCode\":\"560001\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"landmark\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"addressLineOne\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"addressLineTwo\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"postalCode\":\"" + GENERIC_MASK + "\""));
  }

  @Test
  public void testTimelineFieldsMasking() {
    String input =
        "{\"submittedByUsername\":\"agent01\",\"submittedByFirstname\":\"Alice\",\"submittedByLastname\":\"Smith\","
            + "\"approvedByUsername\":\"admin01\",\"approvedByFirstname\":\"Bob\",\"approvedByLastname\":\"Brown\","
            + "\"disbursedByUsername\":\"bank01\",\"disbursedByFirstname\":\"Charlie\",\"disbursedByLastname\":\"Davis\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"submittedByUsername\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"approvedByFirstname\":\"" + NAME_MASK + "\""));
    assertTrue(result.contains("\"disbursedByLastname\":\"" + NAME_MASK + "\""));
  }

  @Test
  public void testClientAccountNoAndAccountNoMasking() {
    String input = "{\"clientAccountNo\":\"ACCXY998877\",\"accountNo\":\"ACCYZ123456\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"clientAccountNo\":\"" + GENERIC_MASK + "998877\""));
    assertTrue(result.contains("\"accountNo\":\"" + GENERIC_MASK + "123456\""));
  }

  @Test
  public void testHandlesEmptyInput() {
    String input = "";
    String result = maskingConverter.transform(null, input);
    assertEquals("", result);
  }

  @Test
  public void testHandlesUnrelatedFields() {
    String input = "{\"someField\":\"someValue\"}";
    String result = maskingConverter.transform(null, input);
    assertEquals(input, result);
  }

  @Test
  public void testClientPanDocumentKeyMasking() {
    String input = "{\"clientPandocumentkey\":\"ABCDE12345XYZ\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"clientPandocumentkey\":\"" + GENERIC_MASK + "12345XYZ\""));
  }

  @Test
  public void testExternalReferenceNumberMasking() {
    String input = "{\"externalReferenceNumber\":\"ABCDE98765ABC\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"externalReferenceNumber\":\"" + GENERIC_MASK + "98765ABC\""));
  }

  @Test
  public void testExternalRefernceNumberMasking() {
    String input = "{\"externalRefernceNumber\":\"ABCDE88888XYZ\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"externalRefernceNumber\":\"" + GENERIC_MASK + "88888XYZ\""));
  }

  @Test
  public void testIFSCCodeMasking() {
    String input = "{\"ifscCode\":\"HDFC0123456\",\"ifsc\":\"ICIC0001234\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"ifscCode\":\"" + GENERIC_MASK + "123456\""));
    assertTrue(result.contains("\"ifsc\":\"" + GENERIC_MASK + "001234\""));
  }

  @Test
  public void testBankAccountHolderNameMasking() {
    String input = "{\"bankAccountHolderName\":\"ABCDEJohn Doe\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"bankAccountHolderName\":\"" + GENERIC_MASK + "John Doe\""));
  }

  @Test
  public void testAccountHolderNameMasking() {
    String input = "{\"accountHolderName\":\"ABCDEJane Smith\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"accountHolderName\":\"" + GENERIC_MASK + "Jane Smith\""));
  }

  @Test
  public void testPanCardMasking() {
    String input = "{\"pancard\":\"ABCDE9876Z\"}";
    String result = maskingConverter.transform(null, input);
    assertTrue(result.contains("\"pancard\":\"" + GENERIC_MASK + "9876Z\""));
  }

  @Test
  public void testLeadInfoPathMasking() {
    String input = "/lead/info/1234567890";
    String result = maskingConverter.transform(null, input);
    assertEquals("/lead/info/" + GENERIC_MASK + "67890", result);
  }
}
