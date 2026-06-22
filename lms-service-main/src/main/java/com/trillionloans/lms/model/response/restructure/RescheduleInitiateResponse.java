package com.trillionloans.lms.model.response.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for M2P reschedule initiation.
 *
 * @author Amar Bhosale
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Reschedule Initiate Response from M2P")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RescheduleInitiateResponse {

  @Schema(description = "Resource ID returned by M2P (reschedule request ID)", example = "54")
  private Long resourceId;

  @Schema(description = "Loan ID for which reschedule was initiated", example = "15736")
  private Long loanId;

  @Schema(description = "Client ID associated with the loan", example = "34781")
  private Long clientId;
}
