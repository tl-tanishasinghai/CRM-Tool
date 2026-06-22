package com.trillionloans.los.model.response.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DigioUploadPdfResponse {

  private String id;

  @JsonProperty("is_agreement")
  @SerializedName("is_agreement")
  private Boolean isAgreement;

  @JsonProperty("agreement_type")
  @SerializedName("agreement_type")
  private String agreementType;

  @JsonProperty("agreement_status")
  @SerializedName("agreement_status")
  private String agreementStatus;

  @JsonProperty("file_name")
  @SerializedName("file_name")
  private String fileName;

  @JsonProperty("created_at")
  @SerializedName("created_at")
  private String createdAt;

  @JsonProperty("self_signed")
  @SerializedName("self_signed")
  private Boolean selfSigned;

  @JsonProperty("self_sign_type")
  @SerializedName("self_sign_type")
  private String selfSignType;

  @JsonProperty("no_of_pages")
  @SerializedName("no_of_pages")
  private Integer noOfPages;

  @JsonProperty("signing_parties")
  @SerializedName("signing_parties")
  private List<SigningParty> signingParties;

  @JsonProperty("sign_request_details")
  @SerializedName("sign_request_details")
  private SignRequestDetails signRequestDetails;

  private String channel;

  @JsonProperty("require_full_document_review")
  @SerializedName("require_full_document_review")
  private Boolean requireFullDocumentReview;

  @JsonProperty("other_doc_details")
  @SerializedName("other_doc_details")
  private Object otherDocDetails;

  @JsonProperty("attached_estamp_details")
  @SerializedName("attached_estamp_details")
  private Object attachedEstampDetails;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SigningParty {
    private String name;
    private String status;
    private String type;

    @JsonProperty("signature_type")
    @SerializedName("signature_type")
    private String signatureType;

    private String identifier;
    private String reason;

    @JsonProperty("expire_on")
    @SerializedName("expire_on")
    private String expireOn;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SignRequestDetails {
    private String name;

    @JsonProperty("requested_on")
    @SerializedName("requested_on")
    private String requestedOn;

    @JsonProperty("expire_on")
    @SerializedName("expire_on")
    private String expireOn;

    private String identifier;

    @JsonProperty("requester_type")
    @SerializedName("requester_type")
    private String requesterType;
  }
}
