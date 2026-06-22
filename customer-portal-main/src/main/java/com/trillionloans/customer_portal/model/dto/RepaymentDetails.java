package com.trillionloans.customer_portal.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentDetails {
  private Double principalDue;
  private Double interestDue;
  private Double chargesDue;
}