package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for approve restructure API.
 *
 * @author Amar Bhosale
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Approve Restructure Response DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApproveRestructureResponseDTO {

  @Schema(description = "Status of the approval", example = "SUCCESS")
  private String status;

  @Schema(description = "Loan Account Number", example = "17253")
  private Long lanId;

  @Schema(description = "Date when restructure was approved", example = "13 February 2026")
  private String approvedOnDate;

  @Schema(description = "Reschedule request ID", example = "61")
  private Long requestId;

  @Schema(description = "Error message if approval failed")
  private String message;
}
