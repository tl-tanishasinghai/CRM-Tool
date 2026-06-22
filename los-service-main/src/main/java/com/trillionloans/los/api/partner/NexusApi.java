package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.LOGGER_HEADER;
import static com.trillionloans.los.constant.StringConstants.LOGGING_RESPONSE;
import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.request.NexusRiskBulkRequest;
import com.trillionloans.los.model.response.NexusRiskBulkResponse;
import com.trillionloans.los.model.response.m2p.NexusColorCodeResponse;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NexusApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;
  private final Gson gson;
  private static final String GET_COLOR_CODE = "GET_COLOR_CODE";

  public NexusApi(
      @Value("${nexus.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.environment = env;
    this.gson = new GsonBuilder().disableHtmlEscaping().create();
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "nexus", env, kafkaLoggingService, kafkaEventProducerService);
    this.util = new WebClientUtil();
  }

  public Mono<NexusColorCodeResponse> getColorCodeFromNexus(
      String pinCode, String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("nexus.api.get-color-code")))
            .queryParam("pincode", pinCode)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_COLOR_CODE, loanApplicationId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, GET_COLOR_CODE, 3, true, true, eventContext);
    return Mono.deferContextual(
        context -> {
          String traceIdForNexus = context.get("traceId");
          log.info("[{}] calling nexus api with traceId: {}", "NEXUS", traceIdForNexus);
          HttpHeaders httpHeaders = getNexusServiceHeaders(traceIdForNexus);
          httpHeaders.add(
              LOGGER_HEADER, "GET " + environment.getProperty("nexus.api.get-color-code"));
          return webClientFactory
              .getFluxDataWithoutStringSerialization(
                  uri, httpHeaders, NexusColorCodeResponse.class, webClientParameters)
              .switchIfEmpty(
                  Mono.defer(
                      () -> {
                        logSuccess("[]", GET_COLOR_CODE);
                        return Mono.empty();
                      }))
              .next()
              .doOnSuccess(
                  res -> {
                    if (res != null) {
                      logSuccess(res, GET_COLOR_CODE);
                    }
                  });
        });
  }

  public Mono<Object> getRiskParameters(String loanApplicationId, String productId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("nexus.api.fetch-risk-parameters")))
            .buildAndExpand(productId, loanApplicationId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "FETCH_RISK_PARAMETERS", 0, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForNexus =
              context.getOrDefault("traceId", UUID.randomUUID().toString().substring(0, 8));
          log.info("[{}] calling nexus api with traceId: {}", "NEXUS", traceIdForNexus);
          HttpHeaders httpHeaders = getNexusServiceHeaders(traceIdForNexus);
          httpHeaders.add(
              LOGGER_HEADER, "GET " + environment.getProperty("nexus.api.fetch-risk-parameters"));
          return webClientFactory.getDataWithoutStringSerialization(
              uri, httpHeaders, Object.class, webClientParameters);
        });
  }

  public Mono<NexusRiskBulkResponse> getRiskParametersBulk(
      List<String> loanApplicationIds, String productId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("nexus.api.bulk-fetch-risk-parameters")))
            .buildAndExpand(productId)
            .toUriString();
    NexusRiskBulkRequest requestBody = new NexusRiskBulkRequest(loanApplicationIds);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "BULK_FETCH_RISK_PARAMETERS", 0, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForNexus =
              context.getOrDefault("traceId", UUID.randomUUID().toString().substring(0, 8));
          log.info("[{}] calling nexus api with traceId: {}", "NEXUS", traceIdForNexus);
          HttpHeaders httpHeaders = getNexusServiceHeaders(traceIdForNexus);
          httpHeaders.add(
              LOGGER_HEADER,
              "POST " + environment.getProperty("nexus.api.bulk-fetch-risk-parameters"));
          return webClientFactory
              .postDataWithoutStringSerialization(
                  uri, requestBody, httpHeaders, Map.class, webClientParameters)
              .map(map -> new NexusRiskBulkResponse((Map<String, Object>) map));
        });
  }

  private HttpHeaders getNexusServiceHeaders(String traceId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("traceId", traceId);
    return headers;
  }

  private void logSuccess(Object responseBody, String loggerHeader) {
    log.info(LOGGING_RESPONSE, loggerHeader, "NEXUS", gson.toJson(responseBody));
  }
}
