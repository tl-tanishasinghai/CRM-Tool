package com.trillionloans.lms.api.partner;

import static com.trillionloans.lms.constant.StringConstants.LOGGER_HEADER;
import static java.util.Objects.requireNonNull;

import com.trillionloans.lms.api.util.WebClientFactory;
import com.trillionloans.lms.api.util.WebClientFactoryImpl;
import com.trillionloans.lms.config.WebClientTimeoutProperties;
import com.trillionloans.lms.model.dto.internal.WebClientParameters;
import com.trillionloans.lms.model.response.VkycResponseDTO;
import com.trillionloans.lms.service.KafkaLoggingService;
import com.trillionloans.lms.util.WebClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class KycAdaptorApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;
  private final WebClientTimeoutProperties webClientTimeoutProperties;

  public KycAdaptorApi(
      @Value("${kyc-adaptor.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      WebClientTimeoutProperties webClientTimeoutProperties) {
    this.environment = env;
    this.webClientFactory =
        new WebClientFactoryImpl(baseUrl, "kyc_adaptor", env, kafkaLoggingService);
    this.util = new WebClientUtil();
    this.webClientTimeoutProperties = webClientTimeoutProperties;
  }

  public Mono<VkycResponseDTO> InitiateVkyc(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("kyc-adaptor.api.initiate-vkyc")))
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "INITIATE_VKYC", 3, false, webClientTimeoutProperties.getLarge());
    return Mono.deferContextual(
        context -> {
          String traceIdForKycAdaptor = context.get("traceId");
          log.info(
              "[{}] calling kyc adaptor api with traceId: {}", "KYC ADAPTOR", traceIdForKycAdaptor);
          HttpHeaders httpHeaders = getKycAdaptorServiceHeaders(traceIdForKycAdaptor, "1001");
          httpHeaders.add(
              LOGGER_HEADER, "GET " + environment.getProperty("kyc-adaptor.api.initiate-vkyc"));
          return webClientFactory.postData(
              uri, null, httpHeaders, VkycResponseDTO.class, webClientParameters);
        });
  }

  private HttpHeaders getKycAdaptorServiceHeaders(String traceId, String partnerId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("traceId", traceId);
    headers.add("partnerId", partnerId);
    return headers;
  }
}
