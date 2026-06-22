package com.trillionloans.customer_portal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trillionloans.customer_portal.model.dto.LeadDetailsDTO;
import com.trillionloans.customer_portal.model.dto.LeadDetailsResponse;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeadDetailsUtilTest {

  @Test
  void testTransformLeadDetails() {

    LeadDetailsResponse leadDetailsResponse = new LeadDetailsResponse();
    leadDetailsResponse.setLeadId(BigInteger.valueOf(12345));
    leadDetailsResponse.setName("John Doe Smith");
    leadDetailsResponse.setEmail("john.doe@example.com");
    leadDetailsResponse.setMobileNo("1234567890");
    leadDetailsResponse.setUcic("UCIC1234");
    leadDetailsResponse.setDateOfBirth("2000-01-01");
    leadDetailsResponse.setAddress("123 Main St, Apt 101, Near Park, 12345");
    leadDetailsResponse.setPanNumber("MUTS9287T");
    leadDetailsResponse.setLoanAccounts("12345,67890");

    // Transform LeadDetailsResponse to LeadDetailsDTO
    LeadDetailsDTO leadDetailsDTO = LeadDetailsUtil.transformLeadDetails(leadDetailsResponse);

    // Assertions for transformed DTO
    assertEquals(BigInteger.valueOf(12345), leadDetailsDTO.getLeadId());
    assertEquals("john.doe@example.com", leadDetailsDTO.getEmail());
    assertEquals("1234567890", leadDetailsDTO.getMobileNo());
    assertEquals("UCIC1234", leadDetailsDTO.getUcic());
    assertEquals("John Doe Smith", leadDetailsDTO.getName());

    // Date of Birth should be formatted as dd/MM/yyyy
    assertEquals("01/01/2000", leadDetailsDTO.getDateOfBirth());

    int expectedAge =
        (int)
            java.time.temporal.ChronoUnit.YEARS.between(
                java.time.LocalDate.parse("2000-01-01"), java.time.LocalDate.now());
    assertEquals(expectedAge, leadDetailsDTO.getAge());

    // Address should map directly
    assertEquals("123 Main St, Apt 101, Near Park, 12345", leadDetailsDTO.getAddress());

    // Pan Number and Loan Accounts
    assertEquals("MUTS9287T", leadDetailsDTO.getPanNumber());
    assertEquals(List.of("12345", "67890"), leadDetailsDTO.getLoanAccounts());
  }

  @Test
  void testTransformLeadDetailsWithNullFields() {
    // Create a LeadDetailsResponse with null fields
    LeadDetailsResponse leadDetailsResponse = new LeadDetailsResponse();
    leadDetailsResponse.setLeadId(BigInteger.valueOf(12345));
    leadDetailsResponse.setName("John Smith");
    leadDetailsResponse.setEmail("john.doe@example.com");
    leadDetailsResponse.setMobileNo("1234567890");
    leadDetailsResponse.setUcic("UCIC1234");
    leadDetailsResponse.setDateOfBirth("2000-01-01");
    leadDetailsResponse.setAddress("123 Main St, Near Park, 12345");
    leadDetailsResponse.setPanNumber(null);
    leadDetailsResponse.setLoanAccounts(null);

    // Transform LeadDetailsResponse to LeadDetailsDTO
    LeadDetailsDTO leadDetailsDTO = LeadDetailsUtil.transformLeadDetails(leadDetailsResponse);

    // Assertions for transformed DTO with null fields
    assertEquals("John Smith", leadDetailsDTO.getName());
    assertEquals("123 Main St, Near Park, 12345", leadDetailsDTO.getAddress());

    // PanNumber should be null
    assertEquals(null, leadDetailsDTO.getPanNumber());

    // LoanAccounts should be an empty list or null depending on your transform logic
    assertEquals(List.of(), leadDetailsDTO.getLoanAccounts());
  }
}
