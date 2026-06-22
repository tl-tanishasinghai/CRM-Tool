package com.trillionloans.los.util;

import static com.trillionloans.los.util.DateTimeConverterUtil.convertEpochToFormattedIst;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trillionloans.los.model.dto.BreDatatableDTO;
import com.trillionloans.los.model.response.BankUnderwritingGstUnderwritingResponseDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class BreDataUtil {
  private static final Gson gson = new Gson();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @SuppressWarnings("unchecked")
  public static Object getScienapticDetailsDTO(
      Object request, Object response, String productCode) {
    Map<String, Object> requestMap = (Map<String, Object>) request;
    Map<String, Object> values = (Map<String, Object>) requestMap.get("values");

    if (values == null) {
      throw new IllegalArgumentException("Invalid request format: 'values' key not found");
    }

    Map<String, Object> input = (Map<String, Object>) values.get("input");

    if (input == null) {
      throw new IllegalArgumentException(
          "Invalid request format: 'input' key not found within 'values'");
    }
    Map<String, Object> bureauTradeLinesData = objectMapper.convertValue(response, Map.class);

    try {
      Map<String, Object> bureauTradeLines = new HashMap<>();
      bureauTradeLines.put("bureauTradeLineData", bureauTradeLinesData);
      input.put("bureauTradeLines", bureauTradeLines);
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Invalid request format: Type casting failed", e);
    }
    return request;
  }

  public static BreDatatableDTO getBreDatatableDTO(Object request, Object response, String loanId) {
    return BreDatatableDTO.builder()
        .lead(loanId)
        .request(gson.toJson(request))
        .response(gson.toJson(response))
        .scienapticstatus("")
        .locale("en")
        .dateformat(convertEpochToFormattedIst(System.currentTimeMillis()))
        .build();
  }

  @SuppressWarnings("unchecked")
  public static Object getScienapticDetailsDTOWithBankDataAndGstData(
      Object request,
      String productCode,
      BankUnderwritingGstUnderwritingResponseDTO bankDataAndGstData) {
    Map<String, Object> requestMap = (Map<String, Object>) request;
    Map<String, Object> values = (Map<String, Object>) requestMap.get("values");
    if (values == null) {
      throw new IllegalArgumentException("Invalid request format: 'values' key not found");
    }
    Map<String, Object> input = (Map<String, Object>) values.get("input");
    if (input == null) {
      throw new IllegalArgumentException(
          "Invalid request format: 'input' key not found within 'values'");
    }
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      List<Map<String, Object>> bankData =
          bankDataAndGstData.getBankData() != null
              ? objectMapper.readValue(
                  bankDataAndGstData.getBankData(),
                  new TypeReference<List<Map<String, Object>>>() {})
              : List.of();
      List<Map<String, Object>> gstData =
          bankDataAndGstData.getGstData() != null
              ? objectMapper.readValue(
                  bankDataAndGstData.getGstData(),
                  new TypeReference<List<Map<String, Object>>>() {})
              : List.of();
      input.put("bankTransferData", bankData);
      input.put("gstData", gstData);
    } catch (ClassCastException | JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid request format: Type casting failed", e);
    }
    return request;
  }

  public static String extractBureauId(Object json, String key) {
    String jsonString = gson.toJson(json);
    JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
    for (JsonElement element : jsonArray) {
      JsonObject jsonObject = element.getAsJsonObject();
      if (jsonObject.has(key)) {
        JsonElement valueElement = jsonObject.get(key);
        if (valueElement != null && valueElement.isJsonPrimitive()) {
          return valueElement.getAsString();
        } else {
          throw new IllegalArgumentException(
              "Value for key '" + key + "' is not a primitive type.");
        }
      }
    }
    throw new IllegalArgumentException("Key '" + key + "' not found in the JSON array.");
  }
}
