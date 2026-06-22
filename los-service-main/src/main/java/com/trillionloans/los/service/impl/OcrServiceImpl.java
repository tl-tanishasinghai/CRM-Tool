package com.trillionloans.los.service.impl;

import com.trillionloans.los.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Placeholder OCR implementation. Phase 2: integrate with actual OCR provider (e.g. AWS Textract,
 * Google Vision) to extract business name and address from documents.
 */
@Service
@Slf4j
public class OcrServiceImpl implements OcrService {

  @Override
  public Mono<OcrResult> extractBusinessData(byte[] documentBytes, String contentType) {
    log.debug(
        "[OCR] Placeholder - OCR not yet integrated. Document size: {} bytes, contentType: {}",
        documentBytes != null ? documentBytes.length : 0,
        contentType);
    // Phase 2: call actual OCR API and map response to OcrResult
    return Mono.just(OcrResult.builder().businessName(null).businessAddress(null).build());
  }
}
