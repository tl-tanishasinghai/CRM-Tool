package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanVerificationLog {

  @JsonProperty("customer_id")
  @SerializedName("customer_id")
  private String customerId;

  @JsonProperty("loan_application_id")
  @SerializedName("loan_application_id")
  private String loanApplicationId;

  @JsonProperty("pan_number")
  @SerializedName("pan_number")
  private String panNumber;

  @JsonProperty("name_entered")
  @SerializedName("name_entered")
  private String nameEntered;

  @JsonProperty("name_match_result")
  @SerializedName("name_match_result")
  private String nameMatchResult;

  @JsonProperty("dob_entered")
  @SerializedName("dob_entered")
  private String dobEntered;

  @JsonProperty("dob_match_result")
  @SerializedName("dob_match_result")
  private String dobMatchResult;

  @JsonProperty("pan_status")
  @SerializedName("pan_status")
  private String panStatus;

  @JsonProperty("seeding_status")
  @SerializedName("seeding_status")
  private String seedingStatus;

  @JsonProperty("final_verification_result")
  @SerializedName("final_verification_result")
  private FinalVerificationResult finalVerificationResult;

  @JsonProperty("verification_timestamp")
  @SerializedName("verification_timestamp")
  private String verificationTimestamp;

  @JsonProperty("rejection_reason")
  @SerializedName("rejection_reason")
  private String rejectionReason;

  public enum FinalVerificationResult {
    SUCCESS,
    FAILURE,
  }
}
