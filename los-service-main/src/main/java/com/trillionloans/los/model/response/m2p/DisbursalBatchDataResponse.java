package com.trillionloans.los.model.response.m2p;

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
public class DisbursalBatchDataResponse {

  // TODO: Need to add JsonProperty and SerializedName as per the M2p report api

  private String bankHolderName;
  private String loanAccountNumber;
  private String transactionDate;
  private String loanApplicationId;
  private String grossDisbursalAmount;
  private String netDisbursalAmount;
  private String ifscCode;
  private String bankName;
  private String bankAccount;
  private String clientId;
  private String clientName;
  private String balanceTransferOutstanding;
  private String partner;
  private String balanceTransferCustomerExistingLoanId;
  private String ancId;
}
