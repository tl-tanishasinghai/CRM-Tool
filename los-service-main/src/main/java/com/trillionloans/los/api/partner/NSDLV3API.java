package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.LOGGING_LITERAL_RETRY;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.exception.PanValidationExceptions.PanVerificationException;
import com.trillionloans.los.exception.TransientVendorException;
import com.trillionloans.los.mapper.OpvResponseCode;
import com.trillionloans.los.model.NsdlPanVerificationResponse;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.request.NsdlPanVerificationRequest;
import com.trillionloans.los.model.request.OpvAPIRequest;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.PKCS7Signer;
import com.trillionloans.los.util.WebClientUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Service
public class NSDLV3API {

  private final String userId;
  private final String nsdlUrl;
  private final PKCS7Signer signer;
  private final WebClientUtil util;
  private final WebClientFactory webClientFactory;
  private final Environment environment;

  private static final String VERSION = "4";

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"); // up to milliseconds

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public NSDLV3API(
      PKCS7Signer signer,
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      @Value("${nsdl.api.url}") String nsdlUrl,
      @Value("${nsdl.user-id}") String userId) {
    this.webClientFactory =
        new WebClientFactoryImpl(
            nsdlUrl, "NSDL", environment, kafkaLoggingService, kafkaEventProducerService);
    this.signer = signer;
    this.environment = environment;
    this.nsdlUrl = nsdlUrl;
    this.userId = userId;
    this.util = new WebClientUtil();
  }

  public Mono<NsdlPanVerificationResponse> verify(
      List<NsdlPanVerificationRequest> panList, String clientId, String loanApplicationId) {
    if (!signer.isEnabled()) {
      log.warn(
          "[NSDL_API] Skipping PAN verification. Signer is disabled. Reason={}",
          signer.getReasonDisabled());
      return Mono.error(
          new PanVerificationException(
              "PAN verification unavailable: " + signer.getReasonDisabled(),
              null,
              HttpStatus.SERVICE_UNAVAILABLE));
    }

    // prepare once to extract retry config
    NsdlRequestData firstReq = prepareNsdlRequest(panList, clientId, loanApplicationId);
    int retries = firstReq.params.getRetryCount();
    long backoffMs =
        Long.parseLong(Objects.requireNonNull(environment.getProperty("web-client.retry-backoff")));

    Retry retrySpec =
        retries > 0
            ? Retry.backoff(retries, Duration.ofMillis(backoffMs))
                .doBeforeRetry(
                    retrySignal ->
                        log.info(
                            LOGGING_LITERAL_RETRY, "NSDL_API", "NSDL", retrySignal.totalRetries()))
                .filter(ex -> ex instanceof TransientVendorException)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure())
            : Retry.max(0); // disables retry

    return Mono.defer(
            () -> {
              // prepare request on each attempt
              NsdlRequestData req = prepareNsdlRequest(panList, clientId, loanApplicationId);

              return webClientFactory
                  .postDataWithoutStringSerialization(
                      req.uri,
                      req.requestBody,
                      req.headers,
                      NsdlPanVerificationResponse.class,
                      req.params)
                  .flatMap(
                      response -> {
                        String code = response.getResponseCode();

                        // Retry only for SYSTEM_ERROR (2) or SYSTEM_FAILURE (23)
                        if (OpvResponseCode.SYSTEM_ERROR.getCode().equals(code)
                            || OpvResponseCode.SYSTEM_FAILURE.getCode().equals(code)) {

                          log.error(
                              "[PAN_VERIFY][NSDL_API] Unsuccessful response from the vendor, {}:"
                                  + " {}.",
                              code,
                              OpvResponseCode.fromCode(code).getDescription());

                          return Mono.error(
                              new TransientVendorException(
                                  "Retryable vendor error: statusCode=" + code));
                        }

                        return Mono.just(response);
                      });
            })
        .retryWhen(retrySpec);
  }

  /** Prepares all request components needed for NSDL PAN verification. */
  private NsdlRequestData prepareNsdlRequest(
      List<NsdlPanVerificationRequest> panList, String clientId, String loanApplicationId) {
    try {
      String requestTime =
          ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

      String transactionId = generateTransactionId(userId);
      String unsignedJson = OBJECT_MAPPER.writeValueAsString(panList);
      String signature = signer.sign(unsignedJson);

      OpvAPIRequest requestBody =
          OpvAPIRequest.builder().inputData(panList).signature(signature).build();

      HttpHeaders headers = getNSDLHeaders(userId, panList.size(), requestTime, transactionId);
      String uri = UriComponentsBuilder.fromUriString(requireNonNull(nsdlUrl)).toUriString();
      EventContext eventContext =
          new EventContext(Event.NSDL_PAN_VERIFY, loanApplicationId, clientId);
      WebClientParameters params =
          util.getWebClientParameters(null, "PAN_VERIFY", 3, true, true, eventContext);

      return new NsdlRequestData(uri, requestBody, headers, params);
    } catch (Exception e) {
      log.error(
          "[PAN_VERIFY][OPV_API] Error preparing OPV PAN verification request, error={}",
          e.getMessage());
      throw new PanVerificationException(
          "Error preparing NSDL PAN verification request", e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /** Holder class for all prepared request components. */
  private static class NsdlRequestData {
    final String uri;
    final OpvAPIRequest requestBody;
    final HttpHeaders headers;
    final WebClientParameters params;

    NsdlRequestData(
        String uri, OpvAPIRequest requestBody, HttpHeaders headers, WebClientParameters params) {
      this.uri = uri;
      this.requestBody = requestBody;
      this.headers = headers;
      this.params = params;
    }
  }

  private HttpHeaders getNSDLHeaders(
      String userId, int recordsCount, String requestTime, String transactionId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add("User_ID", userId);
    headers.add("Records_count", String.valueOf(recordsCount));
    headers.add("Request_time", requestTime);
    headers.add("Transaction_ID", transactionId);
    headers.add("Version", VERSION);
    return headers;
  }

  private static String generateTransactionId(String userId) {
    // Base timestamp up to milliseconds
    String baseTimestamp = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).format(DATE_TIME_FORMATTER);

    // Add extra micro/nano digits for uniqueness (3 digits)
    String extraDigits = String.format("%03d", System.nanoTime() % 1000);

    // Final timestamp = millis + 3 extra digits
    String timestamp = baseTimestamp + extraDigits;

    return userId + ":" + timestamp;
  }
}
