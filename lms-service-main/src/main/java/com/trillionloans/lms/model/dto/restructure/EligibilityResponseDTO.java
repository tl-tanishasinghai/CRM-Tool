package com.trillionloans.lms.model.dto.restructure;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing eligibility response for loan restructure.
 *
 * @author Pawan Kumar
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
@Schema(description = "Eligibility Response DTO for loan restructure")
public class EligibilityResponseDTO {

  @Schema(description = "Whether the loan is eligible for restructure", example = "true")
  private Boolean eligible;

  @Schema(description = "Days Past Due", example = "30")
  private Integer dpd;

  @Schema(description = "Residual Tenure (remaining EMIs)", example = "167")
  private Integer residualTenure;

  @Schema(description = "Number of paid repayments", example = "39")
  private Integer paidRepayments;

  @Schema(description = "Principal Outstanding", example = "50000.00")
  private Double pos;

  @Schema(description = "Total Outstanding", example = "55000.00")
  private Double tos;

  @Schema(description = "Tentative repayment schedule data")
  private TentativeRpsResponseDTO tentativeRps;

  @Schema(description = "Request ID from loan_application_restructure_details insert")
  private Long requestId;

  @Schema(
      description = "Reason for ineligibility when eligible is false",
      example = "DPD not in allowed range [10, 60)")
  private String reason;

  @Schema(description = "Status message when restructure is NOT_TRIGGERED/FAIL or completed")
  private String message;
}
