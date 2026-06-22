package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "AML/PEP Datatable DTO")
public class AmlPepDatatableDTO {

  @JsonProperty("lead_id")
  @Schema(description = "Lead/Application ID")
  private String leadId;

  @JsonProperty("aml_decision")
  @Schema(description = "AML Decision: Pass / Manual Review / Reject")
  private String amlDecision;

  @JsonProperty("aml_name_match_score")
  @Schema(description = "AML Match Score in Percentage")
  private Double amlNameMatchScore;

  @JsonProperty("pep_result")
  @Schema(description = "PEP Result: Y or N")
  private String pepResult;

  @JsonProperty("reason_description")
  @Schema(description = "Reason Description for decision")
  private String reasonDescription;

  @JsonProperty("decision_date")
  @Schema(description = "Date of Decision")
  private String decisionDate;

  @Schema(description = "Date Format for decisiondate")
  private String dateFormat;

  @Schema(description = "Locale parameter required by M2P when amlnamematchscore is provided")
  private String locale;
}
