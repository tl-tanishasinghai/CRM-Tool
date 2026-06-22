package com.trillionloans.los.model.response.m2p;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class M2PDrawdownResponse {
  private String transactionId;
  private String emiConversionId;
  private String accountNumber;
  private BigDecimal amount;
  private Long transactionTime;
  private String processingStatus;
}
