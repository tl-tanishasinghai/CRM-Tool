package com.trillionloans.customer_portal.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RPSInfo {
  private String nextDueDate;
  private Integer dpdDays;
}
