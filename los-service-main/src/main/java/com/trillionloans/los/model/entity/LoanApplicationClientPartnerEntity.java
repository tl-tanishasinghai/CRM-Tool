package com.trillionloans.los.model.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationClientPartnerEntity {
  private String loanApplicationId;
  private String clientId;
  private String partnerId;
}
