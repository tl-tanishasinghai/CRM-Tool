package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankVerificationResponseDTO {
  private String bankVerificationStatus;
  private DataDTO data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DataDTO {
    private String clientId;
    private String bankAccountId;
    private PennyDropDTO pennyDrop;
    private NameMatchPercentageDTO nameMatchPercentage;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PennyDropDTO {
    private String bankResponse;
    private Boolean isValid;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NameMatchPercentageDTO {
    private Double score;
    private Boolean isValid;
  }
}
