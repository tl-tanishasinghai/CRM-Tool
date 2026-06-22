package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.DRAWDOWN_BRE;
import static com.trillionloans.los.constant.StringConstants.LOGGER_HEADER;
import static java.util.Objects.requireNonNull;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.model.dto.BreRequest;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.response.creditline.DrawdownBreResponse;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RiskServiceApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;

  public RiskServiceApi(
      @Value("${risk-service.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.environment = env;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "risk-service", env, kafkaLoggingService, kafkaEventProducerService);
    this.util = new WebClientUtil();
  }

  public Mono<Object> registerAsyncBre(String loanId, BreRequest breRequest, String partnerId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("risk-service.api.bre-data")))
            .buildAndExpand(loanId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "RS-BRE-DATA", 0, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForRiskService = context.get("traceId");
          log.info(
              "[{}] calling risk service api with traceId: {}",
              "RISK_SERVICE",
              traceIdForRiskService);
          HttpHeaders httpHeaders = getRiskServiceHeaders(traceIdForRiskService, partnerId);
          httpHeaders.add(
              LOGGER_HEADER, "POST " + environment.getProperty("risk-service.api.bre-data"));
          return webClientFactory.postDataWithoutStringSerialization(
              uri, breRequest, httpHeaders, Object.class, webClientParameters);
        });
  }

  public Mono<DrawdownBreResponse> triggerDrawdownBre(
      String lineId, BreRequest breRequest, String partnerId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("risk-service.api.drawdown-bre-data")))
            .buildAndExpand(lineId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, DRAWDOWN_BRE, 3, false, false, null);

    return Mono.deferContextual(
        context -> {
          String traceIdForRiskService = context.get("traceId");
          HttpHeaders httpHeaders = getRiskServiceHeaders(traceIdForRiskService, partnerId);
          httpHeaders.add(
              LOGGER_HEADER, "POST " + environment.getProperty("risk-service.api.bre-data"));
          return webClientFactory.postDataWithoutStringSerialization(
              uri, breRequest, httpHeaders, DrawdownBreResponse.class, webClientParameters);
        });
  }

  public Mono<?> registerScienapticCtaBre(String loanId, String partnerId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("risk-service.api.bre-cta")))
            .buildAndExpand(loanId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "RS-BRE-CTA", 0, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForRiskService = context.get("traceId");
          log.info(
              "[{}] calling risk service api for scienaptic cta with traceId: {}",
              "RISK_SERVICE",
              traceIdForRiskService);
          HttpHeaders httpHeaders = getRiskServiceHeaders(traceIdForRiskService, partnerId);
          httpHeaders.add(
              LOGGER_HEADER, "POST " + environment.getProperty("risk-service.api.bre-cta"));
          return webClientFactory.postDataWithoutStringSerialization(
              uri, null, httpHeaders, Object.class, webClientParameters);
        });
  }

  private HttpHeaders getRiskServiceHeaders(String traceId, String partnerId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("traceId", traceId);
    headers.add("partnerId", partnerId);
    return headers;
  }
}
