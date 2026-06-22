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
public class DisbursalAmountResponse {
  private BigDecimal grossDisbursalAmount;
  private BigDecimal netDisbursalAmount;
}
