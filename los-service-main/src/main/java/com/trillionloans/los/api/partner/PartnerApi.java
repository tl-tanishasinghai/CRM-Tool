package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.BRE_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.CKYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.CLOSURE_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.DISB_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.E_SIGN_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.FI_STATUS_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.KYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.MANUAL_KYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.OKYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.REJECTION_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.Util;
import com.trillionloans.los.util.WebClientUtil;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Outbound API Call wrapper class for partner APIs. Methods made generic, compatible for calling
 * APIs from all partners
 */
@Service
public class PartnerApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final Util util;
  private final WebClientUtil webClientUtil;

  public PartnerApi(Environment environment, KafkaLoggingService kafkaLoggingService) {
    this.webClientFactory = new WebClientFactoryImpl(environment, kafkaLoggingService, null);
    this.environment = environment;
    this.util = new Util(environment);
    this.webClientUtil = new WebClientUtil();
  }

  public Mono<Object> registerAadhaarXmlCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, KYC_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerDisbursementCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, DISB_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerESignCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, E_SIGN_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerOkycCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, OKYC_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerBreCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, BRE_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerCkycCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, CKYC_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerPartnerCallback(
      Object requestBody,
      String uri,
      String callMethod,
      String partnerCode,
      Integer retryCount,
      String loggerHeader) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, loggerHeader);
  }

  public Mono<Object> registerManualKycCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, MANUAL_KYC_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerRejectionStatusCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, REJECTION_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerFiStatusCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, FI_STATUS_CALLBACK_IDENTIFIER);
  }

  public Mono<Object> registerClosureCallback(
      Object requestBody, String uri, String callMethod, String partnerCode, Integer retryCount) {
    return triggerPutPostPartnerCallWithRequestBody(
        requestBody, uri, callMethod, partnerCode, retryCount, CLOSURE_CALLBACK_IDENTIFIER);
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
    Map<String, String> headersMap = util.getPropertiesByPrefix(partnerCode + ".headers.");
    HttpHeaders httpHeaders = new HttpHeaders();
    for (Map.Entry<String, String> entry : headersMap.entrySet()) {
      httpHeaders.add(entry.getKey(), entry.getValue());
    }
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            partnerCode, loggerHeader, retryCount, true, true, null);
    return switch (callMethod) {
      case "POST" ->
          webClientFactory.postDataWithoutStringSerialization(
              fullUrl, requestBody, httpHeaders, Object.class, webClientParameters);
      case "PUT" ->
          webClientFactory.putDataWithoutStringSerialization(
              fullUrl, requestBody, httpHeaders, Object.class, webClientParameters);
      default ->
          Mono.error(
              new BaseException(
                  SOMETHING_WENT_WRONG, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR));
    };
  }
}
