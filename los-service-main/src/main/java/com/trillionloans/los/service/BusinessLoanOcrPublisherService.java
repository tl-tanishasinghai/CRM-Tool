package com.trillionloans.los.service;

import com.trillionloans.los.model.dto.BusinessLoanOcrMessage;
import reactor.core.publisher.Mono;

/** Publishes business loan document OCR messages to SQS for async processing. */
public interface BusinessLoanOcrPublisherService {

  /**
   * Publishes a message to the business loan OCR queue. Processing (OCR extraction and evaluation)
   * will happen asynchronously via the consumer.
   *
   * @return Mono&lt;Boolean&gt; true if published, false if queue not configured (skipped)
   */
  Mono<Boolean> publishOcrMessage(BusinessLoanOcrMessage message);
}
