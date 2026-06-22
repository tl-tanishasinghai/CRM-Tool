package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Eligible loan entry for manual disbursal batch")
public class EligibleLoanDTO {

  @JsonProperty("referenceId1")
  private String referenceId1;

  @JsonProperty("productCode")
  private String productCode;

  @JsonProperty("disburseStatus")
  private String disburseStatus;

  @JsonProperty("partnerName")
  private String partnerName;

  @JsonProperty("grossDisbursalAmount")
  private String grossDisbursalAmount;

  @JsonProperty("netDisbursalAmount")
  private String netDisbursalAmount;
}
