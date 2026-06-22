package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskDetailsDataTableDTO {

  @JsonProperty("lead_id")
  private String leadId;

  @JsonProperty("loan_id")
  private String loanId;

  @JsonProperty("risk")
  private String risk;

  @JsonProperty("timestamp")
  private String timestamp;
}
