package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanClientPartnerMapResponse {
  private Integer loanApplicationId;
  private Integer clientId;
  private Integer lanId;
  private String lineId;
  private Integer partnerId;
}
