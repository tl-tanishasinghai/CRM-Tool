package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class M2pPanAadhaarDetailsDTO {
  @JsonProperty("Aadhaar_ID")
  @SerializedName("Aadhaar_ID")
  String aadhaarId;

  @JsonProperty("PAN_NUMBER")
  @SerializedName("PAN_NUMBER")
  String panNumber;

  @JsonProperty("KYC_DOB")
  @SerializedName("KYC_DOB")
  String kycDob;

  @JsonProperty("CLIENT_DOB")
  @SerializedName("CLIENT_DOB")
  String clientDob;
}
