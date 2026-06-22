package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.customer_portal.model.dto.DocumentDetailResponse;
import org.junit.jupiter.api.Test;

class DocumentDetailResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testDeserialization() throws JsonProcessingException {
    // Sample JSON data to be deserialized
    String json =
        "{ \"id\": 123, \"parentEntityId\": 456, \"tagValue\": \"exampleTag\", \"fileName\":"
            + " \"document.pdf\" }";

    // Deserialize the JSON string into DocumentDetailResponse object
    DocumentDetailResponse response = objectMapper.readValue(json, DocumentDetailResponse.class);

    // Assertions to verify that the fields were correctly deserialized
    assertNotNull(response);
    assertEquals(123L, response.getId());
    assertEquals(456L, response.getParentEntityId());
    assertEquals("exampleTag", response.getTagValue());
    assertEquals("document.pdf", response.getFileName());
  }

  @Test
  void testGetterSetter() {
    // Create a DocumentDetailResponse object
    DocumentDetailResponse response = new DocumentDetailResponse();

    // Set values using setters
    response.setId(123L);
    response.setParentEntityId(456L);
    response.setTagValue("exampleTag");
    response.setFileName("document.pdf");

    // Assertions to verify getters are returning correct values
    assertEquals(123L, response.getId());
    assertEquals(456L, response.getParentEntityId());
    assertEquals("exampleTag", response.getTagValue());
    assertEquals("document.pdf", response.getFileName());
  }

  @Test
  void testJsonIgnoreProperties() throws JsonProcessingException {
    // Sample JSON with an extra field that should be ignored
    String jsonWithExtraField =
        "{ \"id\": 123, \"parentEntityId\": 456, \"tagValue\": \"exampleTag\", \"fileName\":"
            + " \"document.pdf\", \"extraField\": \"ignoreMe\" }";

    // Deserialize the JSON string into DocumentDetailResponse object
    DocumentDetailResponse response =
        objectMapper.readValue(jsonWithExtraField, DocumentDetailResponse.class);

    // Assertions to verify the extra field is ignored and deserialization works
    assertNotNull(response);
    assertEquals(123L, response.getId());
    assertEquals(456L, response.getParentEntityId());
    assertEquals("exampleTag", response.getTagValue());
    assertEquals("document.pdf", response.getFileName());
  }
}
