package com.trillionloans.los.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationFunnelVerificationResultCallbackLog {

  @JsonProperty("customer_id")
  @SerializedName("customer_id")
  private String clientId;

  @JsonProperty("loan_application_id")
  @SerializedName("loan_application_id")
  private String leadId;

  // --- Verification Results ---
  @JsonProperty("nsdl_pan_verification")
  @SerializedName("nsdl_pan_verification")
  private String nsdlPanVerification;

  @JsonProperty("karza_pan_verification")
  @SerializedName("karza_pan_verification")
  private String karzaPanVerification;

  @JsonProperty("karza_name_verification")
  @SerializedName("karza_name_verification")
  private String karzaNameVerification;

  @JsonProperty("dob_waterfall_result")
  @SerializedName("dob_waterfall_result")
  private String dobWaterfallResult;

  @JsonProperty("timestamp")
  @SerializedName("timestamp")
  private String timestamp;

  // --- Final Outcome ---
  @JsonProperty("final_verification_result")
  @SerializedName("final_verification_result")
  private String finalVerificationResult;

  @JsonProperty("name_fuzzy_match_percentage")
  @SerializedName("name_fuzzy_match_percentage")
  private String nameFuzzyMatchPercentage;
}
