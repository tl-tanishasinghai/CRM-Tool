package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "auto disbursal callback request")
public class AutoDisbursalCallbackRequest {
  private String status;
  private String systemExternalId;
  private Double amount;
  private String settlementDate;
  String transferUtr;
}
