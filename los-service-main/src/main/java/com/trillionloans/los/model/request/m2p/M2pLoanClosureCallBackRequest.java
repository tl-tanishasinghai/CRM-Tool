package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request body for loan closure callback")
public class M2pLoanClosureCallBackRequest {
  private String loanApplicationId;
  private String loanId;
  private String timeStamp;
  private String clientId;
  private String loanClosureStatus;
  private Integer disbursedAmount;
  private String productKey;

  public String getLoanClosureStatus() {
    return StringUtils.isNotEmpty(loanClosureStatus) ? loanClosureStatus : "";
  }
}
