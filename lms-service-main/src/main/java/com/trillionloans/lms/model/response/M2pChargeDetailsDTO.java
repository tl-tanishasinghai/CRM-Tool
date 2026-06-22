package com.trillionloans.lms.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class M2pChargeDetailsDTO {

  private String externalId;
  private Double amount;
  private Double amountPaid;
  private ChargeTimeTypeDTO chargeTimeType;

  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ChargeTimeTypeDTO {
    private Integer id;
  }
}
