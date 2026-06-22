package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LoanExternalIdResponseDTO {

  @JsonProperty("clientId")
  public String clientId;

  @JsonProperty("loanApplicationId")
  public String loanApplicationId;

  @JsonProperty("loanApplicationReferenceNo")
  public String loanApplicationReferenceNo;
}
