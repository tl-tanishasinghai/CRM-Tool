package com.trillionloans.lms.model.request.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for partial charge waiver with M2P.
 *
 * @author Amar Bhosale
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Partial Waiver Request for M2P")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartialWaiverRequest {

  @Schema(description = "List of charges to waive")
  private List<ChargeWaiver> charges;

  @Schema(description = "Locale for the request", example = "en")
  @Builder.Default
  private String locale = "en";

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "Individual charge waiver details")
  public static class ChargeWaiver {

    @Schema(description = "Charge ID to waive", example = "32")
    private Long chargeId;

    @Schema(description = "Amount to waive", example = "1000.00")
    private Double waiverAmount;
  }
}
