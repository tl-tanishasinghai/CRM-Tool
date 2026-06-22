package com.trillionloans.lms.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeadLoanDetailsReportResponse {
  @JsonProperty("los_product_key")
  @SerializedName("los_product_key")
  private String productCode;

  @JsonProperty("CLIENTID")
  @SerializedName("CLIENTID")
  private int leadId;

  @JsonProperty("ConsumerID")
  @SerializedName("ConsumerID")
  private String consumerId;

  @JsonProperty("MobileNo")
  @SerializedName("MobileNo")
  private String mobileNo;

  @JsonProperty("FirstName")
  @SerializedName("FirstName")
  private String firstName;

  @JsonProperty("LastName")
  @SerializedName("LastName")
  private String lastName;

  @JsonProperty("Pancard")
  @SerializedName("Pancard")
  private String pancard;

  @JsonProperty("AGE")
  @SerializedName("AGE")
  private int age;

  @JsonProperty("ProductType")
  @SerializedName("ProductType")
  private String productType;

  @JsonProperty("LeadDate")
  @SerializedName("LeadDate")
  private String leadDate;

  @JsonProperty("SanctionLimit")
  @SerializedName("SanctionLimit")
  private double sanctionLimit;

  @JsonProperty("disbursedAmount")
  @SerializedName("disbursedAmount")
  private double disbursedAmount;

  @JsonProperty("CurrentDPD")
  @SerializedName("CurrentDPD")
  private int currentDPD;

  @JsonProperty("MaxDPDDays")
  @SerializedName("MaxDPDDays")
  private int maxDPDDays;

  @JsonProperty("ROI")
  @SerializedName("ROI")
  private double roi;
}
