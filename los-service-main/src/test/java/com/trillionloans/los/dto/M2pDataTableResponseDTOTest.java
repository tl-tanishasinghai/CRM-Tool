package com.trillionloans.los.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.model.response.m2p.M2pDataTableResponseDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

class M2pDataTableResponseDTOTest {

  @Test
  void testM2pDataTableResponseDTOBuilder() {
    M2pDataTableResponseDTO.ColumnValueDTO columnValue =
        M2pDataTableResponseDTO.ColumnValueDTO.builder()
            .columnName("Test Column")
            .value("Test Value")
            .build();

    M2pDataTableResponseDTO.RowDTO rowDTO =
        M2pDataTableResponseDTO.RowDTO.builder().row(List.of(columnValue)).build();

    M2pDataTableResponseDTO responseDTO =
        M2pDataTableResponseDTO.builder().columnData(List.of(rowDTO)).build();

    assertThat(responseDTO).isNotNull();
    assertThat(responseDTO.getColumnData()).hasSize(1);
    assertThat(responseDTO.getColumnData().get(0).getRow()).hasSize(1);
    assertThat(responseDTO.getColumnData().get(0).getRow().get(0).getColumnName())
        .isEqualTo("Test Column");
    assertThat(responseDTO.getColumnData().get(0).getRow().get(0).getValue())
        .isEqualTo("Test Value");
  }

  @Test
  void testRowDTOBuilder() {
    M2pDataTableResponseDTO.ColumnValueDTO columnValue1 =
        new M2pDataTableResponseDTO.ColumnValueDTO("Column1", "Value1");
    M2pDataTableResponseDTO.ColumnValueDTO columnValue2 =
        new M2pDataTableResponseDTO.ColumnValueDTO("Column2", "Value2");

    M2pDataTableResponseDTO.RowDTO rowDTO =
        M2pDataTableResponseDTO.RowDTO.builder().row(List.of(columnValue1, columnValue2)).build();

    assertThat(rowDTO).isNotNull();
    assertThat(rowDTO.getRow()).hasSize(2);
    assertThat(rowDTO.getRow().get(0).getColumnName()).isEqualTo("Column1");
    assertThat(rowDTO.getRow().get(1).getValue()).isEqualTo("Value2");
  }

  @Test
  void testColumnValueDTOBuilder() {
    M2pDataTableResponseDTO.ColumnValueDTO columnValueDTO =
        M2pDataTableResponseDTO.ColumnValueDTO.builder()
            .columnName("ColumnName")
            .value("ColumnValue")
            .build();

    assertThat(columnValueDTO).isNotNull();
    assertThat(columnValueDTO.getColumnName()).isEqualTo("ColumnName");
    assertThat(columnValueDTO.getValue()).isEqualTo("ColumnValue");
  }

  @Test
  void testJsonSerialization() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    M2pDataTableResponseDTO.ColumnValueDTO columnValue =
        M2pDataTableResponseDTO.ColumnValueDTO.builder()
            .columnName("Test Column")
            .value("Test Value")
            .build();

    M2pDataTableResponseDTO.RowDTO rowDTO =
        M2pDataTableResponseDTO.RowDTO.builder().row(List.of(columnValue)).build();

    M2pDataTableResponseDTO responseDTO =
        M2pDataTableResponseDTO.builder().columnData(List.of(rowDTO)).build();

    String json = objectMapper.writeValueAsString(responseDTO);
    assertThat(json).contains("Test Column", "Test Value");
  }

  @Test
  void testJsonDeserialization() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    String json =
        "{\"columnData\":[{\"row\":[{\"columnName\":\"Test Column\",\"value\":\"Test Value\"}]}]}";

    M2pDataTableResponseDTO responseDTO =
        objectMapper.readValue(json, M2pDataTableResponseDTO.class);

    assertThat(responseDTO).isNotNull();
    assertThat(responseDTO.getColumnData()).hasSize(1);
    assertThat(responseDTO.getColumnData().get(0).getRow()).hasSize(1);
    assertThat(responseDTO.getColumnData().get(0).getRow().get(0).getColumnName())
        .isEqualTo("Test Column");
    assertThat(responseDTO.getColumnData().get(0).getRow().get(0).getValue())
        .isEqualTo("Test Value");
  }
}
