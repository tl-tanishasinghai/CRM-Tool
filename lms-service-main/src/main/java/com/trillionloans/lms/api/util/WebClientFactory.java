package com.trillionloans.lms.api.util;

import com.trillionloans.lms.model.dto.internal.WebClientParameters;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface defining methods for creating reactive WebClient instances to interact with external
 * APIs.
 *
 * @author sofiyan
 */
public interface WebClientFactory {

  /**
   * Retrieves data from an external API using a GET request.
   *
   * @param uri The URI of the external API endpoint.
   * @param headers The HttpHeaders to be included in the request.
   * @param responseType The class representing the expected response type.
   * @return A Mono containing the retrieved data.
   */
  <T> Mono<T> getData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  /**
   * Sends data to an external API using a POST request.
   *
   * @param uri The URI of the external API endpoint.
   * @param request The request object to be sent in the POST request.
   * @param headers The HttpHeaders to be included in the request.
   * @param responseType The class representing the expected response type.
   * @return A Mono containing the response data from the external API.
   */
  <T> Mono<T> postData(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  /**
   * Updates data on an external API using a PUT request.
   *
   * @param uri The URI of the external API endpoint.
   * @param request The request object to be sent in the PUT request.
   * @param headers The HttpHeaders to be included in the request.
   * @param responseType The class representing the expected response type.
   * @return A Mono containing the updated data from the external API.
   */
  <T> Mono<T> putData(
      String uri,
      Object request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Mono<T> uploadDocument(
      String uri,
      MultiValueMap<String, Object> request,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);

  <T> Flux<T> getFluxData(
      String uri,
      HttpHeaders headers,
      Class<T> responseType,
      WebClientParameters webClientParameters);
}
