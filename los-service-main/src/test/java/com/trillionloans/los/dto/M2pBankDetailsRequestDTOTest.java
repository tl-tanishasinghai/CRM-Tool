package com.trillionloans.los.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trillionloans.los.model.request.m2p.M2pBankDetailsRequestDTO;
import org.junit.jupiter.api.Test;

class M2pBankDetailsRequestDTOTest {

  @Test
  void testBuilderAndGetters() {
    M2pBankDetailsRequestDTO bankDetails =
        M2pBankDetailsRequestDTO.builder()
            .name("John Doe")
            .accountNumber("1234567890")
            .ifscCode("ABC123456")
            .accountTypeId("SAVINGS")
            .supportedForRepayment(true)
            .supportedForDisbursement(false)
            .build();

    assertEquals("John Doe", bankDetails.getName());
    assertEquals("1234567890", bankDetails.getAccountNumber());
    assertEquals("ABC123456", bankDetails.getIfscCode());
    assertEquals("SAVINGS", bankDetails.getAccountTypeId());
    assertTrue(bankDetails.isSupportedForRepayment());
    assertFalse(bankDetails.isSupportedForDisbursement());
  }

  @Test
  void testNoArgsConstructor() {
    M2pBankDetailsRequestDTO bankDetails = new M2pBankDetailsRequestDTO();

    assertNull(bankDetails.getName());
    assertNull(bankDetails.getAccountNumber());
    assertNull(bankDetails.getIfscCode());
    assertNull(bankDetails.getAccountTypeId());
    assertFalse(bankDetails.isSupportedForRepayment());
    assertFalse(bankDetails.isSupportedForDisbursement());
  }

  @Test
  void testAllArgsConstructor() {
    M2pBankDetailsRequestDTO bankDetails =
        new M2pBankDetailsRequestDTO("Jane Doe", "0987654321", "XYZ987654", "CURRENT", true, true);

    assertEquals("Jane Doe", bankDetails.getName());
    assertEquals("0987654321", bankDetails.getAccountNumber());
    assertEquals("XYZ987654", bankDetails.getIfscCode());
    assertEquals("CURRENT", bankDetails.getAccountTypeId());
    assertTrue(bankDetails.isSupportedForRepayment());
    assertTrue(bankDetails.isSupportedForDisbursement());
  }

  @Test
  void testSetters() {
    M2pBankDetailsRequestDTO bankDetails = new M2pBankDetailsRequestDTO();

    bankDetails.setName("Alice");
    bankDetails.setAccountNumber("1122334455");
    bankDetails.setIfscCode("LMN654321");
    bankDetails.setAccountTypeId("SAVINGS");
    bankDetails.setSupportedForRepayment(true);
    bankDetails.setSupportedForDisbursement(true);

    assertEquals("Alice", bankDetails.getName());
    assertEquals("1122334455", bankDetails.getAccountNumber());
    assertEquals("LMN654321", bankDetails.getIfscCode());
    assertEquals("SAVINGS", bankDetails.getAccountTypeId());
    assertTrue(bankDetails.isSupportedForRepayment());
    assertTrue(bankDetails.isSupportedForDisbursement());
  }
}
