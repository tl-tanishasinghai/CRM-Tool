package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents the request payload for NSDL PAN Verification API. Contains a list of PAN verification
 * entries.
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class NsdlPanVerificationRequest {

  /** PAN Number of the user. Format: 10-character alphanumeric (e.g., ABCDE1234F). Mandatory. */
  @JsonProperty("pan")
  @SerializedName("pan")
  private String pan;

  /** Full name of the user (first, middle, last name or name on card). Mandatory. */
  @JsonProperty("name")
  @SerializedName("name")
  private String name;

  /**
   * Father's full name (first, middle, last name). - Mandatory for DCT category. - Optional for
   * Non-DCT category.
   */
  @JsonProperty("fathername")
  @SerializedName("fathername")
  @Builder.Default
  private String fatherName = StringUtils.EMPTY;

  /** Date of Birth in DD/MM/YYYY format. Mandatory. */
  @JsonProperty("dob")
  @SerializedName("dob")
  private String dob;
}
