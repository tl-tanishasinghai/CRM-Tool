package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for the response of M2P data table. This class encapsulates the column
 * data returned in the response.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class M2pDataTableResponseDTO {

  /**
   * A list of row data represented by {@link RowDTO} objects. Each RowDTO contains the values of
   * the corresponding row in the data table.
   */
  @JsonProperty("columnData")
  @SerializedName("columnData")
  private List<RowDTO> columnData;

  /** DTO representing a single row in the data table. */
  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class RowDTO {

    /**
     * A list of column values represented by {@link ColumnValueDTO} objects. Each ColumnValueDTO
     * contains a column name and its corresponding value for this row.
     */
    @JsonProperty("row")
    @SerializedName("row")
    private List<ColumnValueDTO> row;
  }

  /** DTO representing a single column value within a row. */
  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class ColumnValueDTO {

    /** The name of the column. */
    @JsonProperty("columnName")
    private String columnName;

    /** The value associated with the column. */
    @JsonProperty("value")
    private String value;
  }
}
