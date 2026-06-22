package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadLoanDataResponseDTO {

  @JsonProperty("typeofAccount")
  @SerializedName("typeofAccount")
  public String typeofAccount;

  @JsonProperty("status_enum")
  @SerializedName("status_enum")
  public Integer status;

  @JsonProperty("CLIENTID")
  @SerializedName("CLIENTID")
  public Integer clientId;

  @JsonProperty("ProductType")
  @SerializedName("ProductType")
  public String productType;

  @JsonProperty("LeadDate")
  @SerializedName("LeadDate")
  public String leadDate;

  @JsonProperty("SanctionLimit/Amount")
  @SerializedName("SanctionLimit/Amount")
  public BigDecimal sanctionLimitAmount;

  @JsonProperty("DisbursedAmount/DrawdownAmount")
  @SerializedName("DisbursedAmount/DrawdownAmount")
  public BigDecimal disbursedAmountOrDrawdownAmount;

  @JsonProperty("CurrentDPD")
  @SerializedName("CurrentDPD")
  public Integer currentDPD;

  @JsonProperty("MaxDPDDays")
  @SerializedName("MaxDPDDays")
  public Integer maxDPDDays;
}
