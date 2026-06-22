package com.trillionloans.los.model.partner.m2p;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class M2PGenerateCreditLineResponseDTO {
  private Long leadId;
  private String accountNumber;
  private String status;
  private Integer allowedLimit;
}
