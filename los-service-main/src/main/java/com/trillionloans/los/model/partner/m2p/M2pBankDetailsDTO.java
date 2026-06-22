package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class M2pBankDetailsDTO {
  private String accountType;
  private String name;
  private String accountNumber;
  private String ifscCode;
  private Boolean supportedForRepayment;
  private Boolean supportedForDisbursement;
}
