package com.trillionloans.lms.model.response.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for M2P reschedule approval.
 *
 * @author Amar Bhosale
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Approve Reschedule Response from M2P")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApproveRescheduleResponse {

  @Schema(description = "Resource ID (reschedule request ID)", example = "61")
  private Long resourceId;

  @Schema(description = "Loan ID for which reschedule was approved", example = "17253")
  private Long loanId;

  @Schema(description = "Client ID associated with the loan", example = "34781")
  private Long clientId;

  @Schema(description = "Changes made during approval")
  private Object changes;
}
