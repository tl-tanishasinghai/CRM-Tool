package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskDetailResponseDTO {

  @JsonProperty("client_id")
  @SerializedName("client_id")
  private Long clientId;

  @JsonProperty("score_value")
  @SerializedName("score_value")
  private String scoreValue;

  @JsonProperty("postal_code")
  @SerializedName("postal_code")
  private String postalCode;
}
