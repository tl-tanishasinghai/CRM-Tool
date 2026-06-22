package com.trillionloans.los.api.partner;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.AssurekitCreatePlanRequest;
import com.trillionloans.los.model.dto.AssurekitCreatePlanResponse;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AssurekitApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil webClientUtil;
  private final String authToken;
  private static final String CONTENT_TYPE = "Content-Type";

  public AssurekitApi(
      @Value("${assurekit.api.base-url}") String baseUrl,
      @Value("${assurekit.auth.token}") String authToken,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      Environment env) {
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "AssureKit", env, kafkaLoggingService, kafkaEventProducerService);
    this.authToken = authToken;
    this.environment = env;
    this.webClientUtil = new WebClientUtil();
  }

  public Mono<AssurekitCreatePlanResponse> createPlan(AssurekitCreatePlanRequest request) {

    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("assurekit.api.create-plan")))
            .toUriString();

    EventContext eventContext =
        new EventContext(Event.INSURANCE_PLAN_CREATION, request.getLoanId());
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, "ASSUREKIT_CREATE_PLAN", 0, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, request, getHeaders(), AssurekitCreatePlanResponse.class, webClientParameters);
  }

  private HttpHeaders getHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, "application/json");
    headers.add("Authorization", "Basic " + authToken);
    headers.add("accept", "application/json");
    return headers;
  }
}
