package com.trillionloans.los.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Service for extracting text/data from documents via OCR. Placeholder for phase 2 integration with
 * actual OCR provider (e.g. AWS Textract, Google Vision, etc.).
 */
public interface OcrService {

  /**
   * Extracts business-related data from a document. Returns business name and address when
   * available.
   *
   * @param documentBytes raw document bytes (e.g. PDF)
   * @param contentType content type (e.g. application/pdf)
   * @return extracted data or empty when OCR is not yet integrated
   */
  Mono<OcrResult> extractBusinessData(byte[] documentBytes, String contentType);

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  class OcrResult {
    private String businessName;
    private String businessAddress;
  }
}
