package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trillionloans.lms.model.dto.TimelineDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for parsing retrieveLoan response in restructure eligibility flow.
 *
 * @author Pawan Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Response DTO for retrieve loan in restructure flow")
public class RetrieveLoanResponseDTO {

  @Schema(description = "Days Past Due", example = "19")
  private Integer dpdDays;

  @Schema(description = "Whether loan is NPA (Non-Performing Asset)", example = "false")
  private Boolean isNPA;

  @Schema(description = "Total number of repayments due", example = "104")
  private Integer numberOfDueRepayments;

  @Schema(description = "Number of paid repayments", example = "30")
  private Integer numberOfPaidRepayments;

  @Schema(description = "Summary details containing outstanding amounts")
  private RetrieveLoanSummaryDTO summary;

  @Schema(description = "Timeline containing actual disbursement date")
  private TimelineDTO timeline;
}
