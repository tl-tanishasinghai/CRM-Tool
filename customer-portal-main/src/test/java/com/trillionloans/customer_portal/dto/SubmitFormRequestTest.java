package com.trillionloans.customer_portal.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitFormRequestTest {

  private Validator validator;

  @BeforeEach
  void setup() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testValidRequest_shouldPassValidation() {
    SubmitFormRequest.Attachment attachment =
        SubmitFormRequest.Attachment.builder()
            .fileName("document.pdf")
            .fileContent("aGVsbG8=") // base64 of "hello"
            .build();

    SubmitFormRequest request =
        SubmitFormRequest.builder()
            .registeredMobileNumber("+919876543210")
            .email("test@example.com")
            .concernCategory("EMI Related")
            .description("This is a valid description with more than 30 characters.")
            .loanId(String.valueOf(123))
            .panCard("ABCDE1234R")
            .attachments(List.of(attachment))
            .build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations).isEmpty();
  }

  @Test
  void testInvalidMobileNumber_shouldFailValidation() {
    SubmitFormRequest request =
        SubmitFormRequest.builder().registeredMobileNumber("1234567890").build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("registeredMobileNumber"));
  }

  @Test
  void testInvalidEmail_shouldFailValidation() {
    SubmitFormRequest request = SubmitFormRequest.builder().email("invalid-email").build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
  }

  @Test
  void testShortDescription_shouldFailValidation() {
    SubmitFormRequest request = SubmitFormRequest.builder().description("Too short").build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
  }

  @Test
  void testInvalidPanCard_shouldFailValidation() {
    SubmitFormRequest request = SubmitFormRequest.builder().panCard("ABCD12345").build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("panCard"));
  }

  @Test
  void testAttachmentsExceedLimit_shouldFailValidation() {
    List<SubmitFormRequest.Attachment> attachments =
        List.of(
            new SubmitFormRequest.Attachment("a.pdf", "content"),
            new SubmitFormRequest.Attachment("b.pdf", "content"),
            new SubmitFormRequest.Attachment("c.pdf", "content"),
            new SubmitFormRequest.Attachment("d.pdf", "content"),
            new SubmitFormRequest.Attachment("e.pdf", "content"),
            new SubmitFormRequest.Attachment("f.pdf", "content"));

    SubmitFormRequest request = SubmitFormRequest.builder().attachments(attachments).build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("attachments"));
  }

  @Test
  void testAttachmentInvalidFileName_shouldFailValidation() {
    SubmitFormRequest.Attachment attachment = new SubmitFormRequest.Attachment("file.exe", "data");
    SubmitFormRequest request =
        SubmitFormRequest.builder().attachments(List.of(attachment)).build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("attachments[0].fileName"));
  }

  @Test
  void testAttachmentBlankContent_shouldFailValidation() {
    SubmitFormRequest.Attachment attachment = new SubmitFormRequest.Attachment("file.pdf", "");
    SubmitFormRequest request =
        SubmitFormRequest.builder().attachments(List.of(attachment)).build();

    Set<ConstraintViolation<SubmitFormRequest>> violations = validator.validate(request);
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("attachments[0].fileContent"));
  }
}
