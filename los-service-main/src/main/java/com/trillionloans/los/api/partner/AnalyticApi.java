package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.LOGGER_HEADER;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static java.util.Objects.requireNonNull;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.request.AnalyticAddressSimilarityRequest;
import com.trillionloans.los.model.request.AnalyticFaceSimilarityRequest;
import com.trillionloans.los.model.request.AnalyticNameSimilarityRequest;
import com.trillionloans.los.model.response.AnalyticAddressSimilarityResponse;
import com.trillionloans.los.model.response.AnalyticFaceSimilarityResponse;
import com.trillionloans.los.model.response.AnalyticNameSimilarityResponse;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AnalyticApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;

  public AnalyticApi(
      @Value("${analytic-service.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.environment = env;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "ANALYTIC_SERVICE", env, kafkaLoggingService, kafkaEventProducerService);
    this.util = new WebClientUtil();
  }

  public Mono<AnalyticNameSimilarityResponse> checkNameSimilarity(
      AnalyticNameSimilarityRequest analyticNameSimilarityRequest, String clientId, String loanId) {

    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("analytic-service.api.name-similarity")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.ANALYTIC_NAME_SIMILARITY_CHECK, loanId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ANALYTIC_NAME_SIMILARITY", 3, true, true, eventContext);

    return Mono.deferContextual(
        context -> {
          String traceId =
              context.getOrDefault(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
          log.info(
              "[{}] calling analytic service api with traceId: {}", "ANALYTIC_SERVICE", traceId);
          HttpHeaders httpHeaders = getAnalyticServiceHeaders(traceId);
          httpHeaders.add(
              LOGGER_HEADER,
              "POST " + environment.getProperty("analytic-service.api.name-similarity"));
          return webClientFactory.postDataWithoutStringSerialization(
              uri,
              analyticNameSimilarityRequest,
              httpHeaders,
              AnalyticNameSimilarityResponse.class,
              webClientParameters);
        });
  }

  public Mono<AnalyticFaceSimilarityResponse> checkFaceSimilarity(
      AnalyticFaceSimilarityRequest analyticFaceSimilarityRequest, String clientId, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("analytic-service.api.face-similarity")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.ANALYTIC_FACE_SIMILARITY_CHECK, loanId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ANALYTIC_FACE_SIMILARITY", 3, true, true, eventContext);

    return Mono.deferContextual(
        context -> {
          String traceId =
              context.getOrDefault(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
          log.info(
              "[{}] calling analytic service api with traceId: {}", "ANALYTIC_SERVICE", traceId);
          HttpHeaders httpHeaders = getAnalyticServiceHeaders(traceId);
          httpHeaders.add(
              LOGGER_HEADER,
              "POST " + environment.getProperty("analytic-service.api.face-similarity"));
          return webClientFactory.postDataWithoutStringSerialization(
              uri,
              analyticFaceSimilarityRequest,
              httpHeaders,
              AnalyticFaceSimilarityResponse.class,
              webClientParameters);
        });
  }

  public Mono<AnalyticAddressSimilarityResponse> checkAddressSimilarity(
      AnalyticAddressSimilarityRequest analyticAddressSimilarityRequest,
      String clientId,
      String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("analytic-service.api.address-similarity")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.ANALYTIC_ADDRESS_SIMILARITY_CHECK, loanId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "ANALYTIC_ADDRESS_SIMILARITY", 3, true, true, eventContext);

    return Mono.deferContextual(
        context -> {
          String traceId =
              context.getOrDefault(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
          log.info(
              "[{}] calling analytic service api with traceId: {}", "ANALYTIC_SERVICE", traceId);
          HttpHeaders httpHeaders = getAnalyticServiceHeaders(traceId);
          httpHeaders.add(
              LOGGER_HEADER,
              "POST " + environment.getProperty("analytic-service.api.address-similarity"));
          return webClientFactory.postDataWithoutStringSerialization(
              uri,
              analyticAddressSimilarityRequest,
              httpHeaders,
              AnalyticAddressSimilarityResponse.class,
              webClientParameters);
        });
  }

  private HttpHeaders getAnalyticServiceHeaders(String traceId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("traceId", traceId);
    return headers;
  }
}
