package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message payload for business loan document OCR processing. Published to SQS when a business loan
 * document is uploaded; consumed asynchronously to run OCR and populate BusinessLoanDocument.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BusinessLoanOcrMessage {
  private String loanApplicationId;
  private String documentId;
  private String tag;
  private String productCode;
}
