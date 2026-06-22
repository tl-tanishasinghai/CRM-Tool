package com.trillionloans.customer_portal.api.internal;

import com.trillionloans.customer_portal.api.WebClientFactory;
import com.trillionloans.customer_portal.api.WebClientFactoryImpl;
import com.trillionloans.customer_portal.constant.StringConstants;
import com.trillionloans.customer_portal.model.dto.CollectionDetailsResponse;
import com.trillionloans.customer_portal.model.dto.CollectionStatusResponse;
import com.trillionloans.customer_portal.model.dto.CreatePaymentRequest;
import com.trillionloans.customer_portal.model.dto.CreatePaymentResponse;
import com.trillionloans.customer_portal.model.dto.LatestCollectionResponse;
import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.trillionloans.customer_portal.constant.StringConstants.COLLECTION_SERVICE;

@Service
public class CollectionApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;

  private static final String CREATE_PAYMENT_ENDPOINT = "collection-service.api.create-payment.endpoint";
  private static final String COLLECTION_STATUS_ENDPOINT = "collection-service.api.collection-status.endpoint";
  private static final String COLLECTION_DETAILS_ENDPOINT = "collection-service.api.collection-details.endpoint";
  private static final String LATEST_COLLECTION_ENDPOINT = "collection-service.api.latest-collection.endpoint";

  public CollectionApi(@Value("${collection.api.base-url}") String baseUrl, Environment env) {
    this.webClientFactory = new WebClientFactoryImpl(baseUrl, COLLECTION_SERVICE, env, ResponseDTO.class);
    this.environment = env;
  }

  public Mono<CreatePaymentResponse> createPayment(String clientId, String loanAccountNumber, CreatePaymentRequest createPaymentRequest) {
    String uri =
      UriComponentsBuilder.fromUriString(
          Objects.requireNonNull(environment.getProperty(CREATE_PAYMENT_ENDPOINT)))
        .buildAndExpand(clientId, loanAccountNumber)
        .toUriString();
    WebClientParameters webClientParameters =
      new WebClientParameters(
        "CREATE_PAYMENT", COLLECTION_SERVICE, 0, true, true, false);
    return webClientFactory.post(
      uri, createPaymentRequest, getHeaders(), CreatePaymentResponse.class, webClientParameters);
  }

  public Mono<CollectionStatusResponse> getCollectionStatus(String loanAccountNumber, String collectionId) {
    String uri =
      UriComponentsBuilder.fromUriString(
          Objects.requireNonNull(environment.getProperty(COLLECTION_STATUS_ENDPOINT)))
        .buildAndExpand(loanAccountNumber, collectionId)
        .toUriString();
    WebClientParameters webClientParameters =
      new WebClientParameters(
        "COLLECTION_STATUS", COLLECTION_SERVICE, 3, true, true, false);
    return webClientFactory.getData(
      uri, getHeaders(), CollectionStatusResponse.class, webClientParameters);
  }

  public Mono<CollectionDetailsResponse> getCollectionDetails(String loanAccountNumber) {
    String uri =
      UriComponentsBuilder.fromUriString(
          Objects.requireNonNull(environment.getProperty(COLLECTION_DETAILS_ENDPOINT)))
        .buildAndExpand(loanAccountNumber)
        .toUriString();
    WebClientParameters webClientParameters =
      new WebClientParameters(
        "GET_COLLECTION_DETAILS", COLLECTION_SERVICE, 3, true, true, false);
    return webClientFactory.getData(
      uri, getHeaders(), CollectionDetailsResponse.class, webClientParameters);
  }

  public Mono<LatestCollectionResponse> getLatestCollection(String loanAccountNumber) {
    String uri =
      UriComponentsBuilder.fromUriString(
          Objects.requireNonNull(environment.getProperty(LATEST_COLLECTION_ENDPOINT)))
        .buildAndExpand(loanAccountNumber)
        .toUriString();
    WebClientParameters webClientParameters =
      new WebClientParameters(
        "GET_LATEST_COLLECTION", COLLECTION_SERVICE, 3, true, true, false);
    return webClientFactory.getData(
      uri, getHeaders(), LatestCollectionResponse.class, webClientParameters);
  }

  private HttpHeaders getHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}