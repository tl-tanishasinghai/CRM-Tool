package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.LOGGER_HEADER;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.request.m2p.M2pKycCallBackWithAmlRequest;
import com.trillionloans.los.model.request.m2p.M2pLoanClosureCallBackRequest;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * API client for LOS to call LSP (Loan Service Platform) when partner callback flows are not
 * configured. When a product does not have REJECTION_CB or CLOSURE_CB flow, the callback is sent to
 * LSP so LSP can consume it.
 */
@Service
@Slf4j
public class LspApi {

  private static final String LSP_REJECTION_LOGGER_HEADER = "LSP_REJECTION_CALLBACK";
  private static final String LSP_CLOSURE_LOGGER_HEADER = "LSP_CLOSURE_CALLBACK";

  private final WebClientFactory webClientFactory;
  private final WebClientUtil webClientUtil;
  private final String baseUrl;
  private final String rejectionCallbackPath;
  private final String closureCallbackPath;

  public LspApi(
      @Value("${lsp.api.base-url:}") String baseUrl,
      @Value("${lsp.api.rejection-callback-uri:/callback/rejection}") String rejectionCallbackPath,
      @Value("${lsp.api.closure-callback-uri:/callback/loan-closure}") String closureCallbackPath,
      @Value("${lsp.api.partner-id-header:}") String partnerIdHeader,
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.baseUrl = baseUrl != null ? baseUrl : "";
    this.rejectionCallbackPath =
        rejectionCallbackPath != null ? rejectionCallbackPath : "/callback/rejection";
    this.closureCallbackPath =
        closureCallbackPath != null ? closureCallbackPath : "/callback/loan-closure";
    this.partnerIdHeaderName = partnerIdHeader != null ? partnerIdHeader : "";
    this.webClientFactory =
        new WebClientFactoryImpl(
            this.baseUrl, "lsp", environment, kafkaLoggingService, kafkaEventProducerService);
    this.webClientUtil = new WebClientUtil();
  }

  /** Default header name required by LSP for callbacks. */
  private static final String DEFAULT_PARTNER_ID_HEADER = "partnerId";

  private final String partnerIdHeaderName;

  /**
   * Posts the rejection callback payload to LSP so LSP can consume it. Used when the partner does
   * not have REJECTION_CB flow configured for the product.
   *
   * @param request the rejection callback request (same payload as partner callback)
   * @param partnerId the partner ID (resolved from product code or partner_code) to send in the
   *     partnerId header
   * @return the response from LSP, or empty if LSP is not configured (base-url blank)
   */
  public Mono<Object> postRejectionCallback(
      M2pKycCallBackWithAmlRequest request, String partnerId) {
    return postCallback(
        rejectionCallbackPath,
        request,
        partnerId,
        LSP_REJECTION_LOGGER_HEADER,
        "rejection",
        request.getLoanApplicationId(),
        request.getClientId());
  }

  /**
   * Posts the loan closure callback payload to LSP. Used when the partner does not have CLOSURE_CB
   * flow configured for the product.
   *
   * @param request the closure callback request (same payload as partner callback)
   * @param partnerId the partner ID (resolved from product key or partner_code) to send in the
   *     partnerId header
   * @return the response from LSP, or empty if LSP is not configured (base-url blank)
   */
  public Mono<Object> postClosureCallback(M2pLoanClosureCallBackRequest request, String partnerId) {
    return postCallback(
        closureCallbackPath,
        request,
        partnerId,
        LSP_CLOSURE_LOGGER_HEADER,
        "closure",
        request.getLoanApplicationId(),
        request.getClientId());
  }

  private Mono<Object> postCallback(
      String callbackPath,
      Object request,
      String partnerId,
      String logHeader,
      String callbackType,
      String loanApplicationId,
      String clientId) {
    if (baseUrl == null || baseUrl.isBlank()) {
      log.warn(
          "[{}] LSP base-url is not configured; skipping sending {} callback to LSP for"
              + " loanApplicationId={}",
          logHeader,
          callbackType,
          loanApplicationId);
      return Mono.just(new Object());
    }

    String uri = callbackPath.startsWith("/") ? callbackPath : "/" + callbackPath;
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters("lsp", logHeader, 3, true, true, null);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add(LOGGER_HEADER, "POST " + uri);
    String headerName =
        partnerIdHeaderName != null && !partnerIdHeaderName.isBlank()
            ? partnerIdHeaderName
            : DEFAULT_PARTNER_ID_HEADER;
    if (partnerId != null && !partnerId.isBlank()) {
      log.info(
          "[{}] Adding header {} = {} for LSP {} callback, loanApplicationId = {}",
          logHeader,
          headerName,
          partnerId,
          callbackType,
          loanApplicationId);
      headers.add(headerName, partnerId.trim());
    } else {
      log.warn(
          "[{}] No partnerId available, LSP may reject with missing partnerId header for"
              + " loanApplicationId = {}",
          logHeader,
          loanApplicationId);
    }

    log.info(
        "[{}] Sending {} callback to LSP for loanApplicationId = {}, clientId = {}",
        logHeader,
        callbackType,
        loanApplicationId,
        clientId);

    return webClientFactory
        .postDataWithoutStringSerialization(
            uri, request, headers, Object.class, webClientParameters)
        .doOnSuccess(
            r ->
                log.info(
                    "[{}] {} callback sent to LSP successfully for loanApplicationId = {}",
                    logHeader,
                    callbackType,
                    loanApplicationId))
        .doOnError(
            e ->
                log.error(
                    "[{}] Failed to send {} callback to LSP for loanApplicationId = {} : {}",
                    logHeader,
                    callbackType,
                    loanApplicationId,
                    e.getMessage()));
  }
}
