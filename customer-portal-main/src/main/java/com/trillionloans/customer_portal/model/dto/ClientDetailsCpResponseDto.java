package com.trillionloans.customer_portal.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDetailsCpResponseDto {
  private String name;
  private String loanAccountNumber;
  private Integer tenure;
  private String repaymentPeriodFrequencyEnum;
  private Long productId;
  private String repaymentPeriodFrequency;
}
