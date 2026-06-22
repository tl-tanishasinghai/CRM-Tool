package com.trillionloans.customer_portal.api.internal;

import static com.trillionloans.customer_portal.constant.StringConstants.APPLICATION_JSON;
import static com.trillionloans.customer_portal.constant.StringConstants.CONTENT_TYPE;
import static java.util.Objects.requireNonNull;

import com.trillionloans.customer_portal.api.WebClientFactory;
import com.trillionloans.customer_portal.api.WebClientFactoryImpl;
import com.trillionloans.customer_portal.model.dto.ClientConsentDTO;
import com.trillionloans.customer_portal.model.dto.DueDetailsResponse;
import com.trillionloans.customer_portal.model.dto.ForeclosureDetailsResponseDto;
import com.trillionloans.customer_portal.model.dto.LoanDetailsResponse;
import com.trillionloans.customer_portal.model.dto.RPSResponseWithDPD;
import com.trillionloans.customer_portal.model.dto.TransactionDetailResponse;
import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import com.trillionloans.customer_portal.model.response.RpsResponseDto.ResponseRpsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LmsApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;

  public LmsApi(@Value("${lms.api.base-url}") String baseUrl, Environment env) {
    this.webClientFactory = new WebClientFactoryImpl(baseUrl, "lms", env, ResponseDTO.class);
    this.environment = env;
  }

  public Mono<byte[]> fetchSOA(String loanAccountNumber, String dateOfGeneration) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("lms.api.fetch-SOA.endpoint")))
            .queryParam("dateOfGeneration", dateOfGeneration)
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_SOA", "lms", 0, true, true, false);
    return webClientFactory.getData(uri, getLosHeaders(), byte[].class, webClientParameters);
  }

  public Mono<byte[]> fetchNOC(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("lms.api.fetch-NOC.endpoint")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_NOC", "lms", 0, true, true, false);
    return webClientFactory.getData(uri, getLosHeaders(), byte[].class, webClientParameters);
  }

  public Mono<TransactionDetailResponse> fetchTransactionDetails(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("lms.api.fetch-transactionDetails.endpoint")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_TRANSACTION_DETAILS", "lms", 0, true, true, false);
    return webClientFactory.getData(
        uri, getLosHeaders(), TransactionDetailResponse.class, webClientParameters);
  }

  public Flux<LoanDetailsResponse> fetchAllLoansDetails(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("lms.api.fetch-allLoanDetails.endpoint")))
            .buildAndExpand(leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_ALL_LOAN_DETAILS", "lms", 0, true, true, false);
    return webClientFactory.getFluxData(
        uri, getLosHeaders(), LoanDetailsResponse.class, webClientParameters);
  }

  public Mono<RPSResponseWithDPD> fetchRPSWithDPD(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("lms.api.fetch-repayment-schedule-with-dpd.endpoint")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_RPS", "lms", 0, true, true, false);
    return webClientFactory.getData(
        uri, getLosHeaders(), RPSResponseWithDPD.class, webClientParameters);
  }

  public Mono<DueDetailsResponse> fetchDueAsOnDate(String loanAccountNumber, String date) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("lms.api.fetch-due-as-on-date.endpoint")))
            .buildAndExpand(loanAccountNumber, date)
            .toUriString();

    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_DUE_AS_ON_DATE", "lms", 0, true, true, false);
    return webClientFactory.getData(
        uri, getLosHeaders(), DueDetailsResponse.class, webClientParameters);
  }

  public Mono<ResponseRpsDTO> fetchRPS(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("lms.api.fetch-RPS.endpoint")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_RPS", "lms", 0, true, true, false);
    return webClientFactory.getData(
        uri, getLosHeaders(), ResponseRpsDTO.class, webClientParameters);
  }

  public Mono<Object> getConsentByClientId(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("lms.api.get-consent.endpoint")))
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("GET_CONSENT", "lms", 0, true, true, false);
    return webClientFactory.getData(uri, getLosHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> saveConsentByClientId(String clientId, ClientConsentDTO clientConsentDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("lms.api.save-consent.endpoint")))
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("SAVE_CONSENT", "lms", 0, true, true, false);
    return webClientFactory.post(
        uri, clientConsentDTO, getLosHeaders(), Object.class, webClientParameters);
  }

  public Mono<ForeclosureDetailsResponseDto> getForeclosureDetails(String loanAccountNumber, String transactionDate,
                                                                   Boolean isTotalOutstandingInterest, Boolean includePreClosureReason) {
    String uri =
      UriComponentsBuilder.fromUriString(
          requireNonNull(environment.getProperty("lms.api.get-foreclosure.endpoint")))
        .queryParam("transactionDate", transactionDate)
        .queryParam("isTotalOutstandingInterest", isTotalOutstandingInterest)
        .queryParam("includePreClosureReason", includePreClosureReason)
        .buildAndExpand(loanAccountNumber)
        .toUriString();
    WebClientParameters webClientParameters =
      new WebClientParameters("GET_FORECLOSURE", "lms", 3, true, true, false);
    return webClientFactory.getData(
      uri, getLosHeaders(), ForeclosureDetailsResponseDto.class, webClientParameters);
  }

  private HttpHeaders getLosHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, APPLICATION_JSON);
    return headers;
  }
}
