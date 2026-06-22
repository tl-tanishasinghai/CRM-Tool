package com.trillionloans.lms.api.partner;

import static com.trillionloans.lms.constant.StringConstants.SOMETHING_WENT_WRONG;

import com.trillionloans.lms.api.util.WebClientFactory;
import com.trillionloans.lms.api.util.WebClientFactoryImpl;
import com.trillionloans.lms.config.WebClientTimeoutProperties;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.dto.internal.WebClientParameters;
import com.trillionloans.lms.service.KafkaLoggingService;
import com.trillionloans.lms.util.Util;
import com.trillionloans.lms.util.WebClientUtil;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class PartnerApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final Util util;
  private final WebClientUtil webClientUtil;
  private final WebClientTimeoutProperties webClientTimeoutProperties;

  public PartnerApi(
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      WebClientTimeoutProperties webClientTimeoutProperties) {
    this.webClientFactory = new WebClientFactoryImpl(environment, kafkaLoggingService);
    this.environment = environment;
    this.util = new Util(environment);
    this.webClientUtil = new WebClientUtil();
    this.webClientTimeoutProperties = webClientTimeoutProperties;
  }

  private Mono<Object> triggerPutPostPartnerCallWithRequestBody(
      Object requestBody,
      String uri,
      String callMethod,
      String partnerCode,
      Integer retryCount,
      String loggerHeader) {
    String fullUrl =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty(partnerCode + ".api.base-url")))
            .path(uri)
            .buildAndExpand()
            .toUriString();
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            partnerCode, loggerHeader, retryCount, true, webClientTimeoutProperties.getSmall());
    return switch (callMethod) {
      case "POST" ->
          webClientFactory.postData(
              fullUrl,
              requestBody,
              getPartnerHeaders(partnerCode),
              Object.class,
              webClientParameters);
      case "PUT" ->
          webClientFactory.putData(
              fullUrl,
              requestBody,
              getPartnerHeaders(partnerCode),
              Object.class,
              webClientParameters);
      default ->
          Mono.error(
              new BaseException(
                  SOMETHING_WENT_WRONG, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR));
    };
  }

  private HttpHeaders getPartnerHeaders(String partnerCode) {
    Map<String, String> headersMap = util.getPropertiesByPrefix(partnerCode + ".headers.");
    HttpHeaders httpHeaders = new HttpHeaders();
    for (Map.Entry<String, String> entry : headersMap.entrySet()) {
      httpHeaders.add(entry.getKey(), entry.getValue());
    }
    return httpHeaders;
  }
}
