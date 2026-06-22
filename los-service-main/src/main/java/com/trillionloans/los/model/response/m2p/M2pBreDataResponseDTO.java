package com.trillionloans.los.model.response.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class M2pBreDataResponseDTO {
  private String resourceIdentifier;
  private int entityId;
  private boolean rollbackTransaction;
  private AdditionalResponseData additionalResponseData;

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  public static class AdditionalResponseData {
    private M2pBreOutput output;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class M2pBreOutput {
      private boolean success;
      private String message;
      private double roi;
      private double loanAmount;
      private int leadId;
    }
  }
}
