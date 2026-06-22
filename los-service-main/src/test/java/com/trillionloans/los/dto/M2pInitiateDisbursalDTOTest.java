package com.trillionloans.los.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trillionloans.los.model.request.m2p.M2pInitiateDisbursalDTO;
import org.junit.jupiter.api.Test;

class M2pInitiateDisbursalDTOTest {

  @Test
  void testNoArgsConstructor() {
    M2pInitiateDisbursalDTO dto = new M2pInitiateDisbursalDTO();
    assertEquals(0, dto.getPaymentTypeId());
    assertEquals(null, dto.getBankAccountDetailId());
    assertEquals(null, dto.getExpectedDisbursementDate());
    assertEquals(null, dto.getDateFormat());
  }

  @Test
  void testAllArgsConstructor() {
    M2pInitiateDisbursalDTO dto =
        new M2pInitiateDisbursalDTO(1, "bank123", "2023-10-22", "yyyy-MM-dd");
    assertEquals(1, dto.getPaymentTypeId());
    assertEquals("bank123", dto.getBankAccountDetailId());
    assertEquals("2023-10-22", dto.getExpectedDisbursementDate());
    assertEquals("yyyy-MM-dd", dto.getDateFormat());
  }

  @Test
  void testBuilder() {
    M2pInitiateDisbursalDTO dto =
        M2pInitiateDisbursalDTO.builder()
            .paymentTypeId(2)
            .bankAccountDetailId("bank456")
            .expectedDisbursementDate("2023-10-23")
            .dateFormat("yyyy-MM-dd")
            .build();

    assertEquals(2, dto.getPaymentTypeId());
    assertEquals("bank456", dto.getBankAccountDetailId());
    assertEquals("2023-10-23", dto.getExpectedDisbursementDate());
    assertEquals("yyyy-MM-dd", dto.getDateFormat());
  }

  @Test
  void testSetters() {
    M2pInitiateDisbursalDTO dto = new M2pInitiateDisbursalDTO();
    dto.setPaymentTypeId(3);
    dto.setBankAccountDetailId("bank789");
    dto.setExpectedDisbursementDate("2023-10-24");
    dto.setDateFormat("yyyy-MM-dd");

    assertEquals(3, dto.getPaymentTypeId());
    assertEquals("bank789", dto.getBankAccountDetailId());
    assertEquals("2023-10-24", dto.getExpectedDisbursementDate());
    assertEquals("yyyy-MM-dd", dto.getDateFormat());
  }
}
