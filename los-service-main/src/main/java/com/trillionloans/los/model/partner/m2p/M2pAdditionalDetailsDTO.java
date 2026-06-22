package com.trillionloans.los.model.partner.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class M2pAdditionalDetailsDTO {
  private String dataTableName;
  private String appTable;
  private String mPin;

  @JsonProperty("subindustry")
  @SerializedName("subindustry")
  private String subIndustry;

  private String industry;

  @JsonProperty("businesstype")
  @SerializedName("businesstype")
  private String businessType;

  @JsonProperty("businessaddress")
  @SerializedName("businessaddress")
  private String businessAddress;

  private String state;
  private String city;
  private String country;
  private Integer postalCode;

  @JsonProperty("businessaddresstype")
  @SerializedName("businessaddresstype")
  private String businessAddressType;

  @JsonProperty("businessname")
  @SerializedName("businessname")
  private String businessName;

  @JsonProperty("vernacular_preference")
  @SerializedName("vernacular_preference")
  private String vernacularPreference;

  @JsonProperty("vcip_status")
  @SerializedName("vcip_status")
  private String vcipStatus;

  @JsonProperty("vcip_rejection_reason")
  @SerializedName("vcip_rejection_reason")
  private String vcipRejectionReason;

  @JsonProperty("okyc_time_stamp")
  @SerializedName("okyc_time_stamp")
  private String okycTimeStamp;

  private String ucic;

  @JsonProperty("gst_number")
  @SerializedName("gst_number")
  private String gstNumber;

  @JsonProperty("business_document")
  @SerializedName("business_document")
  private String businessDocument;

  @JsonProperty("legal_name")
  @SerializedName("legal_name")
  private String legalName;

  @JsonProperty("trade_name")
  @SerializedName("trade_name")
  private String tradeName;

  @JsonProperty("bank_bene_name")
  @SerializedName("bank_bene_name")
  private String bankBeneName;

  @JsonProperty("udyam_number")
  @SerializedName("udyam_number")
  private String udyamNumber;

  @JsonProperty("date_of_incorporation")
  @SerializedName("date_of_incorporation")
  private String dateOfIncorporation;

  @JsonProperty("category_of_business")
  @SerializedName("category_of_business")
  private String categoryOfBusiness;
}
