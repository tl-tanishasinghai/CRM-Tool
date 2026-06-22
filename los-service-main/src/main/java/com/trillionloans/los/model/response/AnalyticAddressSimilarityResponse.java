package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticAddressSimilarityResponse {
  @JsonProperty("score")
  @SerializedName("score")
  private String score;
}
