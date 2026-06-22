package com.trillionloans.los.model.partner.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class M2pBeneficiaryBankDetailsDTO {
  private String name;
  private String accountNumber;
  private String ifscCode;
  private int accountTypeId;
  private String locale;
  private String bankName;
  private String branchName;
  private String bankCity;
  private String mobileNumber;
}
