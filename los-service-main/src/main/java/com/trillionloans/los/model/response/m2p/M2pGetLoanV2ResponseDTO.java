package com.trillionloans.los.model.response.m2p;

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
public class M2pGetLoanV2ResponseDTO {
  @JsonProperty("Loan_id")
  @SerializedName("Loan_id")
  private Integer loanId;

  @JsonProperty("loanapplicationid")
  @SerializedName("loanapplicationid")
  private Integer loanApplicationId;

  @JsonProperty("clientId")
  @SerializedName("clientId")
  private Integer clientId;

  @JsonProperty("UtrNumber")
  @SerializedName("UtrNumber")
  private String utrNumber;

  @JsonProperty("approvedamount")
  @SerializedName("approvedamount")
  private BigDecimal approvedAmount;

  @JsonProperty("Netdisburseamount")
  @SerializedName("Netdisburseamount")
  private BigDecimal netDisburseAmount;

  @JsonProperty("Disbursed date")
  @SerializedName("Disbursed date")
  private String disbursedDate;

  @JsonProperty("loanApplicationStatus")
  @SerializedName("loanApplicationStatus")
  private String loanApplicationStatus;

  private String stepName;

  @JsonProperty("losProductKey")
  @SerializedName("losProductKey")
  private String losProductKey;
}
