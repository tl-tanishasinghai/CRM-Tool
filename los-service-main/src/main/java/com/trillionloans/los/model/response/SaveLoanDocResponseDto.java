package com.trillionloans.los.model.response;

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
public class SaveLoanDocResponseDto {
  private String loanId;
  private String clientId;
  private String message;
}
