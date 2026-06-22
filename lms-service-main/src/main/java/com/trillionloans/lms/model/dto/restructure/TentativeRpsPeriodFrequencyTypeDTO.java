package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Period frequency type within tentative RPS response.
 *
 * @author Pawan Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Period frequency type")
public class TentativeRpsPeriodFrequencyTypeDTO {

  @Schema(description = "ID")
  private Integer id;

  @Schema(description = "Code")
  private String code;

  @Schema(description = "Value")
  private String value;
}
