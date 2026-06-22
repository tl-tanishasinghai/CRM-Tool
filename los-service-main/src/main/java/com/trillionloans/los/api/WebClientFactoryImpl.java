package com.trillionloans.los.api;

import static com.trillionloans.los.constant.StringConstants.CLIENT_ERROR;
import static com.trillionloans.los.constant.StringConstants.FORBIDDEN_ERROR;
import static com.trillionloans.los.constant.StringConstants.HTML_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.LOGGING_ERROR_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.LOGGING_LITERAL;
import static com.trillionloans.los.constant.StringConstants.LOGGING_LITERAL_RETRY;
import static com.trillionloans.los.constant.StringConstants.LOGGING_LITERAL_URI;
import static com.trillionloans.los.constant.StringConstants.LOGGING_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.SERVER_ERROR;
import static com.trillionloans.los.constant.StringConstants.TIMEOUT_ERROR;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.CustomTimeoutException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.LogEventsDTO;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.response.m2p.M2pErrorResponseDTO;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
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
 * The WebClientFactory impl provides methods for making HTTP requests using WebClient in a reactive
 * programming style.
 */
@Slf4j
public class WebClientFactoryImpl implements WebClientFactory {

  private final WebClient webClient;
  private final String partnerName;
  private final Environment env;
  private final Gson gson;
  private final KafkaLoggingService kafkaLoggingService;
  private String baseUrl;
  private final boolean disableSslVerification;

  private final KafkaEventProducerService kafkaEventProducerService;

  public WebClientFactoryImpl(
      String baseUrl,
      String partnerName,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this(baseUrl, partnerName, env, kafkaLoggingService, kafkaEventProducerService, false);
  }

