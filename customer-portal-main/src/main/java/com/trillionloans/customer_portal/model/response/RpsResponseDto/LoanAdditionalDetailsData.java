package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanAdditionalDetailsData {

  private Boolean isFldg;
  private Double additionalGraceOnPrincipalPayment;
  private String lockInType;
  private Boolean isInterestTypeConvertable;
  private Double courseOnInterestPayment;
  private AdditionalInterestComputationType additionalInterestComputationType;
  private Double additionalGraceOnInterestPayment;
  private Double courseOnPrincipalPayment;
  private Double id;
  private Double residualValue;
  private Double loanId;
}
