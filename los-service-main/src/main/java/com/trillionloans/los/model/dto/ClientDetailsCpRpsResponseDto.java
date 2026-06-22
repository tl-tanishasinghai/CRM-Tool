package com.trillionloans.los.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDetailsCpRpsResponseDto {
  private String name;
  private String loanAccountNumber;
  private Integer tenure;
  private String repaymentPeriodFrequencyEnum;
  private String repaymentPeriodFrequency;
  private Long productId;
}
