package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class M2PCreditLineLoanApplicationRequestDTO {
  private final Integer loanOfficerId;
  private final Integer loanPurposeId;
  private Integer sourcingChannelId;
  private String externalIdOne;
  private String externalIdTwo;
  private final Double amount;
  private final String losProductKey;
}
