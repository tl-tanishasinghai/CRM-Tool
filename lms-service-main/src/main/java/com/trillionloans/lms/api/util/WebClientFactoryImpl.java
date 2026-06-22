package com.trillionloans.lms.api.util;

import static com.trillionloans.lms.constant.StringConstants.LOGGING_ERROR_RESPONSE;
import static com.trillionloans.lms.constant.StringConstants.LOGGING_RESPONSE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.trillionloans.lms.exception.ClientSideException;
import com.trillionloans.lms.exception.ServerErrorException;
import com.trillionloans.lms.model.dto.internal.LogEventsDTO;
import com.trillionloans.lms.model.dto.internal.WebClientParameters;
import com.trillionloans.lms.service.KafkaLoggingService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

/**
 * Implementation of the WebClientFactory interface for creating reactive WebClient instances.
 *
 * @author sofiyan
 */
@Slf4j
public class WebClientFactoryImpl implements WebClientFactory {

  private final WebClient webClient;
  private final String partnerName;
  private final Gson gson;
  private final Environment env;
  private final KafkaLoggingService kafkaLoggingService;
  private String baseUrl;

  private static final String SERVER_ERROR = "server error";
  private static final String CLIENT_ERROR = "client error";
  public static final String LOGGING_LITERAL = "[{}] request to {}: {}";
  public static final String LOGGING_LITERAL_URI = "[{}] request to {} (uri): {}";
  public static final String LOGGING_LITERAL_RETRY = "[{}] retrying request {}, retry count: {}";

  public WebClientFactoryImpl(
      String baseUrl,
      String partnerName,
      Environment env,
      KafkaLoggingService kafkaLoggingService) {
    this.env = env;
    this.webClient =
        WebClient.builder()
            // The default memory size for json in spring is 262,144. Occasionally we get bigger
            // json response setting max size to 10 MB
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
            .baseUrl(baseUrl)
            .clientConnector(
                new ReactorClientHttpConnector(HttpClient.create(getConnectionProvider())))
            .build();
    this.partnerName = partnerName;
    this.kafkaLoggingService = kafkaLoggingService;
    this.gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    this.baseUrl = baseUrl;
  }

  private ConnectionProvider getConnectionProvider() {
    return ConnectionProvider.builder("web-client-provider")
        .maxIdleTime(
            Duration.ofSeconds(
                Long.parseLong(
                    Objects.requireNonNull(env.getProperty("web-client.max-idle-time")))))
        .maxLifeTime(
            Duration.ofSeconds(
                Long.parseLong(
                    Objects.requireNonNull(env.getProperty("web-client.max-life-time")))))
        .pendingAcquireTimeout(
            Duration.ofSeconds(
                Long.parseLong(
                    Objects.requireNonNull(env.getProperty("web-client.pending-acquire-timeout")))))
        .evictInBackground(
            Duration.ofSeconds(
                Long.parseLong(
                    Objects.requireNonNull(env.getProperty("web-client.evict-background")))))
        .pendingAcquireMaxCount(
            Integer.parseInt(
                Objects.requireNonNull(env.getProperty("web-client.acquire-max-count"))))
        .build();
  }

  public WebClientFactoryImpl(Environment env, KafkaLoggingService kafkaLoggingService) {
    this.env = env;
    this.kafkaLoggingService = kafkaLoggingService;
    this.webClient =
        WebClient.builder()
            .clientConnector(
                new ReactorClientHttpConnector(HttpClient.create(getConnectionProvider())))
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    this.gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    this.partnerName = null;
  }

