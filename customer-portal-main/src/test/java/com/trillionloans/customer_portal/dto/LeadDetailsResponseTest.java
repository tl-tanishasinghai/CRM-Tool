package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.customer_portal.model.dto.LeadDetailsResponse;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class LeadDetailsResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testDeserialization() throws JsonProcessingException {
    // Sample JSON data to be deserialized
    String json =
        "{ \"leadId\": 123456789, \"name\": \"John Doe\", \"dateOfBirth\": \"1990-01-01\","
            + " \"mobileNo\": \"1234567890\", \"email\": \"john.doe@example.com\", \"ucic\":"
            + " \"UCIC123\", \"landmark\": \"Near Park\", \"address\": \"123 Main St, Apt 4B,"
            + " Springfield\", \"panNumber\": \"MUTS9287T\", \"loanAccounts\": \"12345,67890\" }";

    // Deserialize the JSON string into LeadDetailsResponse object
    LeadDetailsResponse response = objectMapper.readValue(json, LeadDetailsResponse.class);

    // Assertions to verify that the fields were correctly deserialized
    assertNotNull(response);
    assertEquals(new BigInteger("123456789"), response.getLeadId());
    assertEquals("John Doe", response.getName());
    assertEquals("1990-01-01", response.getDateOfBirth());
    assertEquals("1234567890", response.getMobileNo());
    assertEquals("john.doe@example.com", response.getEmail());
    assertEquals("UCIC123", response.getUcic());
    assertEquals("Near Park", response.getLandmark());
    assertEquals("123 Main St, Apt 4B, Springfield", response.getAddress());
    assertEquals("MUTS9287T", response.getPanNumber());
    assertEquals("12345,67890", response.getLoanAccounts());
  }

  @Test
  void testGetterSetter() {
    // Create a LeadDetailsResponse object
    LeadDetailsResponse response = new LeadDetailsResponse();

    // Set values using setters
    response.setLeadId(new BigInteger("123456789"));
    response.setName("John Doe");
    response.setDateOfBirth("1990-01-01");
    response.setMobileNo("1234567890");
    response.setEmail("john.doe@example.com");
    response.setUcic("UCIC123");
    response.setLandmark("Near Park");
    response.setAddress("123 Main St, Apt 4B, Springfield");
    response.setPanNumber("MUTS9287T");
    response.setLoanAccounts("12345,67890");

    // Assertions to verify getters are returning correct values
    assertEquals(new BigInteger("123456789"), response.getLeadId());
    assertEquals("John Doe", response.getName());
    assertEquals("1990-01-01", response.getDateOfBirth());
    assertEquals("1234567890", response.getMobileNo());
    assertEquals("john.doe@example.com", response.getEmail());
    assertEquals("UCIC123", response.getUcic());
    assertEquals("Near Park", response.getLandmark());
    assertEquals("123 Main St, Apt 4B, Springfield", response.getAddress());
    assertEquals("MUTS9287T", response.getPanNumber());
    assertEquals("12345,67890", response.getLoanAccounts());
  }

  @Test
  void testJsonIgnoreProperties() throws JsonProcessingException {
    // Sample JSON with an extra field that should be ignored
    String jsonWithExtraField =
        "{ \"leadId\": 123456789, \"name\": \"John Doe\", \"dateOfBirth\": \"1990-01-01\","
            + " \"mobileNo\": \"1234567890\", \"email\": \"john.doe@example.com\", \"ucic\":"
            + " \"UCIC123\", \"landmark\": \"Near Park\", \"address\": \"123 Main St, Apt 4B,"
            + " Springfield\", \"panNumber\": \"MUTS9287T\", \"loanAccounts\": \"12345,67890\","
            + " \"extraField\": \"ignoreMe\" }";

    // Deserialize the JSON string into LeadDetailsResponse object
    LeadDetailsResponse response =
        objectMapper.readValue(jsonWithExtraField, LeadDetailsResponse.class);

    // Assertions to verify that deserialization works and extra field is ignored
    assertNotNull(response);
    assertEquals(new BigInteger("123456789"), response.getLeadId());
    assertEquals("John Doe", response.getName());
    assertEquals("1990-01-01", response.getDateOfBirth());
    assertEquals("1234567890", response.getMobileNo());
    assertEquals("john.doe@example.com", response.getEmail());
    assertEquals("UCIC123", response.getUcic());
    assertEquals("Near Park", response.getLandmark());
    assertEquals("123 Main St, Apt 4B, Springfield", response.getAddress());
    assertEquals("MUTS9287T", response.getPanNumber());
    assertEquals("12345,67890", response.getLoanAccounts());
  }
}
