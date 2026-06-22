package com.trillionloans.los.service;

import com.amazonaws.services.sqs.model.SendMessageResult;
import reactor.core.publisher.Mono;

public interface PublisherService {
  Mono<SendMessageResult> sendToFifoQueue(
      final String messagePayload,
      final String messageGroupID,
      final String externalId,
      final String productCode,
      final String partnerName);
}
