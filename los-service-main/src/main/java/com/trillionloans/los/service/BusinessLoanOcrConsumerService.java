package com.trillionloans.los.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.dto.BusinessLoanOcrMessage;
import com.trillionloans.los.model.entity.BusinessLoanDocument;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * SQS consumer for business loan document OCR. Polls the queue, fetches document from M2P, runs
 * OCR, saves extracted data to BusinessLoanDocument, and triggers evaluation.
 */
@Service
@Slf4j
@ConditionalOnProperty(
    name = "business-loan.enable-ocr-consumer",
    havingValue = "true",
    matchIfMissing = false)
public class BusinessLoanOcrConsumerService {

  private final String queueUrl;
  private final AmazonSQSAsync sqsAsyncClient;
  private final ObjectMapper objectMapper;
  private final M2PWrapperApi m2PWrapperApi;
  private final OcrService ocrService;
  private final com.trillionloans.los.repository.BusinessLoanDocumentRepository
      businessLoanDocumentRepository;
  private final BusinessLoanEvaluationService businessLoanEvaluationService;

  private static final int MAX_NUMBER_OF_MESSAGES = 10;
  private static final int WAIT_TIME_SECONDS = 20;

  public BusinessLoanOcrConsumerService(
      @Value("${sqs.queue.businessLoanOcr:}") String queueUrl,
      AmazonSQSAsync sqsAsyncClient,
      ObjectMapper objectMapper,
      M2PWrapperApi m2PWrapperApi,
      OcrService ocrService,
      com.trillionloans.los.repository.BusinessLoanDocumentRepository
          businessLoanDocumentRepository,
      BusinessLoanEvaluationService businessLoanEvaluationService) {
    this.queueUrl = queueUrl;
    this.sqsAsyncClient = sqsAsyncClient;
    this.objectMapper = objectMapper;
    this.m2PWrapperApi = m2PWrapperApi;
    this.ocrService = ocrService;
    this.businessLoanDocumentRepository = businessLoanDocumentRepository;
    this.businessLoanEvaluationService = businessLoanEvaluationService;
  }

  @Scheduled(fixedDelayString = "${business-loan.ocr-consumer-poll-interval-ms:5000}")
  public void pollAndProcessMessages() {
    if (queueUrl == null || queueUrl.isBlank()) {
      return;
    }

    ReceiveMessageRequest request =
        new ReceiveMessageRequest()
            .withQueueUrl(queueUrl)
            .withMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES)
            .withWaitTimeSeconds(WAIT_TIME_SECONDS)
            .withVisibilityTimeout(120);

    try {
      ReceiveMessageResult result = sqsAsyncClient.receiveMessage(request);
      List<Message> messages = result.getMessages();

      if (messages != null && !messages.isEmpty()) {
        log.info("[BUSINESS_LOAN_OCR] Received {} message(s) from SQS", messages.size());
        for (Message message : messages) {
          processMessage(message);
        }
      }
    } catch (Exception e) {
      log.error("[BUSINESS_LOAN_OCR] Error polling SQS queue", e);
    }
  }

  private void processMessage(Message sqsMessage) {
    String body = sqsMessage.getBody();
    String receiptHandle = sqsMessage.getReceiptHandle();

    try {
      BusinessLoanOcrMessage ocrMessage =
          objectMapper.readValue(body, BusinessLoanOcrMessage.class);

      processOcrMessage(ocrMessage)
          .subscribeOn(Schedulers.boundedElastic())
          .doOnSuccess(
              v -> {
                deleteMessage(receiptHandle);
                log.info(
                    "[BUSINESS_LOAN_OCR] Processed message for loanId: {}, tag: {}",
                    ocrMessage.getLoanApplicationId(),
                    ocrMessage.getTag());
              })
          .doOnError(
              error -> {
                log.error(
                    "[BUSINESS_LOAN_OCR] Failed to process message for loanId: {}, tag: {}. Message"
                        + " will retry after visibility timeout.",
                    ocrMessage.getLoanApplicationId(),
                    ocrMessage.getTag(),
                    error);
              })
          .subscribe();
    } catch (Exception e) {
      log.error(
          "[BUSINESS_LOAN_OCR] Failed to parse message body, deleting message. Body: {}", body, e);
      deleteMessage(receiptHandle);
    }
  }

  private Mono<Void> processOcrMessage(BusinessLoanOcrMessage ocrMessage) {
    String loanId = ocrMessage.getLoanApplicationId();
    String documentId = ocrMessage.getDocumentId();
    String tag = ocrMessage.getTag();

    return m2PWrapperApi
        .getDocumentByLoanIdAndDocumentId(loanId, documentId)
        .flatMap(
            documentBytes ->
                ocrService
                    .extractBusinessData(documentBytes, "application/pdf")
                    .flatMap(
                        ocrResult ->
                            upsertBusinessLoanDocument(
                                    loanId,
                                    tag,
                                    documentId,
                                    ocrResult.getBusinessName(),
                                    ocrResult.getBusinessAddress())
                                .then(
                                    Mono.fromRunnable(
                                        () ->
                                            businessLoanEvaluationService
                                                .markDocumentUploadedAndEvaluateAsync(
                                                    loanId, tag)))));
  }

  private Mono<BusinessLoanDocument> upsertBusinessLoanDocument(
      String loanApplicationId,
      String tag,
      String documentId,
      String businessName,
      String businessAddress) {
    return businessLoanDocumentRepository
        .findByLoanApplicationIdAndTag(loanApplicationId, tag)
        .flatMap(
            existing -> {
              existing.setDocumentNumber(documentId);
              existing.setBusinessName(businessName);
              existing.setBusinessAddress(businessAddress);
              existing.setUpdatedAt(LocalDateTime.now());
              return businessLoanDocumentRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  BusinessLoanDocument newDoc =
                      BusinessLoanDocument.builder()
                          .loanApplicationId(loanApplicationId)
                          .tag(tag)
                          .documentNumber(documentId)
                          .businessName(businessName)
                          .businessAddress(businessAddress)
                          .build();
                  return businessLoanDocumentRepository.save(newDoc);
                }));
  }

  private void deleteMessage(String receiptHandle) {
    try {
      sqsAsyncClient.deleteMessage(
          new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(receiptHandle));
    } catch (Exception e) {
      log.error("[BUSINESS_LOAN_OCR] Failed to delete message from SQS", e);
    }
  }
}
