package com.trillionloans.los.util;

import static com.trillionloans.los.constant.StringConstants.ERROR_PROCESSING_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trillionloans.los.exception.BaseException;
import io.r2dbc.postgresql.codec.Json;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;

@Slf4j
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

  public static String extractFieldValue(Object jsonObject, String key) {
    try {
      // Parse the JSON object
      String jsonString = objectMapper.writeValueAsString(jsonObject);
      JsonElement jsonElement = JsonParser.parseString(jsonString);
      if (jsonElement.isJsonObject()) {
        JsonObject jsonObj = jsonElement.getAsJsonObject();
        // Retrieve the value of the specified field
        if (jsonObj.has(key)) {
          return jsonObj.get(key).getAsString();
        }
      } else {
        return null; // Not a valid JSON object
      }
    } catch (Exception e) {
      // Handle any exceptions
      throw new BaseException("Error processing JSON", e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return null;
  }

  public static Map<String, Object> retainBreResponseFromJson(List<String> keys, String json) {
    Map<String, Object> resultMap = new HashMap<>();
    JsonNode rootNode = parseJson(json);

    for (String key : keys) {
      processKey(key, rootNode, resultMap);
    }

    return resultMap;
  }

  private static void processKey(String key, JsonNode rootNode, Map<String, Object> resultMap) {
    if (rootNode == null) {
      return;
    }

    String[] keyPath = key.split("\\.");
    JsonNode currentNode = traversePath(keyPath, rootNode);

    String lastKey = keyPath[keyPath.length - 1];
    if (currentNode != null && !currentNode.isNull()) {
      if (currentNode.isArray()) {
        resultMap.put(
            lastKey, objectMapper.convertValue(currentNode, new TypeReference<List<Object>>() {}));
      } else if (currentNode.isObject()) {
        resultMap.put(lastKey, objectMapper.convertValue(currentNode, Map.class));
      } else {
        resultMap.put(lastKey, currentNode.asText());
      }
    } else {
      resultMap.put(lastKey, lastKey.equals("loans") ? new ArrayList<>() : "");
    }
  }

  private static JsonNode traversePath(String[] keyPath, JsonNode rootNode) {
    JsonNode currentNode = rootNode;

    for (String pathPart : keyPath) {
      if (currentNode == null) {
        break;
      }
      if (currentNode.isObject()) {
        currentNode = currentNode.get(pathPart);
      } else if (currentNode.isArray()) {
        try {
          int index = Integer.parseInt(pathPart);
          currentNode = currentNode.get(index);
        } catch (NumberFormatException e) {
          currentNode = null;
        }
      } else {
        currentNode = null;
      }
    }

    return currentNode;
  }

  public static String extractFieldValueByPath(Object jsonObject, String keyPath) {
    try {
      JsonNode rootNode = objectMapper.valueToTree(jsonObject);
      return extractValueFromPath(rootNode, keyPath);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Key '" + keyPath + "' not found in the JSON array.");
    }
  }

  private static String extractValueFromPath(JsonNode rootNode, String keyPath) {
    String[] keys = keyPath.split("\\.");
    JsonNode currentNode = rootNode;
    for (String key : keys) {
      if (currentNode.has(key)) {
        currentNode = currentNode.get(key);
      } else {
        return null;
      }
    }

    return currentNode.isValueNode() ? currentNode.asText() : currentNode.toString();
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
              if (obj instanceof String) {
                return tryParseJson((String) obj); // Handle String input separately
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

  public static Json toJsonB(Object object) {
    try {
      if (object == null) return null;
      return Json.of(objectMapper.writeValueAsString(object));
    } catch (JsonProcessingException e) {
      log.error("Error converting object to JSONB", e);
      return null;
    }
  }

  public static <T> String serializeResponse(T object) {
    if (object == null) {
      log.warn("[JSON_UTIL][SERIALIZE] Attempted to serialize a null object.");
      return "{}";
    }
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error(
          "[JSON_UTIL][SERIALIZE_ERROR] Failed to serialize {}: {}",
          object.getClass().getSimpleName(),
          e.getMessage(),
          e);
      return "{}";
    }
  }

  /**
   * Converts a DB Json object to a List of Maps. Used for: addressDetails, familyDetails,
   * bankDetails, etc.
   */
  public static List<Map<String, Object>> parseJsonToList(Json jsonField) {
    if (jsonField == null || StringUtils.isBlank(jsonField.asString())) {
      return Collections.emptyList();
    }

    try {
      return objectMapper.readValue(
          jsonField.asString(), new TypeReference<List<Map<String, Object>>>() {});
    } catch (Exception e) {
      log.error(
          "[JSON_UTILS] Failed to parse JSON to List. Content: {}, Error: {}",
          jsonField.asString(),
          e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Converts a DB Json object to a Single Map. Used for: employmentDetails, additionalDetails (if
   * stored as object).
   */
  public static Map<String, Object> parseJsonToMap(Json jsonField) {
    if (jsonField == null || StringUtils.isBlank(jsonField.asString())) {
      return Collections.emptyMap();
    }

    try {
      return objectMapper.readValue(
          jsonField.asString(), new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.error(
          "[JSON_UTILS] Failed to parse JSON to Map. Content: {}, Error: {}",
          jsonField.asString(),
          e.getMessage());
      return Collections.emptyMap();
    }
  }
}
