package com.trillionloans.lms.model.request.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for approving loan reschedule with M2P.
 *
 * @author Amar Bhosale
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Approve Reschedule Request for M2P")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApproveRescheduleRequest {

  @Schema(description = "Date format pattern", example = "dd MMMM yyyy")
  @Builder.Default
  private String dateFormat = "dd MMMM yyyy";

  @Schema(description = "Locale for the request", example = "en")
  @Builder.Default
  private String locale = "en";

  @Schema(description = "Date when restructure is approved", example = "02 February 2026")
  private String approvedOnDate;
}
