package com.trillionloans.customer_portal.api;

import static com.trillionloans.customer_portal.constant.StringConstants.*;
import static com.trillionloans.customer_portal.constant.StringConstants.DEFAULT_ERROR_MESSAGE;
import static com.trillionloans.customer_portal.constant.StringConstants.HTTP_STATUS_410;
import static com.trillionloans.customer_portal.constant.StringConstants.LOGGING_ERROR_RESPONSE;
import static com.trillionloans.customer_portal.constant.StringConstants.LOGGING_LITERAL_URI;
import static com.trillionloans.customer_portal.constant.StringConstants.LOGGING_LITERAL_URI_AND_BODY;
import static com.trillionloans.customer_portal.constant.StringConstants.LOGGING_RESPONSE;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.trillionloans.customer_portal.constant.OtpErrorMessage;
import com.trillionloans.customer_portal.constant.StringConstants;
import com.trillionloans.customer_portal.exception.ClientSideException;
import com.trillionloans.customer_portal.exception.DownstreamServiceException;
import com.trillionloans.customer_portal.exception.ForbiddenException;
import com.trillionloans.customer_portal.exception.ServerErrorException;
import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import com.trillionloans.customer_portal.model.response.ErrorResponseAuthMSG91;
import com.trillionloans.customer_portal.util.CommonUtil;
import com.trillionloans.customer_portal.util.JsonUtil;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

@Slf4j
public class WebClientFactoryImpl implements WebClientFactory {

  private final WebClient webClient;
  private final String partnerName;
  private final Environment env;
  private final Gson gson;
  private final Class<?> errorResponseType;

  public WebClientFactoryImpl(
      String baseUrl, String partnerName, Environment env, Class<?> errorResponseType) {
    this.env = env;
    this.webClient =
        WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(
                new ReactorClientHttpConnector(HttpClient.create(getConnectionProvider())))
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
            .build();
    this.partnerName = partnerName;
    this.errorResponseType = errorResponseType;
    this.gson = new Gson();
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
        .build();
  }

