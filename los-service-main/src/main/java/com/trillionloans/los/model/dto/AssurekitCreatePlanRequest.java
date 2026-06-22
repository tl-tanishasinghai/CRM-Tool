package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssurekitCreatePlanRequest {
  private String programId;
  private String name;
  private String phone;
  private Double loanAmount;
  private String loanStartTime;
  private Integer tenureOfLoan;
  private String loanId;
  private String planName;
}
