package com.trillionloans.los.service.impl;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.model.dto.BusinessLoanOcrMessage;
import com.trillionloans.los.service.BusinessLoanOcrPublisherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class BusinessLoanOcrPublisherServiceImpl implements BusinessLoanOcrPublisherService {

  private final String queueUrl;
  private final AmazonSQSAsync sqsAsyncClient;
  private final ObjectMapper objectMapper;

  public BusinessLoanOcrPublisherServiceImpl(
      @Value("${sqs.queue.businessLoanOcr:}") String queueUrl,
      AmazonSQSAsync sqsAsyncClient,
      ObjectMapper objectMapper) {
    this.queueUrl = queueUrl;
    this.sqsAsyncClient = sqsAsyncClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Boolean> publishOcrMessage(BusinessLoanOcrMessage message) {
    if (queueUrl == null || queueUrl.isBlank()) {
      log.warn(
          "[BUSINESS_LOAN_OCR] SQS queue URL not configured (sqs.queue.businessLoanOcr), skipping"
              + " publish for loanId: {}, tag: {}",
          message.getLoanApplicationId(),
          message.getTag());
      return Mono.just(false);
    }

    return Mono.fromCallable(
            () -> {
              String payload;
              try {
                payload = objectMapper.writeValueAsString(message);
              } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize BusinessLoanOcrMessage", e);
              }

              SendMessageRequest request =
                  new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(payload);

              return sqsAsyncClient.sendMessage(request);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(
            result ->
                log.info(
                    "[BUSINESS_LOAN_OCR] Published OCR message for loanId: {}, tag: {}, documentId:"
                        + " {}",
                    message.getLoanApplicationId(),
                    message.getTag(),
                    message.getDocumentId()))
        .doOnError(
            error ->
                log.error(
                    "[BUSINESS_LOAN_OCR] Failed to publish OCR message for loanId: {}, tag: {}",
                    message.getLoanApplicationId(),
                    message.getTag(),
                    error))
        .map(r -> true)
        .onErrorReturn(false);
  }
}
