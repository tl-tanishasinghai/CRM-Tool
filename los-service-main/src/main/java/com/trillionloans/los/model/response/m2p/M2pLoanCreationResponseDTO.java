package com.trillionloans.los.model.response.m2p;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class M2pLoanCreationResponseDTO {
  private Integer clientId;
  private Integer resourceId;
  private Boolean rollbackTransaction;
  private AdditionalResponseData additionalResponseData;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class AdditionalResponseData {
    private String loanApplicationReferenceNo;
  }
}
