package com.trillionloans.los.service.producers;

import static com.trillionloans.los.constant.StringConstants.SERVICE_NAME;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.model.dto.internal.LoanFunnelDTO;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class KafkaFunnelLoggingService {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${spring.kafka.topic.loan-funnel}")
  private String topicName;

  public KafkaFunnelLoggingService(
      KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  public void sendLogEvent(
      String leadId,
      String clientId,
      LoanFunnelDTO.Stage stage,
      LoanFunnelDTO.SubStage subStage,
      String status,
      String rejectionReason,
      String failReason) {
    LoanFunnelDTO loanFunnelDTO = new LoanFunnelDTO();

    loanFunnelDTO.setRequestId(UUID.randomUUID().toString());
    loanFunnelDTO.setLeadId(leadId);
    loanFunnelDTO.setClientId(clientId);
    loanFunnelDTO.setStage(stage);
    loanFunnelDTO.setSubStage(subStage);
    loanFunnelDTO.setStatus(status);
    loanFunnelDTO.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
    loanFunnelDTO.setServiceName(SERVICE_NAME);
    loanFunnelDTO.setTraceId(MDC.get(TRACE_ID));
    loanFunnelDTO.setRejectionReason(rejectionReason);
    loanFunnelDTO.setFailReason(failReason);

    try {
      String logEventJson = objectMapper.writeValueAsString(loanFunnelDTO);
      kafkaTemplate
          .send(topicName, logEventJson)
          .whenComplete(
              (result, ex) -> {
                if (ex == null) {
                  log.info(
                      "successfully sent log event to kafka. traceId: {}, subStage: {}, topic: {},"
                          + " partition: {}",
                      loanFunnelDTO.getTraceId(),
                      loanFunnelDTO.getSubStage(),
                      result.getRecordMetadata().topic(),
                      result.getRecordMetadata().partition());
                } else {
                  log.error(
                      "failed to send log event to kafka. traceId: {}, subStage: {}, requestId: {},"
                          + " error: {}",
                      loanFunnelDTO.getTraceId(),
                      loanFunnelDTO.getSubStage(),
                      loanFunnelDTO.getRequestId(),
                      ex.getMessage());
                }
              });
    } catch (JsonProcessingException e) {
      log.error(
          "Error serializing loanFunnelDTO to JSON for Kafka topic: {}, requestId: {}, traceId: {}",
          topicName,
          loanFunnelDTO.getRequestId(),
          loanFunnelDTO.getTraceId(),
          e);
    } catch (KafkaException e) {
      log.error(
          "KafkaException while sending to topic: {}, requestId: {}, traceId: {}, message: {}",
          topicName,
          loanFunnelDTO.getRequestId(),
          loanFunnelDTO.getTraceId(),
          e.getClass().getSimpleName());
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.error(
          "Invalid argument/state while sending to Kafka: {}, requestId: {}, traceId: {}, message:"
              + " {}",
          topicName,
          loanFunnelDTO.getRequestId(),
          loanFunnelDTO.getTraceId(),
          e.getClass().getSimpleName());
    }
  }

  public void logMandateRegistrationAsync(
      String clientId,
      String loanId,
      LoanFunnelDTO.SubStage subStage,
      String status,
      String rejectionReason,
      String failReason) {
    Mono.fromRunnable(
            () ->
                sendLogEvent(
                    loanId,
                    clientId,
                    LoanFunnelDTO.Stage.MANDATE_REGISTRATION,
                    subStage,
                    status,
                    rejectionReason,
                    failReason))
        .subscribeOn(Schedulers.parallel())
        .contextWrite(context -> context.put(TRACE_ID, MDC.get(TRACE_ID)))
        .subscribe(
            null,
            error ->
                log.error(
                    "Error in async mandate registration logging for clientId: {}, loanId: {},"
                        + " subStage: {}",
                    clientId,
                    loanId,
                    subStage,
                    error));
  }
}
