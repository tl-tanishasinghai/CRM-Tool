package com.trillionloans.lms.util;

import static com.trillionloans.lms.constant.StringConstants.ERROR_PROCESSING_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trillionloans.lms.exception.BaseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtils {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Retains only the specified keys in the JSON string.
   *
   * @param keys the keys to retain
   * @param jsonString the JSON string
   * @return the JSON string with only the specified keys
   */
  public static String retainKeysFromJson(List<String> keys, String jsonString) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonString);
      List<String> keysToRemove = new ArrayList<>();

      // Collect keys to be removed
      jsonNode
          .fieldNames()
          .forEachRemaining(
              key -> {
                boolean keyExists = keys.stream().anyMatch(k -> k.startsWith(key));
                if (!keyExists) {
                  keysToRemove.add(key);
                } else if (jsonNode.get(key).isObject()) {
                  List<String> nestedKeys =
                      keys.stream()
                          .filter(k -> k.startsWith(key + "."))
                          .map(k -> k.substring((key + ".").length()))
                          .toList();
                  String nestedJson = retainKeysFromJson(nestedKeys, jsonNode.get(key).toString());
                  try {
                    ((ObjectNode) jsonNode).set(key, objectMapper.readTree(nestedJson));
                  } catch (JsonProcessingException e) {
                    throw new BaseException(
                        ERROR_PROCESSING_JSON, e, HttpStatus.INTERNAL_SERVER_ERROR);
                  }
                }
              });

      // Remove keys
      keysToRemove.forEach(((ObjectNode) jsonNode)::remove);
      return objectMapper.writeValueAsString(jsonNode);
    } catch (IOException e) {
      throw new BaseException(ERROR_PROCESSING_JSON, e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public static JsonNode parseJson(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (IOException e) {
      throw new BaseException(ERROR_PROCESSING_JSON, e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public static String convertToJsonString(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (IOException e) {
      throw new BaseException(ERROR_PROCESSING_JSON, e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public static String extractFieldValue(Object jsonObject, String key) {
    try {
      String jsonString = objectMapper.writeValueAsString(jsonObject);
      // Parse the JSON object
      JsonElement jsonElement = JsonParser.parseString(jsonString);
      if (jsonElement.isJsonObject()) {
        JsonObject jsonObj = jsonElement.getAsJsonObject();
        // Retrieve the value of the specified field
        if (jsonObj.has(key)) {
          return jsonObj.get(key).getAsString();
        } else {
          return null; // Specified key doesn't exist in the JSON
        }
      } else {
        return null; // Not a valid JSON object
      }
    } catch (Exception e) {
      // Handle any exceptions
      throw new BaseException(ERROR_PROCESSING_JSON, e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public static Object convertToJsonNode(Object input) {
    return Optional.ofNullable(input)
        .filter(
            obj ->
                !(obj
                    instanceof
                    LinkedMultiValueMap)) // Skip LinkedMultiValueMap (or handle differently)
        .map(
            obj -> {
              if (obj instanceof String str) {
                return tryParseJson(str); // Handle String input separately
              } else if (obj instanceof JsonNode) {
                return obj; // Return as-is for JsonNode
              } else {
                return tryConvertToJsonNode(obj); // Convert other types using objectMapper
              }
            })
        .orElse(null); // Return null if input is null or LinkedMultiValueMap
  }

  // Helper method to parse JSON string
  private static Object tryParseJson(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      return jsonString; // Return original string if parsing fails
    }
  }

  // Helper method to convert other types to JsonNode
  private static Object tryConvertToJsonNode(Object obj) {
    try {
      return objectMapper.convertValue(obj, JsonNode.class);
    } catch (IllegalArgumentException e) {
      return null; // Handle conversion failures gracefully
    }
  }
}
