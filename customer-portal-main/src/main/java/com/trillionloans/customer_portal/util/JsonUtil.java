package com.trillionloans.customer_portal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trillionloans.customer_portal.exception.JsonProcessingFailureException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JsonUtil {
  private JsonUtil() {}

  private static final ObjectMapper OBJECT_MAPPER;
  private static final String FAILED_TO_PROCESS_JSON = "Failed to process JSON ";

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER
        .getFactory()
        .setStreamReadConstraints(
            StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build());
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  public static String pojoToJson(Object o) {
    if (o == null) {
      return null;
    }
    return pojoToJson(o, false);
  }

  public static String pojoToJson(Object o, boolean prettyPrint) {
    try {
      return prettyPrint
          ? OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o)
          : OBJECT_MAPPER.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingFailureException(FAILED_TO_PROCESS_JSON, e);
    }
  }

  public static <T> T readValue(String json, Class<T> clz) {
    if (json == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(json, clz);
    } catch (JsonProcessingException e) {
      throw new JsonProcessingFailureException(FAILED_TO_PROCESS_JSON, e);
    }
  }
}
