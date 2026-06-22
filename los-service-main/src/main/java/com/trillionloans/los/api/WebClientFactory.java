package com.trillionloans.los.api;

import com.trillionloans.los.model.dto.internal.WebClientParameters;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The WebClientFactory interface provides methods for making HTTP requests using WebClient in a
 * reactive programming style. It includes methods for various HTTP operations such as GET, POST,
 * PUT, and uploading documents. Each method supports customization through WebClientParameters and
 * handles different response types.
 */
public interface WebClientFactory {

  <T> Mono<T> getData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> getDataWithoutStringSerialization(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Flux<T> getFluxDataWithoutStringSerialization(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  public <T> Flux<T> getFluxDataWithTypeRef(
      String uri,
      HttpHeaders headers,
      ParameterizedTypeReference<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> postDataWithoutStringSerialization(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> postDataWithForBiddenHandling(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<?> uploadDocumentWithoutStringSerialization(
      String uri,
      MultiValueMap<String, Object> request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> putDataWithoutStringSerialization(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> getDataAsTextResponse(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);
}
