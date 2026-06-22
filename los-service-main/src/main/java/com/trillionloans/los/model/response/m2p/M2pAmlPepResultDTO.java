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
public class M2pAmlPepResultDTO {
  private Long loanApplicationReferenceId;
  private String amlDecision;
  private String pepResult;
}
