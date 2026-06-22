package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for charge details from partial waive template response.
 *
 * @author Amar Bhosale
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Charge details for partial waiver")
public class PartialWaiveChargeDTO {

  @Schema(description = "Charge ID", example = "32")
  private Long chargeId;

  @Schema(description = "Charge name", example = "Penal Interest")
  private String name;

  @Schema(description = "Total charge amount", example = "1000.00")
  private Double amount;

  @Schema(description = "Outstanding amount to be waived", example = "1000.00")
  private Double amountOutstanding;

  @Schema(description = "Already waived amount", example = "0.00")
  private Double amountWaived;
}
