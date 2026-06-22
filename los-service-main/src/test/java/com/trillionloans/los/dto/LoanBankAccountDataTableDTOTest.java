package com.trillionloans.los.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trillionloans.los.model.request.LoanBankAccountDataTableDTO;
import org.junit.jupiter.api.Test;

class LoanBankAccountDataTableDTOTest {

  @Test
  void testAllArgsConstructorAndGetters() {
    // Given
    LoanBankAccountDataTableDTO dto =
        new LoanBankAccountDataTableDTO(
            "1234567890",
            "bank123",
            "IFSC001",
            "John Doe",
            "Bank of Examples",
            "Savings",
            "Self",
            "Yes");

    // Then
    assertEquals("1234567890", dto.getBankAccountNumber());
    assertEquals("bank123", dto.getBankId());
    assertEquals("IFSC001", dto.getIfscCode());
    assertEquals("John Doe", dto.getAccountHolderName());
    assertEquals("Bank of Examples", dto.getBankName());
    assertEquals("Savings", dto.getAccountType());
    assertEquals("Self", dto.getBeneficiaryType());
    assertEquals("Yes", dto.getBankVerified());
  }

  @Test
  void testNoArgsConstructorAndSetters() {
    // Given
    LoanBankAccountDataTableDTO dto = new LoanBankAccountDataTableDTO();

    // When
    dto.setBankAccountNumber("1234567890");
    dto.setBankId("bank123");
    dto.setIfscCode("IFSC001");
    dto.setAccountHolderName("John Doe");
    dto.setBankName("Bank of Examples");
    dto.setAccountType("Savings");
    dto.setBeneficiaryType("Self");
    dto.setBankVerified("Yes");

    // Then
    assertEquals("1234567890", dto.getBankAccountNumber());
    assertEquals("bank123", dto.getBankId());
    assertEquals("IFSC001", dto.getIfscCode());
    assertEquals("John Doe", dto.getAccountHolderName());
    assertEquals("Bank of Examples", dto.getBankName());
    assertEquals("Savings", dto.getAccountType());
    assertEquals("Self", dto.getBeneficiaryType());
    assertEquals("Yes", dto.getBankVerified());
  }

  @Test
  void testBuilder() {
    // Given
    LoanBankAccountDataTableDTO dto =
        LoanBankAccountDataTableDTO.builder()
            .bankAccountNumber("1234567890")
            .bankId("bank123")
            .ifscCode("IFSC001")
            .accountHolderName("John Doe")
            .bankName("Bank of Examples")
            .accountType("Savings")
            .beneficiaryType("Self")
            .bankVerified("Yes")
            .build();

    // Then
    assertEquals("1234567890", dto.getBankAccountNumber());
    assertEquals("bank123", dto.getBankId());
    assertEquals("IFSC001", dto.getIfscCode());
    assertEquals("John Doe", dto.getAccountHolderName());
    assertEquals("Bank of Examples", dto.getBankName());
    assertEquals("Savings", dto.getAccountType());
    assertEquals("Self", dto.getBeneficiaryType());
    assertEquals("Yes", dto.getBankVerified());
  }
}
