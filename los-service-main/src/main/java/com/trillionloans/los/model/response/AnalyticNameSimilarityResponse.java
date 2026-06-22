package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticNameSimilarityResponse {
  @JsonProperty("name_match_percent")
  @SerializedName("name_match_percent")
  private Double nameMatchPercent;
}