  @Override
  public <T> Mono<T> getData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {

    boolean logRequired = webClientParameters.getLogRequired();
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    Integer timeout = webClientParameters.getTimeout();
    int effectiveTimeout = (timeout != null && timeout > 0) ? timeout : 120000;

    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.GET, null, partnerCode, loggerHeader);
    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);

    return webClient
        .get()
        .uri(uri)
        .headers(h -> h.addAll(headers))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ClientSideException(
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode(), uri));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ServerErrorException(
                                  SERVER_ERROR, parsedErrorBody, clientResponse.statusCode()));
                        }))
        .bodyToMono(byte[].class)
        .timeout(Duration.ofMillis(effectiveTimeout))
        .flatMap(
            rawBytes ->
                deserializeBytesResponseAndLog(
                    rawBytes, responseType, partnerCode, loggerHeader, logEventDTO, logRequired));
  }

  @Override
  public <T> Mono<T> postData(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {

    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    Integer retries = webClientParameters.getRetryCount();
    Integer timeout = webClientParameters.getTimeout();
    boolean logRequired = webClientParameters.getLogRequired();

    int effectiveTimeout = (timeout != null && timeout > 0) ? timeout : 120000;

    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.POST, request, partnerCode, loggerHeader);
    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);
    if (logRequired) {
      log.info(LOGGING_LITERAL, loggerHeader, partnerCode, gson.toJson(request));
    }
    WebClient.RequestBodySpec requestBodySpec =
        webClient.post().uri(uri).headers(h -> h.addAll(headers));
    if (request != null) {
      requestBodySpec.body(BodyInserters.fromValue(request));
    }
    return requestBodySpec
        .contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ClientSideException(
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode(), uri));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ServerErrorException(
                                  SERVER_ERROR, parsedErrorBody, clientResponse.statusCode()));
                        }))
        .bodyToMono(byte[].class)
        .timeout(Duration.ofMillis(effectiveTimeout))
        .retryWhen(
            Retry.backoff(
                    retries,
                    Duration.ofMillis(
                        Long.parseLong(
                            Objects.requireNonNull(env.getProperty("web-client.retry-backoff")))))
                .doBeforeRetry(
                    retrySignal ->
                        log.info(
                            LOGGING_LITERAL_RETRY,
                            loggerHeader,
                            partnerName,
                            retrySignal.totalRetries()))
                .filter(
                    exception ->
                        (retries > 0)
                            && (exception instanceof ServerErrorException
                                || exception instanceof ClientSideException))
                .onRetryExhaustedThrow((spec, retrySignal) -> retrySignal.failure()))
        .flatMap(
            rawBytes ->
                deserializeBytesResponseAndLog(
                    rawBytes, responseType, partnerCode, loggerHeader, logEventDTO, logRequired));
  }

  @Override
  public <T> Mono<T> putData(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    return null;
  }

  @Override
  public <T> Mono<T> uploadDocument(
      String uri,
      MultiValueMap<String, Object> request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {

    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    boolean logRequired = webClientParameters.getLogRequired();
    Integer timeout = webClientParameters.getTimeout();

    int effectiveTimeout = (timeout != null && timeout > 0) ? timeout : 120000;

    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.POST, request, partnerCode, loggerHeader);

    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);

    return webClient
        .post()
        .uri(uri)
        .headers(h -> h.addAll(headers))
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(request))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ClientSideException(
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode(), uri));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ServerErrorException(
                                  SERVER_ERROR, parsedErrorBody, clientResponse.statusCode()));
                        }))
        .bodyToMono(byte[].class)
        .timeout(Duration.ofMillis(effectiveTimeout))
        .flatMap(
            rawBytes ->
                deserializeBytesResponseAndLog(
                    rawBytes, responseType, partnerCode, loggerHeader, logEventDTO, logRequired));
  }

  @Override
  public <T> Flux<T> getFluxData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {

    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    Integer retries = webClientParameters.getRetryCount();
    Integer timeout = webClientParameters.getTimeout();

    int effectiveTimeout = (timeout != null && timeout > 0) ? timeout : 120000;

    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.GET, null, partnerCode, loggerHeader);

    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);

    return webClient
        .get()
        .uri(uri)
        .headers(h -> h.addAll(headers))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ClientSideException(
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode(), uri));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse ->
                clientResponse
                    .bodyToMono(byte[].class)
                    .flatMap(
                        errorBody -> {
                          String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
                          Object parsedErrorBody;
                          try {
                            parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
                          } catch (com.google.gson.JsonSyntaxException e) {
                            log.error(
                                "[{}] [ERROR] error body is not json, treating as plain"
                                    + " text/xml/html. original exception: {}",
                                loggerHeader,
                                e.getMessage());
                            parsedErrorBody = errorBodyString;
                          }
                          logError(
                              clientResponse,
                              parsedErrorBody,
                              partnerCode,
                              loggerHeader,
                              logEventDTO);
                          return Mono.error(
                              new ServerErrorException(
                                  SERVER_ERROR, parsedErrorBody, clientResponse.statusCode()));
                        }))
        .bodyToFlux(responseType)
        .timeout(Duration.ofMillis(effectiveTimeout))
        .retryWhen(
            Retry.backoff(
                    retries,
                    Duration.ofMillis(
                        Long.parseLong(
                            Objects.requireNonNull(env.getProperty("web-client.retry-backoff")))))
                .doBeforeRetry(
                    retrySignal ->
                        log.info(
                            LOGGING_LITERAL_RETRY,
                            loggerHeader,
                            partnerName,
                            retrySignal.totalRetries()))
                .filter(
                    exception ->
                        (retries > 0)
                            && (exception instanceof ServerErrorException
                                || exception instanceof ClientSideException))
                .onRetryExhaustedThrow((spec, retrySignal) -> retrySignal.failure()))
        .collectList()
        .doOnSuccess(
            responseList ->
                kafkaLoggingService.logSuccessKafka(logEventDTO, responseList, HttpStatus.OK))
        .flatMapMany(Flux::fromIterable)
        .doOnError(
            error ->
                log.error(
                    "[{}] [ERROR] error during response body processing: {}",
                    loggerHeader,
                    error.getMessage()));
  }

  private void logSuccess(
      Object responseBody,
      String partnerIdentifier,
      String loggerHeader,
      LogEventsDTO logEventDTO,
      boolean logRequired) {
    if (logRequired) {
      log.info(LOGGING_RESPONSE, loggerHeader, partnerIdentifier, gson.toJson(responseBody));
    }
    logKafkaAsync(
        () -> kafkaLoggingService.logSuccessKafka(logEventDTO, responseBody, HttpStatus.OK));
  }

  private void logError(
      ClientResponse clientResponse,
      Object errorMessage,
      String partnerIdentifier,
      String loggerHeader,
      LogEventsDTO logEventDTO) {
    log.error(
        LOGGING_ERROR_RESPONSE,
        loggerHeader,
        partnerIdentifier,
        clientResponse.statusCode().value(),
        gson.toJson(errorMessage));
    logKafkaAsync(
        () ->
            kafkaLoggingService.logErrorKafka(
                logEventDTO, errorMessage, clientResponse.statusCode()));
  }

  private String combineUrl(String baseUrl, String url) {
    return (Objects.isNull(baseUrl) ? "" : baseUrl) + url;
  }

  private void logKafkaAsync(Runnable loggingTask) {
    Mono.fromRunnable(loggingTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error in async Kafka logging", error));
  }

  private <T> Mono<T> deserializeBytesResponseAndLog(
      byte[] rawBytes,
      Class<T> responseType,
      String partnerCode,
      String loggerHeader,
      LogEventsDTO logEventDTO,
      boolean logRequired) {

    // Fast path for byte[] response
    if (responseType == byte[].class) {
      logSuccess(rawBytes, partnerCode, loggerHeader, logEventDTO, logRequired);
      return Mono.just((T) rawBytes);
    }

    String rawBody = new String(rawBytes, StandardCharsets.UTF_8);

    try {
      T parsedData =
          responseType == String.class ? (T) rawBody : gson.fromJson(rawBody, responseType);

      logSuccess(parsedData, partnerCode, loggerHeader, logEventDTO, logRequired);
      return Mono.just(parsedData);

    } catch (com.google.gson.JsonSyntaxException e) {
      logSuccess(rawBody, partnerCode, loggerHeader, logEventDTO, logRequired);
      return Mono.error(new RuntimeException("Invalid JSON syntax in response: " + rawBody, e));

    } catch (com.google.gson.JsonIOException e) {
      logSuccess(rawBody, partnerCode, loggerHeader, logEventDTO, logRequired);
      return Mono.error(
          new RuntimeException("I/O error while parsing JSON response: " + rawBody, e));

    } catch (com.google.gson.JsonParseException e) {
      logSuccess(rawBody, partnerCode, loggerHeader, logEventDTO, logRequired);
      return Mono.error(new RuntimeException("Malformed JSON response: " + rawBody, e));
    }
  }
}
