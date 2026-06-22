package com.trillionloans.los.model.response.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class M2pGetBeneficiaryBankDetailsResponse {
  private String clientThirdPartyBankAccountDetailAssociationId;
  public BankAccountDetails bankAccountDetails;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public class BankAccountDetails {
    public String id;
    public String name;
    public String accountNumber;
    public String ifscCode;
    public String bankName;
    public String bankCity;
    public String branchName;
  }
}
