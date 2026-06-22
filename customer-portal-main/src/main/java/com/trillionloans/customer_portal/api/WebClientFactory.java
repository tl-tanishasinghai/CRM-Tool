package com.trillionloans.customer_portal.api;

import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WebClientFactory {

  <T> Mono<T> getData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Flux<T> getFluxData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> post(
      String uri,
      Object body,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> postMultipart(
      String uri,
      MultipartBodyBuilder bodyBuilder,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters params);
}
