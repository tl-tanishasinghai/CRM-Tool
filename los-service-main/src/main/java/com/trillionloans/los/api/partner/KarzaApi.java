package com.trillionloans.los.api.partner;

import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.KarzaNameSimilarityRequest;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.request.KarazaPanAadhaarLinkCheckDTO;
import com.trillionloans.los.model.request.KarzaFaceSimilarityRequest;
import com.trillionloans.los.model.request.KarzaNameSimilarityResponse;
import com.trillionloans.los.model.request.KarzaPanAuthenticateRequest;
import com.trillionloans.los.model.request.KarzaPanAuthenticateResponse;
import com.trillionloans.los.model.request.ckyc.KarzaCkycSearchRequest;
import com.trillionloans.los.model.response.KarzaFaceSimilarityResponse;
import com.trillionloans.los.model.response.KarzaPanAadhaarLinkResponseDTO;
import com.trillionloans.los.model.response.ckyc.KarzaCkycSearchResponse;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class KarzaApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;
  private final String karzaKey;

  public KarzaApi(
      @Value("${karza.key}") String karzaKey,
      @Value("${karza.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {

    this.karzaKey = karzaKey;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "KARZA", env, kafkaLoggingService, kafkaEventProducerService);
    this.environment = env;
    this.util = new WebClientUtil();
  }

  public Mono<KarzaPanAadhaarLinkResponseDTO> getPanAadhaarLinkStatus(
      KarazaPanAadhaarLinkCheckDTO requestBody, String loanApplicationId, String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("karza.api.pan-aadhaar")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.GET_PAN_AADHAAR_LINK_STATUS, loanApplicationId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_PAN_AADHAAR_LINK_STATUS", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        requestBody,
        getKarzaHeaders(),
        KarzaPanAadhaarLinkResponseDTO.class,
        webClientParameters);
  }

  public Mono<KarzaPanAuthenticateResponse> authenticatePan(
      KarzaPanAuthenticateRequest requestBody, String clientId, String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("karza.api.pan-authenticate")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.KARZA_PAN_VERIFY, loanApplicationId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "KARZA_PAN_AUTHENTICATE", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        requestBody,
        getKarzaHeaders(),
        KarzaPanAuthenticateResponse.class,
        webClientParameters);
  }

  public Mono<KarzaNameSimilarityResponse> checkNameSimilarity(
      KarzaNameSimilarityRequest requestBody,
      String clientId,
      String loanId,
      String requestType,
      String eventTypeOverride) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("karza.api.name-similarity")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.KARZA_NAME_SIMILARITY_CHECK, loanId, clientId);
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("requestType", requestType);
    if (eventTypeOverride != null) {
      metadata.put("override_event_type", eventTypeOverride);
    }
    eventContext.setMetadata(metadata);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "KARZA_NAME_SIMILARITY", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        requestBody,
        getKarzaHeaders(),
        KarzaNameSimilarityResponse.class,
        webClientParameters);
  }

  public Mono<KarzaFaceSimilarityResponse> checkFaceSimilarity(
      KarzaFaceSimilarityRequest karzaFaceSimilarityRequest, String clientId, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("karza.api.face-similarity")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.KARZA_FACE_SIMILARITY_CHECK, loanId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "KARZA_FACE_SIMILARITY", 3, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        karzaFaceSimilarityRequest,
        getKarzaHeaders(),
        KarzaFaceSimilarityResponse.class,
        webClientParameters);
  }

  public Mono<KarzaCkycSearchResponse> fetchPanProfile(KarzaCkycSearchRequest requestBody) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("karza.api.pan-profile")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "KARZA_PAN_PROFILE", 3, true, true, null);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, requestBody, getKarzaHeaders(), KarzaCkycSearchResponse.class, webClientParameters);
  }

  private HttpHeaders getKarzaHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, "application/json");
    headers.add("x-karza-key", karzaKey);
    return headers;
  }
}
