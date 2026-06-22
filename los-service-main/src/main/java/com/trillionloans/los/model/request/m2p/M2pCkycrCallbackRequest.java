package com.trillionloans.los.model.request.m2p;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Request body for Ckycr callback")
public class M2pCkycrCallbackRequest {
  private Integer loanId;

  private String productCode;
}
