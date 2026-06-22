package com.trillionloans.los.model.request.m2p;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Request body for marking loan as disbursed")
public class M2pLoanDisburseRequestDTO {
  private String actualDisbursementDate;
  private String paymentTypeId;
  private String dateFormat;
  private String receiptNumber;
  private Integer bankAccountDetailId;
}
