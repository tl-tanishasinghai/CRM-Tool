package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static java.util.Objects.requireNonNull;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.config.WebClientTimeoutProperties;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.BankVerificationDetailsDTO;
import com.trillionloans.los.model.dto.BankVerificationResponseDTO;
import com.trillionloans.los.model.dto.TransactionResponseDTO;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.response.BankVerificationStatusResponseDTO;
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
public class TransactionApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil util;
  private static final String LOG_LITERAL = "[{}] calling bank transaction api with traceId: {}";
  private final WebClientTimeoutProperties webClientTimeoutProperties;

  /**
   * Constructor for TransactionApi.
   *
   * @param baseUrl The base URL of the transaction service API.
   * @param env The environment containing configuration properties.
   * @param kafkaLoggingService The Kafka logging service for logging purposes.
   */
  public TransactionApi(
      @Value("${transaction-service.api.base-url}") String baseUrl,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      WebClientTimeoutProperties webClientTimeoutProperties) {
    this.environment = env;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "transaction-service", env, kafkaLoggingService, kafkaEventProducerService);
    this.util = new WebClientUtil();
    this.webClientTimeoutProperties = webClientTimeoutProperties;
  }

  public Mono<Object> autoDisburse(Object requestBody, String loanApplicationId, String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("transaction-service.api.auto-disburse")))
            .buildAndExpand()
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.AUTO_DISBURSAL_TRIGGER, loanApplicationId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "AUTO_DISB", 3, true, true, webClientTimeoutProperties.getSmall(), eventContext);
    return Mono.deferContextual(
        context -> {
          String traceIdForTransaction = context.get(TRACE_ID);
          log.info(LOG_LITERAL, "AUTO_DISB_CALL", traceIdForTransaction);
          HttpHeaders httpHeaders = getTransactionServiceHeaders(traceIdForTransaction, null);
          return webClientFactory.postDataWithoutStringSerialization(
              uri, requestBody, httpHeaders, Object.class, webClientParameters);
        });
  }

  public Mono<TransactionResponseDTO> checkTransactionStatus(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("transaction-service.api.get-auto-disburse-status")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.AUTO_DISBURSAL_CHECK, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "CHECK_AUTO_DISB_STATUS", 3, true, true, eventContext);
    return Mono.deferContextual(
        context -> {
          String traceIdForTransaction = context.get(TRACE_ID);
          log.info(LOG_LITERAL, "TRANSACTION_STATUS", traceIdForTransaction);
          HttpHeaders httpHeaders = getTransactionServiceHeaders(traceIdForTransaction, null);
          return webClientFactory.getData(
              uri, httpHeaders, TransactionResponseDTO.class, webClientParameters);
        });
  }

  public Mono<BankVerificationStatusResponseDTO> getBankVerificationStatus(
      String clientId, String bankId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "transaction-service.api.get-bank-verification-status")))
            .buildAndExpand(clientId, bankId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_BANK_VERIFICATION_STATUS", 3, true, true, null);
    return Mono.deferContextual(
        context -> {
          String traceIdForTransaction = context.get(TRACE_ID);
          log.info(LOG_LITERAL, "GET_BANK_VERIFICATION_STATUS", traceIdForTransaction);
          HttpHeaders httpHeaders = getTransactionServiceHeaders(traceIdForTransaction, null);
          return webClientFactory.getData(
              uri, httpHeaders, BankVerificationStatusResponseDTO.class, webClientParameters);
        });
  }

  /**
   * Verifies the bank details by making a POST request to the transaction service API.
   *
   * @param requestBody The details of the bank to be verified.
   * @param leadId The ID of the lead.
   * @param partnerId The ID of the partner.
   * @return A Mono containing the response from the transaction service API.
   */
  public Mono<BankVerificationResponseDTO> verifyBank(
      BankVerificationDetailsDTO requestBody,
      String leadId,
      String partnerId,
      String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("transaction-service.api.verify-bank")))
            .buildAndExpand(leadId, loanApplicationId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.BANK_VERIFICATION, loanApplicationId, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "BANK_VERIFICATION", 1, true, true, eventContext);

    return Mono.deferContextual(
        context -> {
          String traceIdForTransaction = context.get("traceId");
          log.info(LOG_LITERAL, "TRANSACTION", traceIdForTransaction);
          HttpHeaders httpHeaders = getTransactionServiceHeaders(traceIdForTransaction, partnerId);
          return webClientFactory.postDataWithoutStringSerialization(
              uri,
              requestBody,
              httpHeaders,
              BankVerificationResponseDTO.class,
              webClientParameters);
        });
  }

  /**
   * Constructs the HTTP headers required for making API calls to the transaction service.
   *
   * @param partnerId The ID of the partner.
   * @return The HTTP headers with necessary details.
   */
  private HttpHeaders getTransactionServiceHeaders(String traceId, String partnerId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("partnerId", partnerId);
    headers.add("traceId", traceId);
    return headers;
  }
}
