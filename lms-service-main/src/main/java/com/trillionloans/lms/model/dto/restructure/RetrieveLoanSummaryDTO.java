package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary details within RetrieveLoanResponseDTO.
 *
 * @author Pawan Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Summary details within retrieve loan response")
public class RetrieveLoanSummaryDTO {

  @Schema(description = "Principal Outstanding amount", example = "23357.19")
  private Double principalOutstanding;

  @Schema(description = "Total Outstanding amount", example = "24764.36")
  private Double totalOutstanding;
}
