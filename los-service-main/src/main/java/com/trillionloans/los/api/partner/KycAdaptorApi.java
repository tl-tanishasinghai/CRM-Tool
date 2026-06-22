package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.LOGGER_HEADER;
import static java.util.Objects.requireNonNull;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.response.BankUnderwritingGstUnderwritingResponseDTO;
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
public class KycAdaptorApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;

  public KycAdaptorApi(
      @Value("${kyc-adaptor.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.environment = env;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "kyc_adaptor", env, kafkaLoggingService, kafkaEventProducerService);
    this.util = new WebClientUtil();
  }

  public Mono<BankUnderwritingGstUnderwritingResponseDTO> getBankUnderwritingAndGstUnderwritingData(
      String loanId, String partnerId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("kyc-adaptor.api.get-bu-gu")))
            .buildAndExpand(loanId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_BU_GU", 0, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForKycAdaptor = context.get("traceId");
          log.info(
              "[{}] calling kyc adaptor api with traceId: {}", "KYC ADAPTOR", traceIdForKycAdaptor);
          HttpHeaders httpHeaders = getKycAdaptorServiceHeaders(traceIdForKycAdaptor, partnerId);
          httpHeaders.add(
              LOGGER_HEADER, "GET " + environment.getProperty("kyc-adaptor.api.get-bu-gu"));
          return webClientFactory.getDataWithoutStringSerialization(
              uri,
              httpHeaders,
              BankUnderwritingGstUnderwritingResponseDTO.class,
              webClientParameters);
        });
  }

  private HttpHeaders getKycAdaptorServiceHeaders(String traceId, String partnerId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("traceId", traceId);
    headers.add("partnerId", partnerId);
    return headers;
  }

  public Mono<Object> vcipNotification(
      String clientId,
      String loanAppId,
      String partnerName,
      String partnerId,
      String productCode,
      String riskCategory) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("kyc-adaptor.api.vcip.notification")))
            .queryParam("loanAppId", loanAppId)
            .queryParam("partnerName", partnerName)
            .queryParam("partnerId", partnerId)
            .queryParam("productCode", productCode)
            .queryParam("riskCategory", riskCategory)
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "POST_VCIP_NOTIFICATION", 3, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForKycAdaptor = context.get("traceId");
          log.info(
              "[{}] calling kyc adaptor api with traceId: {}", "KYC ADAPTOR", traceIdForKycAdaptor);
          HttpHeaders httpHeaders = getKycAdaptorServiceHeaders(traceIdForKycAdaptor, null);
          httpHeaders.add(
              LOGGER_HEADER,
              "POST " + environment.getProperty("kyc-adaptor.api.vcip.notification"));
          return webClientFactory.postDataWithoutStringSerialization(
              uri, null, httpHeaders, Object.class, webClientParameters);
        });
  }
}
