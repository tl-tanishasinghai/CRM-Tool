package com.trillionloans.los.model.partner.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class M2pLoanAdditionalDetailsDTO {

  @JsonProperty("utr_number")
  private String utrNumber;

  @JsonProperty("total_upfront_amount")
  private String totalUpfrontAmount;

  @JsonProperty("processing_fee")
  private String processingFee;

  @JsonProperty("loan_application_id")
  private String loanApplicationId;

  @JsonProperty("insurance_premium")
  private String insurancePremium;

  @JsonProperty("down_payment_rate")
  private String downPaymentRate;

  @JsonProperty("down_payment_amount")
  private String downPaymentAmount;

  @JsonProperty("processing_fee_gst")
  private String processingFeeGst;

  @JsonProperty("net_amount_financed")
  private String netAmountFinanced;

  @JsonProperty("upfront_collection_date")
  private String upfrontCollectionDate;

  @JsonProperty("apr")
  private String apr;
}
