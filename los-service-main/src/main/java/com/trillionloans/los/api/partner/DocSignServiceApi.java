package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static java.util.Objects.requireNonNull;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.model.dto.internal.DocDetailRequest;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.response.SaveLoanDocResponseDto;
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
public class DocSignServiceApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;

  public DocSignServiceApi(
      @Value("${doc-sign-service.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.environment = env;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "doc-sign-service", env, kafkaLoggingService, kafkaEventProducerService);
    this.util = new WebClientUtil();
  }

  public Mono<SaveLoanDocResponseDto> saveLoanDocDetails(
      String leadId, String clientId, DocDetailRequest docDetailRequest, String partnerId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("doc-sign-service.api.doc-details")))
            .buildAndExpand(clientId, leadId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "DSS-DOC-DETAILS", 3, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForDocSignService = context.get(TRACE_ID);
          log.info(
              "[{}] calling doc sign service api with traceId: {}",
              "DOC_SIGN_SERVICE",
              traceIdForDocSignService);
          HttpHeaders httpHeaders = getDocSignServiceHeaders(traceIdForDocSignService, partnerId);
          return webClientFactory.postDataWithoutStringSerialization(
              uri,
              docDetailRequest,
              httpHeaders,
              SaveLoanDocResponseDto.class,
              webClientParameters);
        });
  }

  private HttpHeaders getDocSignServiceHeaders(String traceId, String partnerId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("traceId", traceId);
    headers.add("partnerId", partnerId);
    return headers;
  }
}
