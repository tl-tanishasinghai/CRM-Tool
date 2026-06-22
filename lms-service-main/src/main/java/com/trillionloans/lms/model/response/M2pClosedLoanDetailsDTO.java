package com.trillionloans.lms.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class M2pClosedLoanDetailsDTO {

  @JsonProperty("loan_id")
  @SerializedName("loan_id")
  private int loanId;

  @JsonProperty("closing_date")
  @SerializedName("closing_date")
  private String closingDate;

  @JsonProperty("loan_account_number")
  @SerializedName("loan_account_number")
  private String loanAccountNumber;

  @JsonProperty("Product_Short_Name")
  @SerializedName("Product_Short_Name")
  private String productShortName;

  @JsonProperty("Client_Name")
  @SerializedName("Client_Name")
  private String clientName;

  @JsonProperty("Mobile_Number")
  @SerializedName("Mobile_Number")
  private String mobileNumber;
}
