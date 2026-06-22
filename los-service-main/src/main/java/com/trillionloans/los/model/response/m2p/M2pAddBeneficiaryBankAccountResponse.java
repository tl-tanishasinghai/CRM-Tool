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
public class M2pAddBeneficiaryBankAccountResponse {
  private String clientId;
  private String clientThirdPartyBankAccountDetailId;
  private String clientThirdPartyBankAccountDetailAssociationId;
  private boolean isAccountAlreadyActive;
}
