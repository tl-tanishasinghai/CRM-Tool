package com.trillionloans.los.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trillionloans.los.constant.BankAccountType;
import com.trillionloans.los.constant.BeneficaryType;
import com.trillionloans.los.model.AttachBankDetailsDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AttachBankDetailsDTOTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Test valid DTO should have no violations")
  void testValidDTO() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName("John Doe")
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty(), "DTO should be valid and have no violations");
  }

  @Test
  @DisplayName("Test DTO for NULL constraint violations")
  void testInvalidDTO_NullValues() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber(null)
            .ifscCode(null)
            .accountHolderName(null)
            .bankName(null)
            .bankAccountType(null)
            .beneficiaryType(null)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(6, violations.size(), "Should have violations for all required fields");
  }

  @Test
  @DisplayName("IFSC Code cannot be NULL")
  void testInvalidDTO_NullIfscCode() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode(null)
            .accountHolderName("John Doe")
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for ifscCode");
    assertEquals(
        "[BankDetails] ifscCode cannot be NULL or empty!",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Account holder name cannot be NULL")
  void testInvalidDTO_NullAccountHolderName() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName(null)
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for accountHolderName");
    assertEquals(
        "[BankDetails] accountHolderName cannot be NULL or empty!",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Bank name cannot be NULL")
  void testInvalidDTO_NullBankName() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName("John Doe")
            .bankName(null)
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for bankName");
    assertEquals(
        "[BankDetails] bankName cannot be NULL or empty!",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Account type cannot be NULL")
  void testInvalidDTO_NullAccountType() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName("John Doe")
            .bankName("Bank of Example")
            .bankAccountType(null)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for accountType");
    assertEquals(
        "[BankDetails] accountType cannot be NULL!", violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Beneficiary type cannot be NULL")
  void testInvalidDTO_NullBeneficiaryType() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName("John Doe")
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(null)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for beneficiaryType");
    assertEquals(
        "[BankDetails] beneficiaryType cannot be NULL!", violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Account number cannot be NULL")
  void testInvalidDTO_EmptyAccountNumber() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber(null)
            .ifscCode("ABC123456")
            .accountHolderName("John Doe")
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for accountNumber");
    assertEquals(
        "[BankDetails] accountNumber cannot be NULL or empty!",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("IFSC Code cannot be EMPTY")
  void testInvalidDTO_EmptyIfscCode() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("")
            .accountHolderName("John Doe")
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for ifscCode");
    assertEquals(
        "[BankDetails] ifscCode cannot be NULL or empty!",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Account holder name cannot be EMPTY")
  void testInvalidDTO_EmptyAccountHolderName() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName("")
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for accountHolderName");
    assertEquals(
        "[BankDetails] accountHolderName cannot be NULL or empty!",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Bank name cannot be EMPTY")
  void testInvalidDTO_EmptyBankName() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName("John Doe")
            .bankName("")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(BeneficaryType.MERCHANT)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for bankName");
    assertEquals(
        "[BankDetails] bankName cannot be NULL or empty!",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Beneficiary type cannot be EMPTY")
  void testInvalidDTO_EmptyBeneficiaryType() {
    AttachBankDetailsDTO dto =
        AttachBankDetailsDTO.builder()
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountHolderName("John Doe")
            .bankName("Bank of Example")
            .bankAccountType(BankAccountType.SAVINGS)
            .beneficiaryType(null)
            .build();

    Set<ConstraintViolation<AttachBankDetailsDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size(), "Should have one violation for beneficiaryType");
    assertEquals(
        "[BankDetails] beneficiaryType cannot be NULL!", violations.iterator().next().getMessage());
  }
}
