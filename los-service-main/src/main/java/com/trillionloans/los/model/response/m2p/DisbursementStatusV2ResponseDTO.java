package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DisbursementStatusV2ResponseDTO {

  @JsonProperty("loanApplicationId")
  private Integer loanApplicationId;

  @JsonProperty("lanID")
  private Integer lanID;

  @JsonProperty("approvedAmount")
  private Double approvedAmount;

  @JsonProperty("netDisbursement")
  private Double netDisbursement;

  @JsonProperty("status")
  private String status;

  @JsonProperty("receiptNumber")
  private String receiptNumber;

  @JsonProperty("disbursementDate")
  private String disbursementDate;

  @JsonProperty("productkey")
  private String productKey;

  @JsonProperty("repaymentStartingDate")
  private String repaymentStartingDate;
}
