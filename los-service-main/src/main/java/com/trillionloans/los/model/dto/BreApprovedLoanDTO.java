package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class BreApprovedLoanDTO {
  @JsonProperty("loan_application_id")
  @SerializedName("loan_application_id")
  String loanApplicationId;

  @JsonProperty("client_id")
  @SerializedName("client_id")
  String clientId;

  @JsonProperty("product_code")
  @SerializedName("product_code")
  String productCode;

  @JsonProperty("dateformat")
  @SerializedName("dateformat")
  String breOfferApprovedOn;
}
