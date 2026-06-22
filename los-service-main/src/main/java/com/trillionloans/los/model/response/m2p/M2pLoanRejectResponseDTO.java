package com.trillionloans.los.model.response.m2p;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class M2pLoanRejectResponseDTO {
  private int officeId;
  private int clientId;
  private int resourceId;
  private Changes changes;

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Changes {
    private int statusEnum;
    private int leadStatus;
  }
}
