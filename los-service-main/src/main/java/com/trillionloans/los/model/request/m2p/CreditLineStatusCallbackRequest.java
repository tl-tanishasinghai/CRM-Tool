package com.trillionloans.los.model.request.m2p;

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
public class CreditLineStatusCallbackRequest {

  private String status; // SUCCESS

  private String loanApplicationId;

  private String productkey;

  private String timeStamp;
}
