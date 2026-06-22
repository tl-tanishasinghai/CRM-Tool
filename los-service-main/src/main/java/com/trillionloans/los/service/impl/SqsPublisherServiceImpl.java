package com.trillionloans.los.service.impl;

import static com.trillionloans.los.constant.StringConstants.EXTERNAL_ID;
import static com.trillionloans.los.constant.StringConstants.FILE_KEY;
import static com.trillionloans.los.constant.StringConstants.PARTNER_NAME;
import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.service.PublisherService;
import com.trillionloans.los.service.S3Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class SqsPublisherServiceImpl implements PublisherService {

  private final String queueUrl;
  private final AmazonSQSAsync sqsAsyncClient;
  private final S3Service s3Service;
  private final ObjectMapper objectMapper;
  private static final String STRING = "String";

  public SqsPublisherServiceImpl(
      @Value("${sqs.queue.riskDataSqs}") String queueUrl,
      AmazonSQSAsync sqsAsyncClient,
      S3Service s3Service,
      ObjectMapper objectMapper) {
    this.queueUrl = queueUrl;
    this.sqsAsyncClient = sqsAsyncClient;
    this.s3Service = s3Service;
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<SendMessageResult> sendToFifoQueue(
      String messagePayload,
      String messageGroupID,
      String externalId,
      String productCode,
      String partnerName) {
    return Mono.fromCallable(
            () -> {
              Map<String, MessageAttributeValue> additionalAttributes = new HashMap<>();
              String traceId = MDC.get(TRACE_ID);
              String dedupeId = UUID.randomUUID().toString().substring(0, 8);
              additionalAttributes.put(
                  PRODUCT_CODE,
                  new MessageAttributeValue().withDataType(STRING).withStringValue(productCode));
              additionalAttributes.put(
                  TRACE_ID,
                  new MessageAttributeValue().withDataType(STRING).withStringValue(traceId));
              additionalAttributes.put(
                  PARTNER_NAME,
                  new MessageAttributeValue().withDataType(STRING).withStringValue(partnerName));
              additionalAttributes.put(
                  EXTERNAL_ID,
                  new MessageAttributeValue().withDataType(STRING).withStringValue(externalId));
              additionalAttributes.put(
                  FILE_KEY,
                  new MessageAttributeValue().withDataType(STRING).withStringValue(traceId));

              String s3Key = s3Service.uploadJsonToS3(messagePayload, traceId, externalId);
              Map<String, String> s3Reference = new HashMap<>();
              s3Reference.put(FILE_KEY, s3Key);
              s3Reference.put(PRODUCT_CODE, productCode);
              s3Reference.put(PARTNER_NAME, partnerName);
              s3Reference.put(EXTERNAL_ID, externalId);
              s3Reference.put(TRACE_ID, traceId);
              String finalPayload = objectMapper.writeValueAsString(s3Reference);

              SendMessageRequest messageRequest =
                  new SendMessageRequest()
                      .withQueueUrl(queueUrl)
                      .withMessageBody(finalPayload)
                      .withMessageGroupId(messageGroupID)
                      .withMessageDeduplicationId(dedupeId)
                      .withMessageAttributes(additionalAttributes);

              return sqsAsyncClient.sendMessage(messageRequest);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(response -> log.info("Message pushed to SQS for loan id : {}", externalId))
        .doOnError(
            error ->
                log.error("Failed to push message to SQS for loan id : {}", externalId, error));
  }
}
