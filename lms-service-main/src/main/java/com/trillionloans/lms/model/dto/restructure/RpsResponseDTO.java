package com.trillionloans.lms.model.dto.restructure;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing RPS (Repayment Schedule) response for loan restructure.
 *
 * @author Pawan Kumar
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "RPS Response DTO for loan restructure")
public class RpsResponseDTO {

  @Schema(description = "Repayment schedule data as object")
  private Object data;

  @Schema(description = "Status message when restructure is NOT_TRIGGERED/FAIL or completed")
  private String message;
}
