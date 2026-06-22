package com.trillionloans.los.model.response;

import java.math.BigDecimal;
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
public class GetLoanV2ResponseDTO {
  private Integer loanId;
  private Integer loanApplicationId;
  private Integer clientId;
  private String utrNumber;
  private BigDecimal approvedAmount;
  private BigDecimal netDisburseAmount;
  private String disbursedDate;
  private String loanApplicationStatus;
  private String stepName;
}
