package com.trillionloans.los.model.request.m2p;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_KEY_CB;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Request body for kyc callbacks or eSign callback")
public class M2pKycCallBackWithAmlRequest {
  // fields common for kyc callbacks & eSign callbacks
  private Integer taskConfigId;
  private String loanApplicationId;

  // fields used for eSign callback only
  private Integer signedDocId;
  private String timeStamp;

  // fields used for kyc callbacks only
  private String clientId;

  @SerializedName("kycstatus")
  @JsonProperty("kycstatus")
  private String kycStatus;

  @SerializedName("aadhaarxmlfacematch")
  @JsonProperty("aadhaarxmlfacematch")
  private String aadhaarXmlFaceMatchPercentage;

  @SerializedName("aadhaarxmlnamematchscore")
  @JsonProperty("aadhaarxmlnamematchscore")
  private String aadhaarXmlNameMatchPercentage;

  @SerializedName("aadhaarxmlfacematchStatus")
  @JsonProperty("aadhaarxmlfacematchStatus")
  private String aadhaarXmlFaceMatchStatus;

  @SerializedName("aadhaarxmlnamematch")
  @JsonProperty("aadhaarxmlnamematch")
  private String aadhaarXmlNameMatch;

  @SerializedName("aadhaarxmlnamematchStatus")
  @JsonProperty("aadhaarxmlnamematchStatus")
  private String aadhaarXmlNameMatchStatus;

  @JsonProperty(PRODUCT_KEY_CB)
  @SerializedName(PRODUCT_KEY_CB)
  private String productCode;

  @SerializedName("aadhaarxmlvaliditycheck")
  @JsonProperty("aadhaarxmlvaliditycheck")
  private String aadhaarXmlValidityCheck;

  @SerializedName("aadhaarxmlvaliditystatus")
  @JsonProperty("aadhaarxmlvaliditystatus")
  private String aadhaarXmlValidityStatus;

  @SerializedName("aadhaarokycnamematch")
  @JsonProperty("aadhaarokycnamematch")
  private String aadhaarOkycNameMatch;

  @SerializedName("aadhaarokycnamematchstatus")
  @JsonProperty("aadhaarokycnamematchstatus")
  private String aadhaarOkycNameMatchStatus;

  @SerializedName("aadhaarokycfacematch")
  @JsonProperty("aadhaarokycfacematch")
  private String aadhaarOkycFaceMatchPercentage;

  @JsonProperty("aadhaarokycnamematchscore")
  private String aadhaarOkycNameMatchPercentage;

  @SerializedName("aadhaarokycfacematchstatus")
  @JsonProperty("aadhaarokycfacematchstatus")
  private String aadhaarOkycFaceMatchStatus;

  // fields used for cKyc callbacks only
  @SerializedName("ckycNamematchstatus")
  @JsonProperty("ckycNamematchstatus")
  private String cKycNameMatchStatus;

  @SerializedName("ckycfacematchstatus")
  @JsonProperty("ckycfacematchstatus")
  private String cKycFaceMatchStatus;

  @SerializedName("ckycNamematchscore")
  @JsonProperty("ckycNamematchscore")
  private String cKycNameMatchScore;

  @SerializedName("ckycfacematchscore")
  @JsonProperty("ckycfacematchscore")
  private String cKycFaceMatchScore;

  @SerializedName("amlStatus")
  @JsonProperty("amlStatus")
  private String amlStatus;

  @SerializedName("amlstatusCode")
  @JsonProperty("amlstatusCode")
  private String amlStatusCode;

  @SerializedName("bestMatchName")
  @JsonProperty("bestMatchName")
  private String amlBestMatchName;

  @SerializedName("bestMatchScore")
  @JsonProperty("bestMatchScore")
  private String amlBestMatchScore;

  @SerializedName("pepMatch")
  @JsonProperty("pepMatch")
  private String pepMatch;

  @SerializedName("amlThreshold")
  @JsonProperty("amlThreshold")
  private String amlThreshold;

  @SerializedName("document_type_ids")
  @JsonProperty("document_type_ids")
  private String documentTypeIds;

  @SerializedName("document_keys")
  @JsonProperty("document_keys")
  private String documentKeys;

  private List<String> kycRejectionReason;

  @SerializedName("rejectReason")
  @JsonProperty("rejectReason")
  private String rejectionReasonCategory;

  @SerializedName("ReasonForReject")
  @JsonProperty("ReasonForReject")
  private String remarks;

  private String workflowName;

  private String status;
}
