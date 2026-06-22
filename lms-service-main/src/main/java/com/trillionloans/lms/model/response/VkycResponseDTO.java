package com.trillionloans.lms.model.response;

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
@AllArgsConstructor
@NoArgsConstructor
public class VkycResponseDTO {

  @JsonProperty("status")
  @SerializedName("status")
  private String status;

  @JsonProperty("additionalInfo")
  @SerializedName("additionalInfo")
  private AdditionalInfo additionalInfo;

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Getter
  @Setter
  public static class AdditionalInfo {

    @JsonProperty("vcipId")
    @SerializedName("vcipId")
    private String vcipId;

    @JsonProperty("vciplink")
    @SerializedName("vciplink")
    private String vcipLink;

    @JsonProperty("respCode")
    @SerializedName("respCode")
    public String respCode;

    @JsonProperty("respDesc")
    @SerializedName("respDesc")
    public String respDesc;
  }
}
