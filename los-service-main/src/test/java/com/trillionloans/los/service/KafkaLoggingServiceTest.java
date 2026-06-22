package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trillionloans.los.model.dto.internal.LogEventsDTO;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class KafkaLoggingServiceTest {

  @Mock private KafkaTemplate<String, String> kafkaTemplate;
  @Mock private ObjectMapper objectMapper;
  @Mock private Gson gson;
  @Mock private S3Service s3Service;

  @InjectMocks private KafkaLoggingService kafkaLoggingService;

  private static final String TOPIC_NAME = "test-topic";

  @Test
  void testSendLogEvent_success() throws JsonProcessingException {
    ReflectionTestUtils.setField(kafkaLoggingService, "topicName", TOPIC_NAME);

    // Setup mock LogEventsDTO and JSON specific to this test
    LogEventsDTO mockLogEventDTO = new LogEventsDTO();
    mockLogEventDTO.setRequestId(UUID.randomUUID().toString());
    String mockLogEventJson = "{\"mock\":\"json\"}";

    // Stub the ObjectMapper behavior
    when(objectMapper.writeValueAsString(any(LogEventsDTO.class))).thenReturn(mockLogEventJson);

    // Stub the KafkaTemplate send behavior
    SendResult<String, String> sendResult =
        new SendResult<>(
            new ProducerRecord<>(TOPIC_NAME, mockLogEventJson),
            new RecordMetadata(new TopicPartition(TOPIC_NAME, 1), 0, 0, 0, 0, 0));
    CompletableFuture<SendResult<String, String>> future =
        CompletableFuture.completedFuture(sendResult);
    when(kafkaTemplate.send(eq(TOPIC_NAME), eq(mockLogEventJson))).thenReturn(future);

    kafkaLoggingService.sendLogEvent(mockLogEventDTO).block();

    verify(kafkaTemplate).send(TOPIC_NAME, mockLogEventJson);
  }

  @Test
  void testSendLogEvent_withJsonProcessingException() throws JsonProcessingException {
    ReflectionTestUtils.setField(kafkaLoggingService, "topicName", TOPIC_NAME);

    LogEventsDTO mockLogEventDTO = new LogEventsDTO();
    // Stub ObjectMapper to throw an exception
    when(objectMapper.writeValueAsString(any(LogEventsDTO.class)))
        .thenThrow(new JsonProcessingException("Serialization error") {});

    // Use StepVerifier to test the error scenario in a non-blocking, reactive way.
    StepVerifier.create(kafkaLoggingService.sendLogEvent(mockLogEventDTO))
        .expectErrorMatches(
            throwable ->
                throwable instanceof JsonProcessingException
                    && throwable.getMessage().contains("Serialization error"))
        .verify();

    // Verify that the Kafka template's send method was never called.
    verify(kafkaTemplate, never()).send(anyString(), anyString());
  }

  @Test
  void testCreateLogEventDTO_basicRequestBody() {
    String uri = "/api/v1/test";
    HttpMethod method = HttpMethod.POST;
    Object requestBody = Map.of("key", "value");
    String partnerCode = "partner-x";
    String loggerHeader = "logger-header";

    MDC.put("traceId", "test-trace-id");
    MDC.put("partnerId", "partner-123");

    LogEventsDTO dto =
        kafkaLoggingService.createLogEventDTO(uri, method, requestBody, partnerCode, loggerHeader);

    assertNotNull(dto);
    assertEquals("test-trace-id", dto.getTraceId());
    assertEquals("partner-123", dto.getPartnerId());
    assertEquals(uri, dto.getUri());
    assertEquals(HttpMethod.POST.name(), dto.getMethod());
    assertEquals(partnerCode, dto.getDestinationApplication());
    assertEquals(loggerHeader, dto.getLoggerHeader());

    MDC.clear();
  }

  @Test
  void testSendLogEvent_withStepVerifier() throws JsonProcessingException {
    ReflectionTestUtils.setField(kafkaLoggingService, "topicName", TOPIC_NAME);

    LogEventsDTO mockLogEventDTO = new LogEventsDTO();
    String mockLogEventJson = "{\"mock\":\"json\"}";

    when(objectMapper.writeValueAsString(any(LogEventsDTO.class))).thenReturn(mockLogEventJson);
    SendResult<String, String> sendResult =
        new SendResult<>(
            new ProducerRecord<>(TOPIC_NAME, mockLogEventJson),
            new RecordMetadata(new TopicPartition(TOPIC_NAME, 1), 0, 0, 0, 0, 0));
    CompletableFuture<SendResult<String, String>> future =
        CompletableFuture.completedFuture(sendResult);
    when(kafkaTemplate.send(eq(TOPIC_NAME), eq(mockLogEventJson))).thenReturn(future);

    Mono<SendResult<String, String>> resultMono = kafkaLoggingService.sendLogEvent(mockLogEventDTO);

    StepVerifier.create(resultMono)
        .expectNextMatches(result -> result.getRecordMetadata().topic().equals(TOPIC_NAME))
        .verifyComplete();
  }
}
