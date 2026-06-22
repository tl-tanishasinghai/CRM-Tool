package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class M2pGetKycStatusResponseDTO {

  @JsonProperty("kycStatus")
  private String kycStatus;

  // OKYC fields
  @JsonProperty("okycfacematch")
  private String okycFaceMatch;

  @JsonProperty("okycfacematchStatus")
  private String okycFaceMatchStatus;

  @JsonProperty("okycnamematch")
  private String okycNameMatch;

  @JsonProperty("okycnamematchscore")
  private String okycNameMatchScore;

  @JsonProperty("okycnamematchStatus")
  private String okycNameMatchStatus;

  // Aadhaar XML fields
  @JsonProperty("aadhaarxmlvaliditycheck")
  private String aadhaarXmlValidityCheck;

  @JsonProperty("aadhaarxmlvaliditystatus")
  private String aadhaarXmlValidityStatus;

  @JsonProperty("aadhaarxmlfacematch")
  private String aadhaarXmlFaceMatch;

  @JsonProperty("aadhaarxmlfacematchStatus")
  private String aadhaarXmlFaceMatchStatus;

  @JsonProperty("aadhaarxmlnamematch")
  private String aadhaarXmlNameMatch;

  @JsonProperty("aadhaarxmlnamematchscore")
  private String aadhaarXmlNameMatchScore;

  @JsonProperty("aadhaarxmlnamematchStatus")
  private String aadhaarXmlNameMatchStatus;

  @JsonProperty("amlStatus")
  private String amlStatus;

  @JsonProperty("loanApplicationId")
  private Integer loanApplicationId;

  @JsonProperty("clientId")
  private Long clientId;

  // Failure reason list — included only when rejected
  @JsonProperty("kycFailureReason")
  private List<String> kycFailureReason;

  @JsonProperty("aadhaarxmlvalidityError")
  private String aadhaarXmlValidityError;

  @JsonProperty("aadhaarxmlfacematchError")
  private String aadhaarXmlFaceMatchError;

  @JsonProperty("aadhaarxmlnamematchError")
  private String aadhaarXmlNameMatchError;

  @JsonProperty("okycfacematchStatusError")
  private String okycFaceMatchStatusError;

  @JsonProperty("okycnamematchError")
  private String okycNameMatchError;

  @JsonProperty("pepMatch")
  private String pepMatch;
}
