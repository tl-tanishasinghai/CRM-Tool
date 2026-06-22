package com.trillionloans.lms.model.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class M2pCreditLineForeClosureDTO {

  @Schema(description = "Mapped to paymentDetails.referenceNumber for M2P")
  private String notes;

  @Schema(description = "Transaction time in epoch milliseconds")
  private Long transactionTime;
}
