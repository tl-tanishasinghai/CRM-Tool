package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.customer_portal.model.dto.LeadIdResponse;
import org.junit.jupiter.api.Test;

class LeadIdResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testDeserialization() throws JsonProcessingException {
    // Sample JSON data to be deserialized
    String json = "{ \"id\": 12345 }";

    // Deserialize the JSON string into LeadIdResponse object
    LeadIdResponse response = objectMapper.readValue(json, LeadIdResponse.class);

    // Assertions to verify that the fields were correctly deserialized
    assertNotNull(response);
    assertEquals(Long.valueOf(12345), response.getEntityId());
  }

  @Test
  void testGetterSetter() {
    // Create a LeadIdResponse object
    LeadIdResponse response = new LeadIdResponse();

    // Set values using setters
    response.setEntityId(12345L);

    // Assertions to verify getters are returning correct values
    assertEquals(Long.valueOf(12345), response.getEntityId());
  }

  @Test
  void testJsonIgnoreProperties() throws JsonProcessingException {
    // Sample JSON with an extra field that should be ignored
    String jsonWithExtraField = "{ \"id\": 12345, \"extraField\": \"ignoreMe\" }";

    // Deserialize the JSON string into LeadIdResponse object
    LeadIdResponse response = objectMapper.readValue(jsonWithExtraField, LeadIdResponse.class);

    // Assertions to verify the extra field is ignored and deserialization works
    assertNotNull(response);
    assertEquals(Long.valueOf(12345), response.getEntityId());
  }
}
