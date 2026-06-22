package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Currency details within tentative RPS response.
 *
 * @author Pawan Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Currency details")
public class TentativeRpsCurrencyDTO {

  @Schema(description = "Currency code")
  private String code;

  @Schema(description = "Currency name")
  private String name;

  @Schema(description = "Decimal places")
  private Integer decimalPlaces;

  @Schema(description = "In multiples of")
  private Integer inMultiplesOf;

  @Schema(description = "Display symbol")
  private String displaySymbol;

  @Schema(description = "Name code")
  private String nameCode;

  @Schema(description = "Display label")
  private String displayLabel;
}
