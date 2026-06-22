package com.trillionloans.los.model.response.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class M2pMaxDpdResponseDto {

  private int clientId;

  private int loanId;

  private String losProductKey;

  private int maxDpd;
}
