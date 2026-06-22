package com.trillionloans.los.service.producers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.trillionloans.los.config.EventsConfig;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.internal.EventBusDTO;
import com.trillionloans.los.model.dto.internal.EventContext;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class KafkaEventProducerService {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final EventsConfig eventsConfig;
  // If CTA type event is emitted for below event names
  // Then event type is changes from CTA to workflowStep
  private final Set<String> workflowCTAs =
      new HashSet<>(
          Arrays.asList(
              "LOAN_CTA",
              "CKYCR_CTA",
              "AXML_CTA",
              "TRIGGER_DISB",
              "AGREEMENT_CTA",
              "LOAN_BANK_ACCOUNT_CTA"));

  @Value("${spring.kafka.topic.event}")
  private String topicName;

  public KafkaEventProducerService(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      EventsConfig eventsConfig) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.eventsConfig = eventsConfig;
  }

  public void publishEvent(EventContext eventContext, Object responseBody, Object request) {
    if (!eventContext.isPublishEvent()) return;

    sendLogEvent(toDto(eventContext, responseBody, request))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            sendResult -> {
              log.info(
                  "successfully sent log event to kafka. topic: {}," + " partition: {}",
                  sendResult.getRecordMetadata().topic(),
                  sendResult.getRecordMetadata().partition());
            },
            error -> {
              log.error("failed to send log event to kafka." + " error: {}", error.getMessage());
            });
  }

  private Mono<SendResult<String, String>> sendLogEvent(EventBusDTO eventBusDTO) {
    return Mono.fromCallable(() -> objectMapper.writeValueAsString(eventBusDTO))
        .flatMap(
            logEventJson ->
                Mono.fromFuture(kafkaTemplate.send(topicName, logEventJson).toCompletableFuture())
                    .doOnError(
                        JsonProcessingException.class,
                        e -> {
                          log.error(
                              "error serializing logEventsDTO to json for ," + "  kafka topic: {}",
                              topicName);
                        })
                    .doOnError(
                        KafkaException.class,
                        e -> {
                          log.error(
                              "kafkaException while sending to kafka topic: {}, error : {}",
                              topicName,
                              e.getClass().getSimpleName());
                        })
                    .doOnError(
                        e -> {
                          log.error(
                              "unexpected error while sending to kafka topic: {}, error : {}",
                              topicName,
                              e.getClass().getSimpleName());
                        }));
  }

  private EventBusDTO toDto(EventContext context, Object responseBody, Object request) {
    enrich(context, responseBody, request);
    EventBusDTO dto = new EventBusDTO();
    dto.setClientId(context.getClientId());
    dto.setLoanAppId(context.getLoanId()); // loanId maps to loanAppId
    dto.setPartnerId(context.getPartnerId());

    String eventType = context.getEvent().getDefaultEventType();
    if (context.getMetadata() != null && context.getMetadata().containsKey("override_event_type")) {
      // override default eventType
      eventType = context.getMetadata().get("override_event_type").toString();
      context.getMetadata().remove("override_event_type");
    }

    if (context.getEvent().equals(Event.GENERIC_CTA)
        && context.getMetadata() != null
        && context.getMetadata().containsKey("cta-name")) {
      // special handling for CTAs
      String ctaName = context.getMetadata().get("cta-name").toString();
      dto.setEventName(ctaName);
      context.getMetadata().remove("cta-name");

      if (workflowCTAs.contains(ctaName)) {
        eventType = "WORKFLOW_STEP";
      }
    } else {
      dto.setEventName(context.getEvent().getEventName()); // enum to string
    }

    dto.setEvenType(StringUtils.isEmpty(eventType) ? "SYSTEM" : eventType);
    dto.setEventDate(Instant.now()); // event emitted time
    if (context.getMetadata() != null && !context.getMetadata().isEmpty()) {
      // Convert Object values to String if needed
      Map<String, String> metadata =
          context.getMetadata().entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      e -> e.getValue() != null ? e.getValue().toString() : null));
      dto.setMetadata(metadata);
    }
    return dto;
  }

  private void enrich(EventContext context, Object response, Object request) {
    String event = context.getEvent().name();
    var config = eventsConfig.getEvents().get(event);
    if (config == null) return;

    // Convert request/response into JSON
    String reqJson = request != null ? objectMapper.valueToTree(request).toString() : null;
    String resJson = response != null ? objectMapper.valueToTree(response).toString() : null;

    // Apply enrichment from both request and response
    applySourceConfig(context, config.getRequest(), reqJson, event);
    applySourceConfig(context, config.getResponse(), resJson, event);
  }

  private void applySourceConfig(
      EventContext context, EventsConfig.SourceConfig source, String json, String eventName) {
    if (source == null || json == null) return;

    try {
      if (source.getClientId() != null) {
        Object value = JsonPath.read(json, source.getClientId());
        if (value != null) context.setClientId(value.toString());
      }
    } catch (Exception e) {
      log.warn(
          "Failed to extract clientId for event {} from {}", eventName, source.getClientId(), e);
    }

    try {
      if (source.getLoanAppId() != null) {
        Object value = JsonPath.read(json, source.getLoanAppId());
        if (value != null) context.setLoanId(value.toString());
      }
    } catch (Exception e) {
      log.warn(
          "Failed to extract loanAppId for event {} from {}", eventName, source.getLoanAppId(), e);
    }

    if (source.getMetadata() != null) {
      Map<String, Object> metadata =
          context.getMetadata() != null ? new HashMap<>(context.getMetadata()) : new HashMap<>();
      for (Map.Entry<String, String> entry : source.getMetadata().entrySet()) {
        try {
          Object value = JsonPath.read(json, entry.getValue());
          metadata.put(entry.getKey(), value != null ? value.toString() : null);
        } catch (Exception e) {
          log.warn(
              "Failed to extract metadata {} for event {} with path {}",
              entry.getKey(),
              eventName,
              entry.getValue(),
              e);
        }
      }
      context.setMetadata(metadata);
    }
  }
}
