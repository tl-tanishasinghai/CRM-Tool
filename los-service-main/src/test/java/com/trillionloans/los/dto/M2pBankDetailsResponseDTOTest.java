package com.trillionloans.los.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trillionloans.los.model.response.m2p.M2pBankDetailsResponseDTO;
import org.junit.jupiter.api.Test;

class M2pBankDetailsResponseDTOTest {

  @Test
  void testAllArgsConstructor() {
    M2pBankDetailsResponseDTO.AccountType accountType =
        new M2pBankDetailsResponseDTO.AccountType(1L, "SAV", "Savings", "SAV001");
    M2pBankDetailsResponseDTO.Status status =
        new M2pBankDetailsResponseDTO.Status(1L, "ACTIVE", "Active");

    M2pBankDetailsResponseDTO dto =
        new M2pBankDetailsResponseDTO(
            1L,
            "John Doe",
            "123456789",
            accountType,
            "IFSC001",
            "ABC Bank",
            status,
            "New York",
            "Main Branch",
            true,
            1001L,
            true);

    assertEquals(1L, dto.getBankId());
    assertEquals("John Doe", dto.getAccountHolderName());
    assertEquals("123456789", dto.getAccountNumber());
    assertEquals(accountType, dto.getAccountType());
    assertEquals("IFSC001", dto.getIfscCode());
    assertEquals("ABC Bank", dto.getBankName());
    assertEquals(status, dto.getStatus());
    assertEquals("New York", dto.getBankCity());
    assertEquals("Main Branch", dto.getBranchName());
    assertTrue(dto.isVerified());
    assertEquals(1001L, dto.getBankAccountAssociationId());
    assertTrue(dto.isPrimaryAccount());
  }

  @Test
  void testNoArgsConstructor() {
    M2pBankDetailsResponseDTO dto = new M2pBankDetailsResponseDTO();
    assertNull(dto.getBankId());
    assertNull(dto.getAccountHolderName());
    assertNull(dto.getAccountNumber());
    assertNull(dto.getAccountType());
    assertNull(dto.getIfscCode());
    assertNull(dto.getBankName());
    assertNull(dto.getStatus());
    assertNull(dto.getBankCity());
    assertNull(dto.getBranchName());
    assertFalse(dto.isVerified());
    assertEquals(0, dto.getBankAccountAssociationId());
    assertFalse(dto.isPrimaryAccount());
  }

  @Test
  void testBuilder() {
    M2pBankDetailsResponseDTO dto =
        M2pBankDetailsResponseDTO.builder()
            .bankId(2L)
            .accountHolderName("Jane Doe")
            .accountNumber("987654321")
            .ifscCode("IFSC002")
            .bankName("XYZ Bank")
            .bankCity("Los Angeles")
            .branchName("Downtown Branch")
            .isVerified(false)
            .bankAccountAssociationId(1002L)
            .isPrimaryAccount(false)
            .build();

    assertEquals(2L, dto.getBankId());
    assertEquals("Jane Doe", dto.getAccountHolderName());
    assertEquals("987654321", dto.getAccountNumber());
    assertEquals("IFSC002", dto.getIfscCode());
    assertEquals("XYZ Bank", dto.getBankName());
    assertEquals("Los Angeles", dto.getBankCity());
    assertEquals("Downtown Branch", dto.getBranchName());
    assertFalse(dto.isVerified());
    assertEquals(1002L, dto.getBankAccountAssociationId());
    assertFalse(dto.isPrimaryAccount());
  }

  @Test
  void testAccountType() {
    M2pBankDetailsResponseDTO.AccountType accountType =
        new M2pBankDetailsResponseDTO.AccountType(1L, "CUR", "Current", "CUR001");

    assertEquals(1L, accountType.getId());
    assertEquals("CUR", accountType.getCode());
    assertEquals("Current", accountType.getValue());
    assertEquals("CUR001", accountType.getSystemCode());
  }

  @Test
  void testStatus() {
    M2pBankDetailsResponseDTO.Status status =
        new M2pBankDetailsResponseDTO.Status(1L, "PENDING", "Pending");

    assertEquals(1L, status.getId());
    assertEquals("PENDING", status.getCode());
    assertEquals("Pending", status.getValue());
  }
}
