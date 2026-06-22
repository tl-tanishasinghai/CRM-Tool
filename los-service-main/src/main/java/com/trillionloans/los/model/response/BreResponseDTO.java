package com.trillionloans.los.model.response;

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
public class BreResponseDTO {

  private String loanId;
  private String action;
  private boolean success;
  private Double loanAmount;
  private Double roi;
  private Integer tenure;
}
