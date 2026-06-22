package com.trillionloans.los.model.response.m2p;

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
public class RequestedLoanDetailsResponse {

  private Long externalId;
  private String clientId;
  private BigDecimal requestedLoanAmount;
  private Integer tenure;
  private Integer tenureEnum;
  private String panNumber;
  private String dob;
  private String pincode;
}
