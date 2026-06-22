package com.trillionloans.lms.model.response;

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
public class ChargeDetailsDTO {
  private String externalId;
  private Double amount;
  private Double amountPaid;
  private String chargeType;
  private String chargeLeviedTimeStamp;
}