  public WebClientFactoryImpl(
      String baseUrl,
      String partnerName,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      boolean disableSslVerification) {
    this.env = env;
    this.disableSslVerification = disableSslVerification;
    this.webClient =
        WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(createHttpClient(partnerName)))
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    this.partnerName = partnerName;
    this.kafkaLoggingService = kafkaLoggingService;
    this.kafkaEventProducerService = kafkaEventProducerService;
    this.gson =
        new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .disableHtmlEscaping()
            .create();
    this.baseUrl = baseUrl;
  }

  public WebClientFactoryImpl(
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.env = env;
    this.disableSslVerification = false;
    this.kafkaLoggingService = kafkaLoggingService;
    this.kafkaEventProducerService = kafkaEventProducerService;
    this.webClient =
        WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(createHttpClient(null)))
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    this.gson =
        new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .disableHtmlEscaping()
            .create();
    this.partnerName = null;
  }

  private HttpClient createHttpClient(String partnerNameForLog) {
    HttpClient httpClient = HttpClient.create(getConnectionProvider());
    if (disableSslVerification || !isSslVerifyEnabled()) {
      log.warn(
          "WebClient SSL certificate verification is DISABLED for {}. "
              + "Do not use this in production.",
          partnerNameForLog != null ? partnerNameForLog : "client");
      try {
        httpClient =
            httpClient.secure(
                spec -> {
                  try {
                    spec.sslContext(
                        SslContextBuilder.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build());
                  } catch (SSLException e) {
                    throw new RuntimeException(e);
                  }
                });
      } catch (Exception e) {
        log.error("Failed to configure SSL bypass", e);
        throw new RuntimeException("Failed to configure SSL bypass", e);
      }
    }
    return httpClient;
  }

  private boolean isSslVerifyEnabled() {
    String value = env.getProperty("web-client.ssl.verify-enabled");
    return value == null || Boolean.parseBoolean(value);
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
        .pendingAcquireMaxCount(
            Integer.parseInt(
                Objects.requireNonNull(env.getProperty("web-client.acquire-max-count"))))
        .pendingAcquireTimeout(
            Duration.ofSeconds(
                Long.parseLong(
                    Objects.requireNonNull(env.getProperty("web-client.pending-acquire-timeout")))))
        .evictInBackground(
            Duration.ofSeconds(
                Long.parseLong(
                    Objects.requireNonNull(env.getProperty("web-client.evict-background")))))
        .build();
  }

  public <T> Mono<T> getData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    boolean logRequired = webClientParameters.getLogRequired();
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.GET, null, partnerCode, loggerHeader);
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());

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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .bodyToMono(responseType)
        .timeout(Duration.ofMillis(effectiveTimeout))
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
        .doOnSuccess(
            data ->
                logSuccess(
                    data,
                    partnerCode,
                    loggerHeader,
                    logEventDTO,
                    logRequired,
                    webClientParameters.getEventContext()));
  }

  private int getEffectiveTimeout(Integer timeout) {
    return (timeout != null && timeout > 0) ? timeout : 30000;
  }

  public <T> Mono<T> getDataWithoutStringSerialization(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    boolean logRequired = webClientParameters.getLogRequired();
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.GET, null, partnerCode, loggerHeader);
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());

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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
        .flatMap(
            rawBytes ->
                deserializeBytesResponseAndLog(
                    rawBytes,
                    responseType,
                    partnerCode,
                    loggerHeader,
                    logEventDTO,
                    logRequired,
                    webClientParameters.getEventContext()));
  }

  public <T> Mono<T> postDataWithoutStringSerialization(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    boolean logRequired = webClientParameters.getLogRequired();
    Integer retries = webClientParameters.getRetryCount();
    boolean responseLogRequired = webClientParameters.getResponseLogRequired();
    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.POST, request, partnerCode, loggerHeader);
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
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
                    rawBytes,
                    responseType,
                    partnerCode,
                    loggerHeader,
                    logEventDTO,
                    responseLogRequired,
                    webClientParameters.getEventContext()));
  }

  public <T> Flux<T> getFluxDataWithoutStringSerialization(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    boolean publishEvent =
        webClientParameters.getEventContext() != null
            && webClientParameters.getEventContext().isPublishEvent();
    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);
    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.GET, null, partnerCode, loggerHeader);
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
        .collectList()
        .doOnSuccess(
            responseList -> {
              kafkaLoggingService.logSuccessKafka(logEventDTO, responseList, HttpStatus.OK);
              if (publishEvent)
                logKafkaAsync(
                    () ->
                        kafkaEventProducerService.publishEvent(
                            webClientParameters.getEventContext(), responseList, null));
            })
        .flatMapMany(Flux::fromIterable)
        .doOnError(
            error ->
                log.error(
                    "[{}] [ERROR] error during response body processing: {}",
                    loggerHeader,
                    error.getMessage()));
  }

  public <T> Mono<T> postDataWithForBiddenHandling(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    boolean logRequired = webClientParameters.getLogRequired();
    boolean responseLogRequired = webClientParameters.getResponseLogRequired();
    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.POST, request, partnerCode, loggerHeader);
    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);
    if (logRequired) {
      log.info(LOGGING_LITERAL, loggerHeader, partnerCode, gson.toJson(request));
    }
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());
    return webClient
        .post()
        .uri(uri)
        .headers(h -> h.addAll(headers))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(request))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                handle4xxError(clientResponse, partnerCode, loggerHeader, logEventDTO))
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
        .flatMap(
            rawBytes ->
                deserializeBytesResponseAndLog(
                    rawBytes,
                    responseType,
                    partnerCode,
                    loggerHeader,
                    logEventDTO,
                    responseLogRequired,
                    webClientParameters.getEventContext()));
  }

  private <T> Mono<? extends T> handle4xxError(
      ClientResponse clientResponse,
      String partnerCode,
      String loggerHeader,
      LogEventsDTO logEventDTO) {
    return clientResponse.statusCode() == HttpStatus.FORBIDDEN
        ? handleForbiddenError(clientResponse, partnerCode, loggerHeader, logEventDTO)
        : handleClientSideError(clientResponse, partnerCode, loggerHeader, logEventDTO);
  }

  private <T> Mono<? extends T> handleClientSideError(
      ClientResponse clientResponse,
      String partnerCode,
      String loggerHeader,
      LogEventsDTO logEventDTO) {
    return clientResponse
        .bodyToMono(byte[].class)
        .flatMap(
            errorBody -> {
              String errorBodyString = new String(errorBody, StandardCharsets.UTF_8);
              Object parsedErrorBody;
              try {
                parsedErrorBody = gson.fromJson(errorBodyString, Object.class);
              } catch (com.google.gson.JsonSyntaxException e) {
                log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
                parsedErrorBody = errorBodyString;
              }
              logError(clientResponse, parsedErrorBody, partnerCode, loggerHeader, logEventDTO);
              return Mono.error(
                  new ClientSideException(
                      CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
            });
  }

  private <T> Mono<? extends T> handleForbiddenError(
      ClientResponse clientResponse,
      String partnerCode,
      String loggerHeader,
      LogEventsDTO logEventDTO) {
    return clientResponse
        .bodyToMono(M2pErrorResponseDTO.class)
        .flatMap(
            errorBody -> {
              logError(clientResponse, errorBody, partnerCode, loggerHeader, logEventDTO);
              return Mono.error(
                  new ForbiddenException(FORBIDDEN_ERROR, errorBody, clientResponse.statusCode()));
            });
  }

  public <T> Mono<T> uploadDocumentWithoutStringSerialization(
      String uri,
      MultiValueMap<String, Object> request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);
    boolean responseLogRequired = webClientParameters.getResponseLogRequired();
    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.POST, request, partnerCode, loggerHeader);
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
        .flatMap(
            rawBytes ->
                deserializeBytesResponseAndLog(
                    rawBytes,
                    responseType,
                    partnerCode,
                    loggerHeader,
                    logEventDTO,
                    responseLogRequired,
                    webClientParameters.getEventContext()));
  }

  public <T> Mono<T> putDataWithoutStringSerialization(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    boolean logRequired = webClientParameters.getLogRequired();
    boolean responseLogRequired = webClientParameters.getResponseLogRequired();
    Integer retries = webClientParameters.getRetryCount();
    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.PUT, request, partnerCode, loggerHeader);
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());
    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);
    if (logRequired && request != null) {
      log.info(LOGGING_LITERAL, loggerHeader, partnerCode, gson.toJson(request));
    }
    WebClient.RequestBodySpec requestBodySpec =
        webClient.put().uri(uri).headers(h -> h.addAll(headers));

    if (request != null) {
      requestBodySpec.body(BodyInserters.fromValue(request));
    }
    return requestBodySpec
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
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
                    rawBytes,
                    responseType,
                    partnerCode,
                    loggerHeader,
                    logEventDTO,
                    responseLogRequired,
                    webClientParameters.getEventContext()));
  }

  public <T> Flux<T> getFluxDataWithTypeRef(
      String uri,
      HttpHeaders headers,
      ParameterizedTypeReference<T> responseType,
      WebClientParameters webClientParameters) {
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
    log.info(LOGGING_LITERAL_URI, loggerHeader, partnerCode, uri);
    boolean publishEvent =
        webClientParameters.getEventContext() != null
            && webClientParameters.getEventContext().isPublishEvent();

    LogEventsDTO logEventDTO =
        kafkaLoggingService.createLogEventDTO(
            combineUrl(baseUrl, uri), HttpMethod.GET, null, partnerCode, loggerHeader);
    int effectiveTimeout = getEffectiveTimeout(webClientParameters.getTimeout());

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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .onErrorResume(
            TimeoutException.class,
            ex -> {
              log.error("[{}] {}", loggerHeader, TIMEOUT_ERROR);
              return Mono.error(
                  new CustomTimeoutException(TIMEOUT_ERROR, null, HttpStatus.GATEWAY_TIMEOUT));
            })
        .collectList()
        .doOnSuccess(
            responseList -> {
              kafkaLoggingService.logSuccessKafka(logEventDTO, responseList, HttpStatus.OK);
              if (publishEvent)
                logKafkaAsync(
                    () ->
                        kafkaEventProducerService.publishEvent(
                            webClientParameters.getEventContext(), responseList, null));
            })
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
      LogEventsDTO logEventsDTO,
      boolean logRequired,
      EventContext eventContext) {
    if (logRequired) {
      log.info(LOGGING_RESPONSE, loggerHeader, partnerIdentifier, gson.toJson(responseBody));
    }
    logKafkaAsync(
        () -> kafkaLoggingService.logSuccessKafka(logEventsDTO, responseBody, HttpStatus.OK));

    if (eventContext != null && eventContext.isPublishEvent())
      logKafkaAsync(
          () ->
              kafkaEventProducerService.publishEvent(
                  eventContext, responseBody, logEventsDTO.getRequestBody()));
  }

  private void logError(
      ClientResponse clientResponse,
      Object errorMessage,
      String partnerIdentifier,
      String loggerHeader,
      LogEventsDTO logEventsDTO) {
    log.error(
        LOGGING_ERROR_RESPONSE,
        loggerHeader,
        partnerIdentifier,
        clientResponse.statusCode().value(),
        gson.toJson(errorMessage));
    logKafkaAsync(
        () ->
            kafkaLoggingService.logErrorKafka(
                logEventsDTO, errorMessage, clientResponse.statusCode()));
  }

  private String combineUrl(String baseUrl, String url) {
    return (Objects.isNull(baseUrl) ? "" : baseUrl) + url;
  }

  private void logKafkaAsync(Runnable loggingTask) {
    Mono.fromRunnable(loggingTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("[{KAFKA_OPS}] error in async Kafka logging", error));
  }

  private <T> Mono<T> deserializeBytesResponseAndLog(
      byte[] rawBytes,
      Class<T> responseType,
      String partnerCode,
      String loggerHeader,
      LogEventsDTO logEventDTO,
      boolean logRequired,
      EventContext eventContext) {

    if (responseType == byte[].class) {
      logSuccess(rawBytes, partnerCode, loggerHeader, logEventDTO, logRequired, eventContext);
      return Mono.just((T) rawBytes);
    }

    try {
      String rawBody = new String(rawBytes, StandardCharsets.UTF_8);
      T parsedData =
          responseType == String.class ? (T) rawBody : gson.fromJson(rawBody, responseType);
      logSuccess(parsedData, partnerCode, loggerHeader, logEventDTO, logRequired, eventContext);
      return Mono.just(parsedData);
    } catch (Exception e) {
      String rawBody = new String(rawBytes, StandardCharsets.UTF_8);
      logSuccess(rawBody, partnerCode, loggerHeader, logEventDTO, logRequired, eventContext);
      return Mono.error(new RuntimeException("invalid response format, response: " + rawBody, e));
    }
  }

  public <T> Mono<T> getDataAsTextResponse(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters) {
    boolean logRequired = webClientParameters.getLogRequired();
    String partnerCode = partnerName == null ? webClientParameters.getPartnerName() : partnerName;
    String loggerHeader = webClientParameters.getLoggerHeader();
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
                                  CLIENT_ERROR, parsedErrorBody, clientResponse.statusCode()));
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
                            log.error(HTML_RESPONSE, loggerHeader, e.getMessage());
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
        .bodyToMono(String.class)
        .flatMap(
            responseBody -> {
              T result;
              if (responseType == String.class) {
                // If expecting String, return as-is
                result = responseType.cast(responseBody);
              } else {
                // Try to parse as JSON
                try {
                  result = gson.fromJson(responseBody, responseType);
                } catch (com.google.gson.JsonSyntaxException e) {
                  log.warn(
                      "[{}] [WARN] Response is not JSON for type {}, returning as String if"
                          + " possible",
                      loggerHeader,
                      responseType.getName());
                  // If response type is Object.class, return the string as-is
                  if (responseType == Object.class) {
                    result = responseType.cast(responseBody);
                  } else {
                    return Mono.error(e);
                  }
                }
              }
              return Mono.just(result);
            })
        .doOnSuccess(
            data ->
                logSuccess(
                    data,
                    partnerCode,
                    loggerHeader,
                    logEventDTO,
                    logRequired,
                    webClientParameters.getEventContext()));
  }
}
