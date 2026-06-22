package com.trillionloans.los.model;

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
public class NsdlPanVerificationResponse {

  @JsonProperty("response_Code")
  @SerializedName("response_Code")
  private String responseCode;

  private List<OutputData> outputData;

  /** Represents one PAN verification record from OPV API response. */
  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OutputData {

    @JsonProperty("pan")
    @SerializedName("pan")
    private String pan;

    /**
     * PAN verification status code.
     *
     * <p>For possible values, see {@link com.trillionloans.los.mapper.PanStatus}.
     */
    @JsonProperty("pan_status")
    @SerializedName("pan_status")
    private String panStatus;

    /**
     * Name match flag (VARCHAR 1). Y - YES (matched with database) N - NO (not matched with
     * database)
     */
    @JsonProperty("name")
    @SerializedName("name")
    private String name;

    /**
     * Father's name match flag (VARCHAR 1, Optional). Only for DCT category when PAN is valid. Y -
     * YES (matched with database) N - NO (not matched with database) Blank for other categories.
     */
    @JsonProperty("fathername")
    @SerializedName("fathername")
    private String fatherName;

    /**
     * Date of birth match flag (VARCHAR 1). Y - YES (matched with database) N - NO (not matched
     * with database)
     */
    @JsonProperty("dob")
    @SerializedName("dob")
    private String dob;

    /**
     * PAN seeding status (VARCHAR 2). Y - Operative PAN R - Inoperative PAN NA - Non-individual
     * PANs
     */
    @JsonProperty("seeding_status")
    @SerializedName("seeding_status")
    private String seedingStatus;
  }
}
