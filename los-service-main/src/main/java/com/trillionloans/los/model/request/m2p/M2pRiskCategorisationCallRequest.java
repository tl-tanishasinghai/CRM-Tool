package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class M2pRiskCategorisationCallRequest {

  @JsonProperty("risk_cd_risk")
  private String riskCdRisk;

  private String date;
  private String reason;
  private String locale;
  private String dateFormat;
}
