package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanApplicationIdResponse {

  private String loanApplicationId;
}
