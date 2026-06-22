package com.trillionloans.los.model.partner.m2p;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class M2PGenerateCreditLineRequestDTO {
  private Integer allowedLimit; //  Usage Limit should be between 1000 and 500000
  private TenureDetails tenureDetails; //  Validity should be between 1 and 60
  private String agreementIdentifier;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  public static class TenureDetails {
    private Integer value;
    private String type; // Could be enum later (e.g., MONTHS, DAYS)
  }
}
