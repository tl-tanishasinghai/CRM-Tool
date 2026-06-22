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
@Schema(description = "Request body for callbacks of loan Approval")
public class M2pLoanApprovalCallBackRequest {
  private String loanApplicationId;
  private String timeStamp;
  private String clientId;
  private String loanApprovalStatus;
  private String losProductKey;
}
