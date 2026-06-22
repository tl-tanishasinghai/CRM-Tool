package com.trillionloans.los.api.partner;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class ScienapticApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;

  public ScienapticApi(
      @Value("${scienaptic.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "SCIENAPTIC", env, kafkaLoggingService, kafkaEventProducerService);
    this.environment = env;
    this.util = new WebClientUtil();
  }

  public Mono<Object> postBureauData(
      Object requestBody, String productCode, String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("scienaptic.api." + productCode)))
            .buildAndExpand()
            .toUriString();
    EventContext eventContext = new EventContext(Event.BRE_SCIENAPTIC_CALL, loanApplicationId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "BRE_SCIENAPTIC_CALL", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, requestBody, getScienapticHeaders(productCode), Object.class, webClientParameters);
  }

  private HttpHeaders getScienapticHeaders(String productCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add(
        "Authorization",
        "Bearer "
            + Objects.requireNonNull(
                environment.getProperty("scienaptic.auth.token." + productCode)));
    return headers;
  }
}
