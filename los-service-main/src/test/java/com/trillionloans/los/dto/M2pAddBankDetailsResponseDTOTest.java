package com.trillionloans.los.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.trillionloans.los.model.response.m2p.M2pAddBankDetailsResponseDTO;
import org.junit.jupiter.api.Test;

class M2pAddBankDetailsResponseDTOTest {

  @Test
  void testConstructorAndGetters() {
    String bankAccountDetailsId = "12345";
    M2pAddBankDetailsResponseDTO responseDTO =
        new M2pAddBankDetailsResponseDTO(bankAccountDetailsId, null, null);

    // Verify that the field is set correctly
    assertNotNull(responseDTO);
    assertEquals(bankAccountDetailsId, responseDTO.bankAccountDetailsId());
  }

  @Test
  void testToString() {
    String bankAccountDetailsId = "12345";
    M2pAddBankDetailsResponseDTO responseDTO =
        new M2pAddBankDetailsResponseDTO(bankAccountDetailsId, null, null);

    // Verify that toString() returns the expected format
    String expectedString =
        "M2pAddBankDetailsResponseDTO[bankAccountDetailsId="
            + bankAccountDetailsId
            + ", isBankVerified=null, errorMessage=null]";
    assertEquals(expectedString, responseDTO.toString());
  }

  @Test
  void testEqualsAndHashCode() {
    String bankAccountDetailsId1 = "12345";
    String bankAccountDetailsId2 = "67890";

    M2pAddBankDetailsResponseDTO responseDTO1 =
        new M2pAddBankDetailsResponseDTO(bankAccountDetailsId1, null, null);
    M2pAddBankDetailsResponseDTO responseDTO2 =
        new M2pAddBankDetailsResponseDTO(bankAccountDetailsId1, null, null);
    M2pAddBankDetailsResponseDTO responseDTO3 =
        new M2pAddBankDetailsResponseDTO(bankAccountDetailsId2, null, null);

    // Verify that equals() works as expected
    assertEquals(responseDTO1, responseDTO2); // Same ID should be equal
    assertNotEquals(responseDTO1, responseDTO3); // Different IDs should not be equal

    // Verify that hashCode() is consistent with equals()
    assertEquals(responseDTO1.hashCode(), responseDTO2.hashCode());
    assertNotEquals(responseDTO1.hashCode(), responseDTO3.hashCode());
  }
}
