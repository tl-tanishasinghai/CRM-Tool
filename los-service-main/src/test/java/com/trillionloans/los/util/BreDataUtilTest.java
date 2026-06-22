package com.trillionloans.los.util;

import static org.assertj.core.api.Assertions.*;

import com.trillionloans.los.model.dto.BreDatatableDTO;
import com.trillionloans.los.model.response.BankUnderwritingGstUnderwritingResponseDTO;
import java.util.*;
import org.junit.jupiter.api.Test;

class BreDataUtilTest {

  @Test
  void testGetScienapticDetailsDTO_validInput_shouldAddBureauData() {
    Map<String, Object> input = new HashMap<>();
    Map<String, Object> values = new HashMap<>();
    values.put("input", input);
    Map<String, Object> request = new HashMap<>();
    request.put("values", values);

    Map<String, Object> response = Map.of("score", 700);

    Object result = BreDataUtil.getScienapticDetailsDTO(request, response, "PROD001");

    Map<String, Object> updatedInput =
        (Map<String, Object>)
            ((Map<String, Object>) ((Map<String, Object>) result).get("values")).get("input");
    assertThat(updatedInput).containsKey("bureauTradeLines");
    assertThat(((Map<String, Object>) updatedInput.get("bureauTradeLines")))
        .containsKey("bureauTradeLineData");
  }

  @Test
  void testGetScienapticDetailsDTO_missingValues_shouldThrowException() {
    Map<String, Object> request = new HashMap<>();

    assertThatThrownBy(() -> BreDataUtil.getScienapticDetailsDTO(request, new Object(), ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("values");
  }

  @Test
  void testGetScienapticDetailsDTO_missingInput_shouldThrowException() {
    Map<String, Object> request = new HashMap<>();
    request.put("values", new HashMap<>());

    assertThatThrownBy(() -> BreDataUtil.getScienapticDetailsDTO(request, new Object(), ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("input");
  }

  @Test
  void testGetBreDatatableDTO_shouldReturnCorrectDTO() {
    Object req = Map.of("field", "value");
    Object res = Map.of("result", "ok");
    String loanId = "LN123";

    BreDatatableDTO dto = BreDataUtil.getBreDatatableDTO(req, res, loanId);

    assertThat(dto.getLead()).isEqualTo(loanId);
    assertThat(dto.getRequest()).contains("field");
    assertThat(dto.getResponse()).contains("result");
    assertThat(dto.getLocale()).isEqualTo("en");
  }

  @Test
  void testGetScienapticDetailsDTOWithBankDataAndGstData_validData() {
    Map<String, Object> input = new HashMap<>();
    Map<String, Object> values = new HashMap<>();
    values.put("input", input);
    Map<String, Object> request = new HashMap<>();
    request.put("values", values);

    String bankDataJson = "[{\"bankField\":\"bankValue\"}]";
    String gstDataJson = "[{\"gstField\":\"gstValue\"}]";

    BankUnderwritingGstUnderwritingResponseDTO dto =
        new BankUnderwritingGstUnderwritingResponseDTO();
    dto.setBankData(bankDataJson);
    dto.setGstData(gstDataJson);

    Object result = BreDataUtil.getScienapticDetailsDTOWithBankDataAndGstData(request, "PROD", dto);

    Map<String, Object> inputMap =
        (Map<String, Object>)
            ((Map<String, Object>) ((Map<String, Object>) result).get("values")).get("input");
    assertThat(inputMap).containsKeys("bankTransferData", "gstData");
  }

  @Test
  void testExtractBureauId_validKey_shouldReturnValue() {
    List<Map<String, Object>> data = List.of(Map.of("bureauId", "B123"));
    String bureauId = BreDataUtil.extractBureauId(data, "bureauId");

    assertThat(bureauId).isEqualTo("B123");
  }

  @Test
  void testExtractBureauId_keyNotFound_shouldThrowException() {
    List<Map<String, Object>> data = List.of(Map.of("otherKey", "value"));

    assertThatThrownBy(() -> BreDataUtil.extractBureauId(data, "bureauId"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void testExtractBureauId_nonPrimitiveValue_shouldThrowException() {
    List<Map<String, Object>> data = List.of(Map.of("bureauId", Map.of("nested", "value")));

    assertThatThrownBy(() -> BreDataUtil.extractBureauId(data, "bureauId"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a primitive");
  }
}
