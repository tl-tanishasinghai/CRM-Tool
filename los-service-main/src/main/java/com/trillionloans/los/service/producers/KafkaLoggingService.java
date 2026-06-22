package com.trillionloans.los.service.producers;

import static com.trillionloans.los.constant.StringConstants.APPLICATION_NAME;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.SERVICE_NAME;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.util.JsonUtils.convertToJsonNode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.trillionloans.los.model.dto.internal.FileStorageDTO;
import com.trillionloans.los.model.dto.internal.LogEventsDTO;
import com.trillionloans.los.service.S3Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Service class responsible for logging events to Kafka. This class provides methods for creating
 * log event DTOs and sending them to a Kafka topic.
 */
@Service
@Slf4j
public class KafkaLoggingService {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final Gson gson;
  private final S3Service s3Service;

  @Value("${spring.kafka.topic.logging}")
  private String topicName;

  @Value("${s3.bucket.requestResponseLog}")
  private String bucketName;

  @Value("${aws.kms.requestResponseLog}")
  private String kmsKeyId;

  /**
   * Constructor for KafkaLoggingService.
   *
   * @param kafkaTemplate the Kafka template for sending messages
   * @param objectMapper the ObjectMapper for JSON serialization
   * @param gson the Gson instance for JSON handling
   * @param s3Service the service to interact with S3
   */
  public KafkaLoggingService(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      Gson gson,
      S3Service s3Service) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.gson = gson;
    this.s3Service = s3Service;
  }

  /**
   * Sends a log event to the specified Kafka topic.
   *
   * @param logEventDTO the log event data to send
   */
  public Mono<SendResult<String, String>> sendLogEvent(LogEventsDTO logEventDTO) {
    return Mono.fromCallable(() -> objectMapper.writeValueAsString(logEventDTO))
        .flatMap(
            logEventJson ->
                Mono.fromFuture(kafkaTemplate.send(topicName, logEventJson).toCompletableFuture())
                    .doOnError(
                        JsonProcessingException.class,
                        e -> {
                          log.error(
                              "error serializing logEventsDTO to json for traceId: {},"
                                  + " loggerHeader: {}, kafka topic: {}, requestId: {}",
                              logEventDTO.getTraceId(),
                              logEventDTO.getLoggerHeader(),
                              topicName,
                              logEventDTO.getRequestId());
                        })
                    .doOnError(
                        KafkaException.class,
                        e -> {
                          log.error(
                              "kafkaException while sending to kafka topic: {}, traceId: {},"
                                  + " loggerHeader: {}, requestId: {}, error : {}",
                              topicName,
                              logEventDTO.getTraceId(),
                              logEventDTO.getLoggerHeader(),
                              logEventDTO.getRequestId(),
                              e.getClass().getSimpleName());
                        })
                    .doOnError(
                        e -> {
                          log.error(
                              "unexpected error while sending to kafka topic: {}, requestId: {},"
                                  + " traceId: {}, loggerHeader: {}, error : {}",
                              topicName,
                              logEventDTO.getRequestId(),
                              logEventDTO.getTraceId(),
                              logEventDTO.getLoggerHeader(),
                              e.getClass().getSimpleName());
                        }));
  }

  /**
   * Creates a LogEventsDTO object with the provided data.
   *
   * @param uri the request URI
   * @param method the HTTP method
   * @param requestBody the request body
   * @param partnerCode the partner code
   * @return a populated LogEventsDTO object
   */
  public LogEventsDTO createLogEventDTO(
      String uri, HttpMethod method, Object requestBody, String partnerCode, String loggerHeader) {
    LogEventsDTO logEventsDTO = new LogEventsDTO();
    String traceId = MDC.get(TRACE_ID);
    String partnerId = MDC.get(PARTNER_ID);

    logEventsDTO.setRequestId(UUID.randomUUID().toString());
    logEventsDTO.setTraceId(traceId);
    logEventsDTO.setServiceName(SERVICE_NAME);
    logEventsDTO.setPartnerId(partnerId);
    logEventsDTO.setMethod(method.name());
    logEventsDTO.setUri(uri);
    logEventsDTO.setLoggerHeader(loggerHeader);
    logEventsDTO.setCreatedAt(LocalDateTime.now());
    logEventsDTO.setSourceApplication(APPLICATION_NAME);
    logEventsDTO.setDestinationApplication(partnerCode);

    if (requestBody instanceof MultiValueMap) {
      processMultipartRequest(
              (MultiValueMap<String, Object>) requestBody, partnerId, traceId, bucketName)
          .doOnSuccess(jsonString -> logEventsDTO.setRequestBody(convertToJsonNode(jsonString)))
          .subscribe();
    } else {
      logEventsDTO.setRequestBody(convertToJsonNode(requestBody));
    }

    return logEventsDTO;
  }

  /**
   * Processes a multipart request and converts it to a JSON string.
   *
   * @param multipartData the multipart data
   * @param partnerId the partner ID
   * @param traceId the trace ID
   * @param bucketName the S3 bucket name
   * @return a Mono containing the JSON string of the processed multipart request
   */
  private Mono<String> processMultipartRequest(
      MultiValueMap<String, Object> multipartData,
      String partnerId,
      String traceId,
      String bucketName) {

    Map<String, Object> formDataFields = new ConcurrentHashMap<>();
    Map<String, List<FileStorageDTO>> fileInfoFields = new ConcurrentHashMap<>();

    // Process all file uploads in parallel
    List<Mono<Tuple2<String, FileStorageDTO>>> fileUploadOperations = new ArrayList<>();

    multipartData.forEach(
        (key, values) ->
            values.forEach(
                value -> {
                  if (value instanceof FilePart filePart) {
                    fileUploadOperations.add(
                        processFilePart(filePart, partnerId, traceId, bucketName, key));
                  } else {
                    formDataFields.put(key, value.toString());
                  }
                }));

    return Flux.fromIterable(fileUploadOperations)
        .flatMap(mono -> mono)
        .collectList()
        .map(
            results -> {
              // Group file information by field name
              results.forEach(
                  tuple -> {
                    String fieldName = tuple.getT1();
                    FileStorageDTO fileInfo = tuple.getT2();
                    fileInfoFields.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(fileInfo);
                  });

              // Create the final JSON structure
              JsonObject combinedRequest = new JsonObject();
              JsonObject fileInfoJson = gson.toJsonTree(fileInfoFields).getAsJsonObject();
              JsonObject formFieldsJson = gson.toJsonTree(formDataFields).getAsJsonObject();

              combinedRequest.add("fileInfo", fileInfoJson);
              combinedRequest.add("formFields", formFieldsJson);

              return gson.toJson(combinedRequest);
            });
  }

  /**
   * Processes a file part and uploads it to S3.
   *
   * @param filePart the file part
   * @param partnerId the partner ID
   * @param traceId the trace ID
   * @param bucketName the S3 bucket name
   * @param fieldName the field name of the file part
   * @return a Mono containing a tuple of the field name and the FileStorageDTO
   */
  private Mono<Tuple2<String, FileStorageDTO>> processFilePart(
      FilePart filePart, String partnerId, String traceId, String bucketName, String fieldName) {

    return s3Service
        .uploadFile(filePart, partnerId, traceId, bucketName, kmsKeyId, "m2p")
        .map(
            url -> {
              FileStorageDTO fileInfo = new FileStorageDTO();
              fileInfo.setFileName(filePart.filename());
              fileInfo.setS3Url(url);
              fileInfo.setContentType(
                  Objects.requireNonNull(filePart.headers().getContentType()).toString());
              return Tuples.of(fieldName, fileInfo);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "failed to upload file: {}. error: {}", filePart.filename(), error.getMessage());
              return Mono.empty();
            });
  }

  /**
   * Logs a successful Kafka event with the given response body and status code.
   *
   * @param logEventDTO the log event data to update
   * @param responseBody the response body object
   * @param statusCode the HTTP status code for the response
   */
  public void logSuccessKafka(
      LogEventsDTO logEventDTO, Object responseBody, HttpStatusCode statusCode) {
    logEventDTO.setResponseBody(convertToJsonNode(responseBody));
    logEventDTO.setStatusCode(statusCode.toString());
    logEventDTO.setProcessingTimeMs(calculateProcessingTime(logEventDTO));
    sendLogEvent(logEventDTO)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            sendResult -> {
              log.info(
                  "successfully sent log event to kafka. traceId: {}, loggerHeader: {}, topic: {},"
                      + " partition: {}",
                  logEventDTO.getTraceId(),
                  logEventDTO.getLoggerHeader(),
                  sendResult.getRecordMetadata().topic(),
                  sendResult.getRecordMetadata().partition());
            },
            error -> {
              log.error(
                  "failed to send log event to kafka. traceId: {}, loggerHeader: {}, requestId: {},"
                      + " error: {}",
                  logEventDTO.getTraceId(),
                  logEventDTO.getLoggerHeader(),
                  logEventDTO.getRequestId(),
                  error.getMessage());
            });
  }

  /**
   * Logs an error Kafka event with the given error body and status code.
   *
   * @param logEventDTO the log event data to update
   * @param errorBody the error body object
   * @param statusCode the HTTP status code for the error
   */
  public void logErrorKafka(LogEventsDTO logEventDTO, Object errorBody, HttpStatusCode statusCode) {
    logEventDTO.setResponseBody(convertToJsonNode(errorBody));
    logEventDTO.setStatusCode(statusCode.toString());
    logEventDTO.setProcessingTimeMs(calculateProcessingTime(logEventDTO));
    sendLogEvent(logEventDTO)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            sendResult -> {
              log.info(
                  "successfully sent log event to kafka. traceId: {}, loggerHeader: {}, topic: {},"
                      + " partition: {}",
                  logEventDTO.getTraceId(),
                  logEventDTO.getLoggerHeader(),
                  sendResult.getRecordMetadata().topic(),
                  sendResult.getRecordMetadata().partition());
            },
            error -> {
              log.error(
                  "failed to send log event to kafka. traceId: {}, loggerHeader: {}, requestId: {},"
                      + " error: {}",
                  logEventDTO.getTraceId(),
                  logEventDTO.getLoggerHeader(),
                  logEventDTO.getRequestId(),
                  error.getMessage());
            });
  }

  /**
   * Calculates the processing time in milliseconds from the creation time of the log event.
   *
   * @param logEventDTO the log event to calculate processing time for
   * @return the processing time in milliseconds
   */
  private long calculateProcessingTime(LogEventsDTO logEventDTO) {
    return System.currentTimeMillis()
        - logEventDTO
            .getCreatedAt()
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
  }
}
