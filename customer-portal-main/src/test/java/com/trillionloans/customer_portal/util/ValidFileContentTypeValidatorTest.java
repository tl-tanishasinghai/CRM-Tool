package com.trillionloans.customer_portal.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ValidFileContentTypeValidatorTest {

  public static final String MOCK_VALID_PDF_BASE64 =
      "JVBERi0xLjUKJcTl8uXrp/Og0MTGCjEgMCBvYmoKPDwvQ3JlYXRpb25EYXRlIChEOjIwMjUw"
          + "NDA5MTIzNDU2KzAwJzAwJykKL1RpdGxlIChUZXN0IFBERikKL01vZERhdGUgKEQ6MjAyNTA0"
          + "MDkxMjM0NTYrMDAnMDAnKQovU3ViamVjdCAoKQovQXV0aG9yIChBdXRob3IpPj4KZW5kb2Jq"
          + "CnhyZWYKMCBvYmoKMSAwIG9iago8PC9UeXBlL1BhZ2UvUGFyZW50IDIgMCBSCi9NZWRpYUJv"
          + "eFswIDAgNjEyIDc5Ml0KL0NvbnRlbnRzIDMgMCBSCj4+CmVuZG9iagozIDAgb2JqCjw8L0xl"
          + "bmd0aCA0MSAvRmlsdGVyL0ZsYXRlRGVjb2RlPj5zdHJlYW0KSGVsbG8gd29ybGQhCmVuZHN0"
          + "cmVhbQplbmRvYmoKMiAwIG9iago8PC9UeXBlL1BhZ2VzL0NvdW50IDEKL0tpZHNbMSAwIFJd"
          + "Pj4KZW5kb2JqCnhyZWYKMSA1CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAxNSAwMDAw"
          + "MCBuIAowMDAwMDAwMDI3IDAwMDAwIG4gCjAwMDAwMDAxMDIgMDAwMDAgbgowMDAwMDAwMTc5"
          + "IDAwMDAwIG4gCjAwMDAwMDAyMjQgMDAwMDAgbgowMDAwMDAwMzU1IDAwMDAwIG4gCnRyYWls"
          + "ZXIKPDwvSW5mbyAxIDAgUiAvUm9vdCA0IDAgUj4+CnN0YXJ0eHJlZgozODMKJSVFT0YK";

  public static final String MOCK_VALID_JPEG_BASE64 =
      "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBxISEhUSEhIVFRUVFRUVFRUVFRUVFRUWFhUVFRUYHSggGBolHRUVITEhJSkrLi4uFx8zODMsNygtLisBCgoKDg0OGxAQGy0lICUtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLf/AABEIAKAAoAMBIgACEQEDEQH/xAAbAAACAwEBAQAAAAAAAAAAAAADBAACBQYBB//EADwQAAIBAgMFBgQEBQQDAQAAAAECEQADBBIhMQVBUWEicYGRobHwE0KxwRRCUmJy8RZTc6KywiQz/8QAGQEAAwEBAQAAAAAAAAAAAAAAAAECAwQF/8QAJxEAAgICAgICAgMBAAAAAAAAAAECEQMhEjEEQRMiUWEUcbH/2gAMAwEAAhEDEQA/APw8REQEREBERAREQEREBERAREQEREBERAREQEREBERAREQEREBERAREQEREBERAREQEREH/2Q==";

  public static final String MOCK_MALICIOUS_JS_BASE64 =
      Base64.getEncoder().encodeToString("<script>alert(1)</script>".getBytes());

  public static final String MOCK_FAKE_PNG_TXT_BASE64 =
      Base64.getEncoder().encodeToString("This is text content, not an image".getBytes());

  public static final String MOCK_EMPTY_CONTENT = "";

  private static Validator validator;

  @BeforeAll
  static void init() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void validPdfContentShouldPass() {
    SubmitFormRequest.Attachment attachment =
        SubmitFormRequest.Attachment.builder()
            .fileName("document.pdf")
            .fileContent(MOCK_VALID_PDF_BASE64)
            .build();

    Set<ConstraintViolation<SubmitFormRequest.Attachment>> violations =
        validator.validate(attachment);

    assertTrue(violations.isEmpty());
  }

  @Test
  void validJpegContentShouldPass() {
    SubmitFormRequest.Attachment attachment =
        SubmitFormRequest.Attachment.builder()
            .fileName("test.jpg")
            .fileContent(MOCK_VALID_JPEG_BASE64)
            .build();

    Set<ConstraintViolation<SubmitFormRequest.Attachment>> violations =
        validator.validate(attachment);

    assertTrue(violations.isEmpty());
  }

  @Test
  void maliciousJsContentShouldFail() {
    SubmitFormRequest.Attachment attachment =
        SubmitFormRequest.Attachment.builder()
            .fileName("testimages.js.JPG")
            .fileContent(MOCK_MALICIOUS_JS_BASE64)
            .build();

    Set<ConstraintViolation<SubmitFormRequest.Attachment>> violations =
        validator.validate(attachment);

    assertFalse(violations.isEmpty());
  }

  @Test
  void fakePngWithTextContentShouldFail() {
    SubmitFormRequest.Attachment attachment =
        SubmitFormRequest.Attachment.builder()
            .fileName("fake_image.png")
            .fileContent(MOCK_FAKE_PNG_TXT_BASE64)
            .build();

    Set<ConstraintViolation<SubmitFormRequest.Attachment>> violations =
        validator.validate(attachment);

    assertFalse(violations.isEmpty());
  }

  @Test
  void emptyFileContentShouldFail() {
    SubmitFormRequest.Attachment attachment =
        SubmitFormRequest.Attachment.builder()
            .fileName("empty.pdf")
            .fileContent(MOCK_EMPTY_CONTENT)
            .build();

    Set<ConstraintViolation<SubmitFormRequest.Attachment>> violations =
        validator.validate(attachment);

    assertFalse(violations.isEmpty());
  }

  @Test
  void nullFileContentShouldFail() {
    SubmitFormRequest.Attachment attachment =
        SubmitFormRequest.Attachment.builder().fileName("file.pdf").fileContent(null).build();

    Set<ConstraintViolation<SubmitFormRequest.Attachment>> violations =
        validator.validate(attachment);

    assertFalse(violations.isEmpty()); // expect failure
  }
}