  @Override
  public <T> Mono<T> getData(
      String uri, HttpHeaders headers, Class<T> responseType, WebClientParameters params) {
    log.info(LOGGING_LITERAL_URI, params.getLoggerHeader(), getPartnerCode(params), uri);

    Mono<T> result =
        webClient
            .get()
            .uri(uri)
            .headers(h -> h.addAll(headers))
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                res -> handle4xxError(res, getPartnerCode(params), params.getLoggerHeader()))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                res -> handle5xxError(res, getPartnerCode(params), params.getLoggerHeader()))
            .bodyToMono(responseType)
            .onErrorMap(this::mapDownstreamException);

    return result.doOnSuccess(
        data -> {
          if (Boolean.TRUE.equals(params.getRequestLogRequired())) {
            logSuccess(
                data,
                getPartnerCode(params),
                params.getLoggerHeader(),
                params.getResponseLogRequired());
          }
        });
  }

  @Override
  public <T> Flux<T> getFluxData(
      String uri, HttpHeaders headers, Class<T> responseType, WebClientParameters params) {
    log.info(LOGGING_LITERAL_URI, params.getLoggerHeader(), getPartnerCode(params), uri);

    return webClient
        .get()
        .uri(uri)
        .headers(h -> h.addAll(headers))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            res -> handle4xxError(res, getPartnerCode(params), params.getLoggerHeader()))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            res -> handle5xxError(res, getPartnerCode(params), params.getLoggerHeader()))
        .bodyToFlux(responseType)
        .onErrorMap(this::mapDownstreamException)
        .doOnNext(
            data ->
                logSuccess(
                    data,
                    getPartnerCode(params),
                    params.getLoggerHeader(),
                    params.getResponseLogRequired()))
        .collectList()
        .flatMapMany(
            list -> {
              if (list.isEmpty()) log.info("Received empty response for URI: {}", uri);
              return Flux.fromIterable(list);
            });
  }

  @Override
  public <T> Mono<T> post(
      String uri,
      Object body,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters params) {
    if (params.getRequestLogRequired()) {
      log.info(
          LOGGING_LITERAL_URI_AND_BODY,
          params.getLoggerHeader(),
          getPartnerCode(params),
          uri,
          gson.toJson(body));
    }

    Integer retries = params.getRetryCount();
    WebClient.RequestBodySpec spec = webClient.post().uri(uri).headers(h -> h.addAll(headers));
    if (body != null) spec.body(BodyInserters.fromValue(body));

    return spec.contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            res -> handle4xxErrorPost(res, getPartnerCode(params), params.getLoggerHeader(), uri))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            res -> handle5xxErrorPost(res, getPartnerCode(params), params.getLoggerHeader(), uri))
        .bodyToMono(String.class)
        .onErrorMap(this::mapDownstreamException)
        .retryWhen(
            Retry.backoff(
                    retries,
                    Duration.ofMillis(
                        Long.parseLong(
                            Objects.requireNonNull(env.getProperty("web-client.retry-backoff")))))
                .doBeforeRetry(
                    retrySignal ->
                        log.info(
                            StringConstants.LOGGING_LITERAL_RETRY,
                            headers,
                            partnerName,
                            retrySignal.totalRetries()))
                .filter(
                    exception ->
                        (retries > 0)
                            && (exception instanceof ServerErrorException
                                || exception instanceof ClientSideException))
                .onRetryExhaustedThrow((retrySpec, signal) -> signal.failure()))
        .doOnSuccess(
            data ->
                logSuccess(
                    data,
                    getPartnerCode(params),
                    params.getLoggerHeader(),
                    params.getResponseLogRequired()))
        .flatMap(data -> Mono.just(gson.fromJson(data, responseType)));
  }

  private Throwable mapDownstreamException(Throwable t) {
    if (t instanceof ConnectException
        || t instanceof TimeoutException
        || t instanceof WebClientRequestException) {
      log.error(
          "Downstream connection issue: {}, message: {}",
          t.getClass().getSimpleName(),
          t.getMessage());
      return new DownstreamServiceException(
          DEFAULT_ERROR_MESSAGE, t, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return t;
  }

  @Override
  public <T> Mono<T> postMultipart(
      String uri,
      MultipartBodyBuilder bodyBuilder,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters params) {

    if (params.getRequestLogRequired()) {
      log.info(
          LOGGING_LITERAL_MULTIPART_URI,
          params.getLoggerHeader(),
          getPartnerCode(params),
          uri,
          gson.toJson(bodyBuilder.build()));
    }

    Integer retries = params.getRetryCount();
    WebClient.RequestBodySpec spec = webClient.post().uri(uri).headers(h -> h.addAll(headers));
    spec.body(BodyInserters.fromMultipartData(bodyBuilder.build()));

    return spec.contentType(MediaType.MULTIPART_FORM_DATA)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            res -> handle4xxErrorPost(res, getPartnerCode(params), params.getLoggerHeader(), uri))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            res -> handle5xxErrorPost(res, getPartnerCode(params), params.getLoggerHeader(), uri))
        .bodyToMono(byte[].class)
        .flatMap(
            bytes -> {
              try {
                String jsonString = new String(bytes, StandardCharsets.UTF_8);
                T parsedData = gson.fromJson(jsonString, responseType);
                return Mono.just(parsedData);
              } catch (JsonSyntaxException e) {
                logError(
                    null,
                    new String(bytes, StandardCharsets.UTF_8),
                    getPartnerCode(params),
                    params.getLoggerHeader());
                return Mono.error(
                    new DownstreamServiceException("Invalid response", e, HttpStatus.BAD_GATEWAY));
              }
            })
        .onErrorMap(this::mapDownstreamException)
        .retryWhen(
            Retry.backoff(
                    retries,
                    Duration.ofMillis(
                        Long.parseLong(
                            Objects.requireNonNull(env.getProperty("web-client.retry-backoff")))))
                .doBeforeRetry(
                    retrySignal ->
                        log.info(
                            StringConstants.LOGGING_LITERAL_RETRY,
                            headers,
                            partnerName,
                            retrySignal.totalRetries()))
                .filter(
                    exception ->
                        (retries > 0)
                            && (exception instanceof ServerErrorException
                                || exception instanceof ClientSideException))
                .onRetryExhaustedThrow((retrySpec, signal) -> signal.failure()))
        .doOnSuccess(
            data ->
                logSuccess(
                    data,
                    getPartnerCode(params),
                    params.getLoggerHeader(),
                    params.getResponseLogRequired()));
  }

  private <T> Mono<? extends T> handle4xxError(
      ClientResponse clientResponse, String partnerCode, String loggerHeader) {
    return clientResponse.statusCode() == HttpStatus.UNAUTHORIZED
            || clientResponse.statusCode() == HttpStatus.FORBIDDEN
        ? handleForbiddenAndUnauthorizedError(clientResponse, partnerCode, loggerHeader)
        : handleClientSideError(clientResponse, partnerCode, loggerHeader);
  }

  private <T> Mono<? extends T> handleForbiddenAndUnauthorizedError(
      ClientResponse clientResponse, String partnerCode, String loggerHeader) {
    return clientResponse
        .bodyToMono(errorResponseType)
        .flatMap(
            errorBody -> {
              logError(clientResponse.statusCode(), errorBody, partnerCode, loggerHeader);
              return Mono.error(
                  new ForbiddenException(
                      DEFAULT_ERROR_MESSAGE, errorBody, clientResponse.statusCode()));
            });
  }

  private <T> Mono<? extends T> handleClientSideError(
      ClientResponse clientResponse, String partnerCode, String loggerHeader) {
    return clientResponse
        .bodyToMono(Object.class)
        .flatMap(
            errorBody -> {
              logError(clientResponse.statusCode(), errorBody, partnerCode, loggerHeader);
              return Mono.error(
                  new ClientSideException(
                      DEFAULT_ERROR_MESSAGE, errorBody, clientResponse.statusCode()));
            });
  }

  private <T> Mono<? extends T> handle5xxError(
      ClientResponse response, String partnerCode, String header) {
    return response
        .bodyToMono(Object.class)
        .flatMap(
            body -> {
              logError(response.statusCode(), body, partnerCode, header);
              return Mono.error(
                  new ServerErrorException(DEFAULT_ERROR_MESSAGE, body, response.statusCode()));
            });
  }

  private <T> Mono<? extends T> handle4xxErrorPost(
      ClientResponse response, String partnerCode, String header, String uri) {

    return response
        .bodyToMono(String.class)
        .flatMap(
            errorBody -> {
              logError(response.statusCode(), errorBody, partnerCode, header);

              return Mono.error(
                  new ClientSideException(
                      extractErrorMessage(errorBody, response.statusCode()),
                      gson.fromJson(errorBody, Object.class),
                      response.statusCode(),
                      uri));
            });
  }

  private <T> Mono<? extends T> handle5xxErrorPost(
      ClientResponse response, String partnerCode, String header, String uri) {

    return response
        .bodyToMono(String.class)
        .flatMap(
            errorBody -> {
              logError(response.statusCode(), errorBody, partnerCode, header);

              return Mono.error(
                  new ServerErrorException(
                      extractErrorMessage(errorBody, response.statusCode()),
                      gson.fromJson(errorBody, Object.class),
                      response.statusCode(),
                      uri));
            });
  }

  private void logSuccess(Object body, String partnerCode, String header, boolean logRequired) {
    if (logRequired) {
      log.info(LOGGING_RESPONSE, header, partnerCode, gson.toJson(body));
    }
  }

  private void logError(
      HttpStatusCode statusCode,
      Object errorMessage,
      String partnerIdentifier,
      String loggerHeader) {
    log.error(
        LOGGING_ERROR_RESPONSE,
        loggerHeader,
        partnerIdentifier,
        statusCode != null ? statusCode.value() : "Unknown",
        gson.toJson(errorMessage));
  }

  private String getPartnerCode(WebClientParameters params) {
    return partnerName != null ? partnerName : params.getPartnerName();
  }

  private String extractErrorMessage(String body, HttpStatusCode statusCode) {
    try {
      ErrorResponseAuthMSG91 error = null;
      try {
        error = JsonUtil.readValue(body, ErrorResponseAuthMSG91.class);
      } catch (RuntimeException jsonEx) {
        log.warn(
            "JSON parsing failed while reading downstream error response: {}", jsonEx.getMessage());
        return DEFAULT_ERROR_MESSAGE;
      }

      if (error != null && !CommonUtil.nullOrEmpty(error.getMessage())) {
        if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
          return error.getMessage();
        }

        if (HTTP_STATUS_410.equals(error.getStatus())) {
          return extractMessage(error.getMessage());
        }
      }

    } catch (IllegalArgumentException e) {
      log.warn("Invalid argument while processing error message: {}", e.getMessage());
    } catch (RuntimeException e) {
      log.error("Unexpected runtime issue in extractErrorMessage: {}", e.getMessage(), e);
    }

    return DEFAULT_ERROR_MESSAGE;
  }

  private static String extractMessage(String message) {
    if (!CommonUtil.nullOrEmpty(message)
        && OtpErrorMessage.containsMessage(message.trim().toLowerCase())) {
      return message;
    }
    return DEFAULT_ERROR_MESSAGE;
  }
}
