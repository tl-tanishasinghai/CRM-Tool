package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.APPLICATION_NAME;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.SERVICE_NAME;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.util.JsonUtils.convertToJsonNode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.trillionloans.los.model.dto.internal.FileStorageDTO;
import com.trillionloans.los.model.dto.internal.LogEventsDTO;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service class for logging incoming requests and responses. This class intercepts HTTP requests
 * and responses to log relevant information before sending it to a Kafka topic.
 */
@Service
@Slf4j
public class IncomingRequestResponseLoggingService {

  private final KafkaLoggingService kafkaLoggingService;
  private final S3Service s3Service;
  private final Gson gson = new Gson();

  @Value("${s3.bucket.requestResponseLog}")
  private String bucketName;

  @Value("${aws.kms.requestResponseLog}")
  private String kmsKeyId;

  /**
   * Constructor for IncomingRequestResponseLoggingService.
   *
   * @param kafkaLoggingService the service to log events to Kafka
   * @param s3Service the service to interact with S3
   */
  public IncomingRequestResponseLoggingService(
      KafkaLoggingService kafkaLoggingService, S3Service s3Service) {
    this.kafkaLoggingService = kafkaLoggingService;
    this.s3Service = s3Service;
  }

  /**
   * Logs the incoming HTTP request and response.
   *
   * @param exchange the server web exchange containing the request and response
   * @param chain the filter chain to proceed with the request processing
   * @return a Mono representing the completion of the logging operation
   */
  public Mono<Void> logIncomingRequest(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    long startTime = System.currentTimeMillis();

    if (isMultipartRequest(request)) {
      return handleMultipartRequest(exchange, chain, startTime);
    }

    return DataBufferUtils.join(request.getBody())
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);
              return bytes;
            })
        .defaultIfEmpty(new byte[0])
        .flatMap(
            bytes -> {
              String requestBody = new String(bytes, StandardCharsets.UTF_8);
              ServerHttpRequestDecorator decoratedRequest = decorateRequest(exchange, bytes);
              return processRequest(
                  exchange.mutate().request(decoratedRequest).build(),
                  chain,
                  startTime,
                  requestBody);
            });
  }

  /**
   * Decorates the ServerHttpRequest to include the body.
   *
   * @param exchange the server web exchange
   * @param body the request body as a byte array
   * @return a decorated request with the body
   */
  private ServerHttpRequestDecorator decorateRequest(ServerWebExchange exchange, byte[] body) {
    return new ServerHttpRequestDecorator(exchange.getRequest()) {
      @Override
      public Flux<DataBuffer> getBody() {
        return Flux.just(exchange.getResponse().bufferFactory().wrap(body));
      }
    };
  }

  /**
   * Processes the request and creates a response decorator to log the response data.
   *
   * @param exchange the server web exchange
   * @param chain the filter chain
   * @param startTime the start time of the request
   * @param requestBody the request body as a string
   * @return a Mono representing the completion of the request processing
   */
  private Mono<Void> processRequest(
      ServerWebExchange exchange, WebFilterChain chain, long startTime, String requestBody) {
    ServerHttpResponseDecorator decoratedResponse =
        createResponseDecorator(exchange, startTime, requestBody);
    return chain.filter(exchange.mutate().response(decoratedResponse).build());
  }

  /**
   * Checks if the request is a multipart request.
   *
   * @param request the incoming HTTP request
   * @return true if the request is multipart, false otherwise
   */
  private boolean isMultipartRequest(ServerHttpRequest request) {
    MediaType contentType = request.getHeaders().getContentType();
    return contentType != null && contentType.includes(MediaType.MULTIPART_FORM_DATA);
  }

  /**
   * Handles multipart requests.
   *
   * @param exchange the server web exchange
   * @param chain the filter chain
   * @param startTime the start time of the request
   * @return a Mono representing the completion of the multipart request handling
   */
  private Mono<Void> handleMultipartRequest(
      ServerWebExchange exchange, WebFilterChain chain, long startTime) {
    return exchange
        .getMultipartData()
        .flatMap(
            multipartData -> {
              List<FileStorageDTO> requestFileInfoList = new ArrayList<>();
              JsonObject formDataFields = new JsonObject();

              return processMultipartParts(multipartData, requestFileInfoList, formDataFields)
                  .then(
                      processMultipartRequest(
                          exchange, chain, startTime, requestFileInfoList, formDataFields));
            });
  }

  /**
   * Processes multipart parts.
   *
   * @param multipartData the multipart data
   * @param requestFileInfoList the list to store file information
   * @param formDataFields the JSON object to store form data fields
   * @return a Flux representing the completion of the multipart parts processing
   */
  private Flux<Void> processMultipartParts(
      MultiValueMap<String, Part> multipartData,
      List<FileStorageDTO> requestFileInfoList,
      JsonObject formDataFields) {
    return Flux.fromIterable(multipartData.entrySet())
        .flatMap(
            entry ->
                Flux.fromIterable(entry.getValue())
                    .flatMap(
                        part -> {
                          if (part instanceof FilePart filePart) {
                            return handleFilePart(filePart, requestFileInfoList);
                          }
                          return handleFormField(part, entry.getKey(), formDataFields);
                        }));
  }

  /**
   * Handles file parts in a multipart request.
   *
   * @param filePart the file part
   * @param requestFileInfoList the list to store file information
   * @return a Mono representing the completion of the file part handling
   */
  private Mono<Void> handleFilePart(FilePart filePart, List<FileStorageDTO> requestFileInfoList) {
    String partnerId = MDC.get(PARTNER_ID);
    String traceId = MDC.get(TRACE_ID);
    FileStorageDTO fileInfo = new FileStorageDTO();

    return s3Service
        .uploadFile(filePart, partnerId, traceId, bucketName, kmsKeyId, APPLICATION_NAME)
        .doOnSuccess(
            url -> {
              fileInfo.setFileName(filePart.filename());
              fileInfo.setS3Url(url);
              fileInfo.setContentType(String.valueOf(filePart.headers().getContentType()));
              requestFileInfoList.add(fileInfo);
            })
        .then();
  }

  /**
   * Handles form fields in a multipart request.
   *
   * @param part the form field part
   * @param fieldName the name of the form field
   * @param formDataFields the JSON object to store form data fields
   * @return a Mono representing the completion of the form field handling
   */
  private Mono<Void> handleFormField(Part part, String fieldName, JsonObject formDataFields) {
    return DataBufferUtils.join(part.content())
        .mapNotNull(
            dataBuffer -> {
              try {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                String value = new String(bytes, StandardCharsets.UTF_8).trim();
                formDataFields.addProperty(fieldName, value.isEmpty() ? "" : value);
              } catch (Exception e) {
                log.warn("Error processing form field {}: {}", fieldName, e.getMessage());
                formDataFields.addProperty(fieldName, "");
              }
              return dataBuffer;
            })
        .then();
  }

  /**
   * Processes the multipart request and logs the response.
   *
   * @param exchange the server web exchange
   * @param chain the filter chain
   * @param startTime the start time of the request
   * @param fileInfoList the list of file information
   * @param formFields the JSON object containing form fields
   * @return a Mono representing the completion of the multipart request processing
   */
  private Mono<Void> processMultipartRequest(
      ServerWebExchange exchange,
      WebFilterChain chain,
      long startTime,
      List<FileStorageDTO> fileInfoList,
      JsonObject formFields) {
    String loggerHeader = exchange.getRequest().getHeaders().getFirst("loggerheader");
    ServerHttpResponseDecorator responseDecorator =
        new ServerHttpResponseDecorator(exchange.getResponse()) {
          @Override
          public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
            return super.writeWith(
                Flux.from(body)
                    .collectList()
                    .flatMap(
                        dataBuffers -> {
                          byte[] allBytes = combineDataBuffers(dataBuffers);
                          String responseBody = new String(allBytes, StandardCharsets.UTF_8);
                          long endTime = System.currentTimeMillis();

                          return Mono.fromRunnable(
                                  () -> {
                                    try {
                                      // Create combined request body with both file info list and
                                      // form fields
                                      JsonObject combinedRequest = new JsonObject();
                                      combinedRequest.add(
                                          "fileInfo", gson.toJsonTree(fileInfoList));
                                      combinedRequest.add("formFields", formFields);

                                      LogEventsDTO logEvent =
                                          createLogEvent(
                                              exchange.getRequest(),
                                              getDelegate(),
                                              startTime,
                                              endTime,
                                              gson.toJson(combinedRequest),
                                              responseBody,
                                              loggerHeader);
                                      kafkaLoggingService
                                          .sendLogEvent(logEvent)
                                          .subscribeOn(Schedulers.boundedElastic())
                                          .subscribe(
                                              sendResult ->
                                                  log.info(
                                                      "successfully sent log event to kafka."
                                                          + " traceId: {}, topic: {}, partition:"
                                                          + " {}",
                                                      logEvent.getTraceId(),
                                                      sendResult.getRecordMetadata().topic(),
                                                      sendResult.getRecordMetadata().partition()),
                                              error ->
                                                  log.error(
                                                      "failed to send log event to kafka. traceId:"
                                                          + " {}, requestId: {}, error: {}",
                                                      logEvent.getTraceId(),
                                                      logEvent.getRequestId(),
                                                      error.getMessage()));
                                    } catch (Exception e) {
                                      log.error(
                                          "Error processing multipart request logging."
                                              + " loggerHeader: {}, error: {}",
                                          loggerHeader,
                                          e.getMessage(),
                                          e);
                                    }
                                  })
                              .subscribeOn(Schedulers.boundedElastic())
                              .then(
                                  Mono.just(exchange.getResponse().bufferFactory().wrap(allBytes)));
                        }));
          }
        };

    return chain.filter(exchange.mutate().response(responseDecorator).build());
  }

  /**
   * Creates a decorator for the response to log response data.
   *
   * @param exchange the server web exchange
   * @param startTime the start time of the request
   * @param requestBody the request body as a string
   * @return a decorated response that logs its body
   */
  private ServerHttpResponseDecorator createResponseDecorator(
      ServerWebExchange exchange, long startTime, String requestBody) {
    String loggerHeader = exchange.getRequest().getHeaders().getFirst("loggerheader");
    return new ServerHttpResponseDecorator(exchange.getResponse()) {
      @Override
      public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
        return super.writeWith(
            Flux.from(body)
                .collectList()
                .flatMap(
                    dataBuffers -> {
                      byte[] allBytes = combineDataBuffers(dataBuffers);

                      Mono<String> processedResponseBody;
                      if (isBinaryResponse(getDelegate())) {
                        processedResponseBody = handleBinaryResponse(allBytes, getDelegate());
                      } else {
                        processedResponseBody =
                            Mono.just(new String(allBytes, StandardCharsets.UTF_8));
                      }

                      return processedResponseBody.flatMap(
                          responseBody -> {
                            long endTime = System.currentTimeMillis();

                            return Mono.fromRunnable(
                                    () -> {
                                      try {
                                        LogEventsDTO logEvent =
                                            createLogEvent(
                                                exchange.getRequest(),
                                                getDelegate(),
                                                startTime,
                                                endTime,
                                                requestBody,
                                                responseBody,
                                                loggerHeader);
                                        kafkaLoggingService
                                            .sendLogEvent(logEvent)
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .subscribe(
                                                sendResult ->
                                                    log.info(
                                                        "successfully sent log event to kafka."
                                                            + " traceId: {}, topic: {}, partition:"
                                                            + " {}",
                                                        logEvent.getTraceId(),
                                                        sendResult.getRecordMetadata().topic(),
                                                        sendResult.getRecordMetadata().partition()),
                                                error ->
                                                    log.error(
                                                        "failed to send log event to kafka."
                                                            + " traceId: {}, requestId: {}, error:"
                                                            + " {}",
                                                        logEvent.getTraceId(),
                                                        logEvent.getRequestId(),
                                                        error.getMessage()));
                                      } catch (Exception e) {
                                        log.error(
                                            "Error creating or sending log event. loggerHeader: {},"
                                                + " error: {}",
                                            loggerHeader,
                                            e.getMessage());
                                      }
                                    })
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(
                                    Mono.just(
                                        exchange.getResponse().bufferFactory().wrap(allBytes)));
                          });
                    })
                .onErrorResume(
                    e -> {
                      log.error(
                          "Error processing response body, loggerHeader: {}, error: {}",
                          loggerHeader,
                          e.getMessage());
                      return Mono.error(e);
                    }));
      }
    };
  }

  /**
   * Combines multiple DataBuffers into a single byte array.
   *
   * @param dataBuffers the list of DataBuffers to combine
   * @return a byte array containing all combined data
   */
  private byte[] combineDataBuffers(List<? extends DataBuffer> dataBuffers) {
    int totalBytes = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
    byte[] result = new byte[totalBytes];
    int offset = 0;
    for (DataBuffer dataBuffer : dataBuffers) {
      int length = dataBuffer.readableByteCount();
      dataBuffer.read(result, offset, length);
      offset += length;
      DataBufferUtils.release(dataBuffer);
    }
    return result;
  }

  /**
   * Creates a LogEventsDTO object with the provided data.
   *
   * @param request the incoming HTTP request
   * @param response the HTTP response
   * @param startTime the start time of the request
   * @param endTime the end time of the request
   * @param requestBody the request body as a string
   * @param responseBody the response body as a string
   * @return a populated LogEventsDTO object
   */
  private LogEventsDTO createLogEvent(
      ServerHttpRequest request,
      ServerHttpResponse response,
      long startTime,
      long endTime,
      String requestBody,
      String responseBody,
      String loggerHeader) {
    return LogEventsDTO.builder()
        .requestId(UUID.randomUUID().toString())
        .traceId(MDC.get(TRACE_ID))
        .serviceName(SERVICE_NAME)
        .method(request.getMethod().toString())
        .uri(request.getURI().toString())
        .loggerHeader(loggerHeader)
        .statusCode(Objects.requireNonNull(response.getStatusCode()).toString())
        .processingTimeMs(endTime - startTime)
        .createdAt(LocalDateTime.now())
        .sourceApplication("client")
        .destinationApplication(APPLICATION_NAME)
        .requestBody(convertToJsonNode(requestBody))
        .responseBody(convertToJsonNode(responseBody))
        .partnerId(
            request.getHeaders().getFirst(PARTNER_ID) == null
                ? "1001"
                : request.getHeaders().getFirst(PARTNER_ID))
        .build();
  }

  /**
   * Checks if the response is binary.
   *
   * @param response the HTTP response
   * @return true if the response is binary, false otherwise
   */
  private boolean isBinaryResponse(ServerHttpResponse response) {
    MediaType contentType = response.getHeaders().getContentType();
    return contentType != null
        && (contentType.includes(MediaType.APPLICATION_PDF)
            || contentType.includes(MediaType.APPLICATION_OCTET_STREAM)
            || contentType.includes(MediaType.IMAGE_JPEG)
            || contentType.includes(MediaType.IMAGE_PNG));
  }

  /**
   * Handles binary responses by uploading the content to S3 and returning the URL.
   *
   * @param responseBytes the response bytes
   * @param response the HTTP response
   * @return a Mono containing the JSON string of the binary response
   */
  private Mono<String> handleBinaryResponse(byte[] responseBytes, ServerHttpResponse response) {
    try {
      // Upload binary content to S3 and return the URL
      String fileName = UUID.randomUUID().toString();
      String contentType =
          Objects.requireNonNull(response.getHeaders().getContentType()).toString();
      String partnerId = MDC.get(PARTNER_ID);
      String traceId = MDC.get(TRACE_ID);

      return s3Service
          .uploadContent(
              responseBytes,
              fileName,
              contentType,
              partnerId,
              traceId,
              bucketName,
              kmsKeyId,
              APPLICATION_NAME)
          .map(
              url -> {
                JsonObject binaryResponse = new JsonObject();
                binaryResponse.addProperty("type", "binary");
                binaryResponse.addProperty("contentType", contentType);
                binaryResponse.addProperty("size", responseBytes.length);
                binaryResponse.addProperty("s3Url", url);
                return gson.toJson(binaryResponse);
              });
    } catch (IllegalArgumentException | ClassCastException e) {
      log.info("Invalid binary response");
      return Mono.just("{\"error\":\"Invalid binary response\"}");
    }
  }
}
