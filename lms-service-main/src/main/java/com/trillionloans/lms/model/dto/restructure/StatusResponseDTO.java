package com.trillionloans.lms.model.dto.restructure;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing restructure status response.
 *
 * @author Pawan Kumar
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Restructure status response DTO")
public class StatusResponseDTO {

  @Schema(description = "Restructure status: SUCCESS, FAIL, or NOT_TRIGGERED")
  private String status;

  @Schema(description = "Loan account number (LAN)")
  private Long lanId;

  @Schema(description = "Approval date in dd MMMM yyyy format, or null")
  private String approvedOnDate;

  @Schema(description = "Request ID from loan_application_restructure_details")
  private Long requestId;
}
