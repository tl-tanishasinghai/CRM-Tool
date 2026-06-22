package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class BreProcessStatusRequestDTO {
  private Object request;
  private String stage;
  private String productCode;
  private String loanId;
  private Throwable error;
  private String status;
  private boolean isActive;
  private String scienapticStatus;
}
