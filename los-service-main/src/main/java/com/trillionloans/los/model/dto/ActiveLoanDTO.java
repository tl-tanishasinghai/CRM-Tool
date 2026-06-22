package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
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
public class ActiveLoanDTO {
  String loanApplicationId;

  @JsonProperty("loan_product_id")
  @SerializedName("loan_product_id")
  String productId;

  BigDecimal disbursedAmount;
}
