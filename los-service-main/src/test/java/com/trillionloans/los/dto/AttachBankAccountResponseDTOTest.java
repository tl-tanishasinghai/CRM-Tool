package com.trillionloans.los.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.trillionloans.los.model.response.AttachBankAccountResponseDTO;
import org.junit.jupiter.api.Test;

class AttachBankAccountResponseDTOTest {

  @Test
  void testConstructorAndGetters() {
    // Arrange
    String expectedBankId = "12345";

    // Act
    AttachBankAccountResponseDTO responseDTO = new AttachBankAccountResponseDTO(expectedBankId);

    // Assert
    assertNotNull(responseDTO);
    assertEquals(expectedBankId, responseDTO.bankId());
  }

  @Test
  void testDifferentBankIds() {
    // Arrange
    String bankId1 = "12345";
    String bankId2 = "67890";

    // Act
    AttachBankAccountResponseDTO responseDTO1 = new AttachBankAccountResponseDTO(bankId1);
    AttachBankAccountResponseDTO responseDTO2 = new AttachBankAccountResponseDTO(bankId2);

    // Assert
    assertNotEquals(responseDTO1.bankId(), responseDTO2.bankId());
  }

  @Test
  void testNullBankId() {
    // Arrange
    String nullBankId = null;

    // Act
    AttachBankAccountResponseDTO responseDTO = new AttachBankAccountResponseDTO(nullBankId);

    // Assert
    assertNotNull(responseDTO);
    assertEquals(nullBankId, responseDTO.bankId());
  }
}
