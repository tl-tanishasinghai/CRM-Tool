package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NexusColorCodeResponse {

  @JsonProperty("pincode")
  @SerializedName("pincode")
  private String pinCode;

  @JsonProperty("color_code")
  @SerializedName("color_code")
  private String colorCode;
}
