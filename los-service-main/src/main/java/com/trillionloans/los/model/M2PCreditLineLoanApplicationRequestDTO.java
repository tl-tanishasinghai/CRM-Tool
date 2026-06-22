package com.trillionloans.los.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class M2PCreditLineLoanApplicationRequestDTO {
  private String channelMode;
  private Long loanPurposeId;
  private Long loanOfficerId;
  private Long sourcingChannelId;
  private String externalIdOne;
  private String externalIdTwo;
  private Long amount;
  private String losProductKey;
}
