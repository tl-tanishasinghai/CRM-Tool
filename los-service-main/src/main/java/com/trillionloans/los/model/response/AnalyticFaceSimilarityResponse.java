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
public class AnalyticFaceSimilarityResponse {
  @JsonProperty("face_match_percent")
  @SerializedName("face_match_percent")
  private Double faceMatchPercent;

  @JsonProperty("time_taken")
  @SerializedName("time_taken")
  private Double timeTaken;
}
