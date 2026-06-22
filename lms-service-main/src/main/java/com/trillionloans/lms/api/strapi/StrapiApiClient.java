package com.trillionloans.lms.api.strapi;

import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.trillionloans.lms.api.util.WebClientFactory;
import com.trillionloans.lms.api.util.WebClientFactoryImpl;
import com.trillionloans.lms.config.WebClientTimeoutProperties;
import com.trillionloans.lms.model.dto.internal.WebClientParameters;
import com.trillionloans.lms.model.dto.strapi.StrapiChargesConfigDto;
import com.trillionloans.lms.model.dto.strapi.StrapiPagedResponse;
import com.trillionloans.lms.model.dto.strapi.StrapiProductConfigDto;
import com.trillionloans.lms.service.KafkaLoggingService;
import com.trillionloans.lms.util.WebClientUtil;
import java.lang.reflect.Type;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class StrapiApiClient {

  private static final String LOGGER = "STRAPI";

  private final WebClientFactory webClientFactory;
  private final Gson gson;
  private final String apiToken;
  private final Environment environment;
  private final WebClientUtil util;
  private final WebClientTimeoutProperties webClientTimeoutProperties;

  public StrapiApiClient(
      @Value("${strapi.api.base-url}") String baseUrl,
      @Value("${strapi.api.token}") String apiToken,
      Gson gson,
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      WebClientTimeoutProperties webClientTimeoutProperties) {
    this.apiToken = apiToken;
    this.gson = gson;
    this.environment = environment;
    this.util = new WebClientUtil();
    this.webClientTimeoutProperties = webClientTimeoutProperties;
    this.webClientFactory =
        new WebClientFactoryImpl(baseUrl, LOGGER, environment, kafkaLoggingService);
  }

  public Mono<StrapiProductConfigDto> findProductConfigByCode(String productCode) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("strapi.api.product-config-uri")))
            .buildAndExpand(productCode)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "STRAPI_FETCH_PRODUCT_CONFIG", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getData(uri, getStrapiHeaders(), String.class, webClientParameters)
        .flatMap(
            body -> {
              Type type = new TypeToken<StrapiPagedResponse<StrapiProductConfigDto>>() {}.getType();
              StrapiPagedResponse<StrapiProductConfigDto> response = gson.fromJson(body, type);
              List<StrapiProductConfigDto> data = response.getData();
              if (data == null || data.isEmpty()) {
                log.warn(
                    "[{}] no product-configuration found for product_code={}", LOGGER, productCode);
                return Mono.empty();
              }
              return Mono.just(data.get(0));
            });
  }

  public Mono<StrapiChargesConfigDto> findChargesConfigByCode(String productCode) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("strapi.api.charges-config-by-code-uri")))
            .buildAndExpand(productCode)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "STRAPI_FETCH_CHARGES_CONFIG", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getData(uri, getStrapiHeaders(), String.class, webClientParameters)
        .flatMap(
            body -> {
              Type type = new TypeToken<StrapiPagedResponse<StrapiChargesConfigDto>>() {}.getType();
              StrapiPagedResponse<StrapiChargesConfigDto> response = gson.fromJson(body, type);
              List<StrapiChargesConfigDto> data = response.getData();
              if (data == null || data.isEmpty()) {
                log.warn("[{}] no charges-config found for product_code={}", LOGGER, productCode);
                return Mono.empty();
              }
              return Mono.just(data.get(0));
            });
  }

  public Mono<List<StrapiChargesConfigDto>> findAllChargesConfigs() {
    String uri = requireNonNull(environment.getProperty("strapi.api.all-charges-config-uri"));
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "STRAPI_FETCH_ALL_CHARGES_CONFIG",
            0,
            true,
            webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getData(uri, getStrapiHeaders(), String.class, webClientParameters)
        .map(
            body -> {
              Type type = new TypeToken<StrapiPagedResponse<StrapiChargesConfigDto>>() {}.getType();
              StrapiPagedResponse<StrapiChargesConfigDto> response = gson.fromJson(body, type);
              return response.getData() != null ? response.getData() : List.of();
            });
  }

  private HttpHeaders getStrapiHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(apiToken);
    return headers;
  }
}
