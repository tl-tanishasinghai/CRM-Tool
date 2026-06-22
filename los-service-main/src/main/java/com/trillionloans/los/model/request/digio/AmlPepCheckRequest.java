package com.trillionloans.los.model.request.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.List;
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
public class AmlPepCheckRequest {
  @JsonProperty("screening_mode")
  @SerializedName("screening_mode")
  private String screeningMode;

  @JsonProperty("screening_settings")
  @SerializedName("screening_settings")
  private ScreeningSettings screeningSettings;

  @JsonProperty("user_details")
  @SerializedName("user_details")
  private UserDetails userDetails;

  @JsonProperty("rules")
  @SerializedName("rules")
  private List<String> rules;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScreeningSettings {
    @JsonProperty("enable_monitoring")
    @SerializedName("enable_monitoring")
    private boolean enableMonitoring;

    @JsonProperty("screening_type")
    @SerializedName("screening_type")
    private String screeningType;

    @JsonProperty("monitoring_frequency")
    @SerializedName("monitoring_frequency")
    private int monitoringFrequency;

    @JsonProperty("response_mode")
    @SerializedName("response_mode")
    private String responseMode;

    @JsonProperty("cut_off_threshold")
    @SerializedName("cut_off_threshold")
    private int cutOffThreshold;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserDetails {
    @JsonProperty("name")
    @SerializedName("name")
    private String name;

    @JsonProperty("pan")
    @SerializedName("pan")
    private String pan;
  }
}
