package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating QC checks in M2P datatable. Used for name match and face match
 * validation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcChecksDataTableDTO {

  @JsonProperty("check_name")
  private String checkName;

  @JsonProperty("status")
  private String status;

  @JsonProperty("data")
  private String data;

  @JsonProperty("score")
  private String score;

  @JsonProperty("locale")
  private String locale;

  @JsonProperty("dateFormat")
  private String dateFormat;
}
