package com.trillionloans.los.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction Response DTO")
public class TransactionResponseDTO {
  private String status;
  private String message;
  private String traceId;
  private TransactionDataDTO data;

  @Data
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TransactionDataDTO {
    private String status;
    private String systemExternalId;
    private String settlementDate;
    private Double amount;
  }
}
