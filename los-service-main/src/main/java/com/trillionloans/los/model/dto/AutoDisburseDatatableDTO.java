package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Auto Disbursal Datatable DTO")
public class AutoDisburseDatatableDTO {

  @JsonProperty("autodisbursed")
  @SerializedName(("autodisbursed"))
  private boolean autoDisbursed;

  private String locale;
  private String dateFormat;
}
