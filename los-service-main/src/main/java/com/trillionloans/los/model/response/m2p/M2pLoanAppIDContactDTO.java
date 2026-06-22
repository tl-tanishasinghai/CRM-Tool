package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class M2pLoanAppIDContactDTO {

  @JsonProperty("mobile_no")
  @SerializedName("mobile_no")
  private String mobileNo;

  @JsonProperty("loan_application_reference_no")
  @SerializedName("loan_application_reference_no")
  private String loanApplicationReferenceNo;
}
