package com.trillionloans.los.api.partner;

import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.request.ckyc.SyntizenCkycSearchRequest;
import com.trillionloans.los.model.response.ckyc.SyntizenCkycSearchResponse;
import com.trillionloans.los.service.ckyc.SyntizenTokenManager;
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

@Slf4j
@Service
public class SyntizenApi {

  private static final String AUTH_KEY_EXPIRED_CODE = "403";

  private final WebClientUtil util;
  private final Environment environment;
  private final WebClientFactory webClientFactory;
  private final String syntizenKey;
  private final SyntizenTokenManager syntizenTokenManager;

  public SyntizenApi(
      @Value("${syntizen.api.baseUrl}") String baseUrl,
      @Value("${syntizen.api.apikey}") String syntizenKey,
      @Value("${syntizen.api.disable-ssl-verification:true}") boolean disableSslVerification,
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      SyntizenTokenManager syntizenTokenManager) {

    this.syntizenKey = syntizenKey;
    this.syntizenTokenManager = syntizenTokenManager;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl,
            "Syntizen",
            environment,
            kafkaLoggingService,
            kafkaEventProducerService,
            disableSslVerification);
    this.environment = environment;
    this.util = new WebClientUtil();
  }

  private HttpHeaders getSyntizenHeaders(String authToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, "application/json");
    headers.add("apikey", syntizenKey);
    headers.add("authkey", authToken);
    return headers;
  }

  public Mono<SyntizenCkycSearchResponse> generateCkycSearchResponse(
      SyntizenCkycSearchRequest ckycSearchRequest) {
    return executeWithTokenRefresh(ckycSearchRequest, 1);
  }

  private Mono<SyntizenCkycSearchResponse> executeWithTokenRefresh(
      SyntizenCkycSearchRequest ckycSearchRequest, int retriesLeft) {

    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("syntizen.api.ckycsearch")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "SYNTIZEN_CKYC_SEARCH", 3, true, true, null);

    return syntizenTokenManager
        .getValidToken()
        .flatMap(
            authToken ->
                webClientFactory.postDataWithoutStringSerialization(
                    uri,
                    ckycSearchRequest,
                    getSyntizenHeaders(authToken),
                    SyntizenCkycSearchResponse.class,
                    webClientParameters))
        .flatMap(
            response -> {
              if (AUTH_KEY_EXPIRED_CODE.equals(response.getRespcode()) && retriesLeft > 0) {
                log.warn(
                    "[SYNTIZEN_CKYC_SEARCH] auth key expired, invalidating and retrying. Retries"
                        + " left: {}",
                    retriesLeft);
                return syntizenTokenManager
                    .invalidateAndRefresh()
                    .then(executeWithTokenRefresh(ckycSearchRequest, retriesLeft - 1));
              }
              return Mono.just(response);
            });
  }
}
