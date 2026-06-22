package com.trillionloans.customer_portal.api.internal;

import static com.trillionloans.customer_portal.constant.StringConstants.APPLICATION_JSON;
import static com.trillionloans.customer_portal.constant.StringConstants.CONTENT_TYPE;
import static java.util.Objects.requireNonNull;

import com.trillionloans.customer_portal.api.WebClientFactory;
import com.trillionloans.customer_portal.api.WebClientFactoryImpl;
import com.trillionloans.customer_portal.model.dto.ClientDetailsCpResponseDto;
import com.trillionloans.customer_portal.model.dto.DocumentDetailResponse;
import com.trillionloans.customer_portal.model.dto.LeadDetailsResponse;
import com.trillionloans.customer_portal.model.dto.LeadIdResponse;
import com.trillionloans.customer_portal.model.dto.LoanApplicationIdResponse;
import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LosApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;

  public LosApi(@Value("${los.api.base-url}") String baseUrl, Environment env) {
    this.webClientFactory = new WebClientFactoryImpl(baseUrl, "los", env, ResponseDTO.class);
    this.environment = env;
  }

  public Mono<LeadDetailsResponse> fetchLeadDetails(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("los.api.leadId-details.endpoint")))
            .buildAndExpand(leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_LEAD_DETAILS", "los", 0, true, true, false);
    return webClientFactory.getData(
        uri, getLosHeaders(), LeadDetailsResponse.class, webClientParameters);
  }

  public Flux<LeadIdResponse> fetchLeadDetailAgainstMobileNumber(String mobileNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("los.api.get-lead-against-mobileNumber.endpoint")))
            .buildAndExpand(mobileNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("GET_LEAD_INFO", "los", 0, true, true, false);
    return webClientFactory.getFluxData(
        uri, getLosHeaders(), LeadIdResponse.class, webClientParameters);
  }

  public Flux<LeadIdResponse> fetchLeadDetailAgainstMobileNumberAndDOB(
      String mobileNumber, String dateOfBirth) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("los.api.get-lead-against-mobileNumber-dob.endpoint")))
            .buildAndExpand(mobileNumber, dateOfBirth)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("GET_LEAD_INFO_DOB", "los", 0, true, true, false);
    return webClientFactory.getFluxData(
        uri, getLosHeaders(), LeadIdResponse.class, webClientParameters);
  }

  public Flux<LeadIdResponse> fetchLeadDetailAgainstMobileNumberDOBAndPAN(
      String mobileNumber, String dateOfBirth, String panLast4Digits) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "los.api.get-lead-against-mobileNumber-dob-pan.endpoint")))
            .buildAndExpand(mobileNumber, dateOfBirth, panLast4Digits)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("GET_LEAD_INFO_DOB_PAN", "los", 0, true, true, false);
    return webClientFactory.getFluxData(
        uri, getLosHeaders(), LeadIdResponse.class, webClientParameters);
  }

  public Mono<byte[]> fetchDocumentAgainstId(String loanApplicationId, String documentId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("los.api.get-document.endpoint")))
            .buildAndExpand(loanApplicationId, documentId)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("GET_DOCUMENT", "los", 0, true, true, false);
    return webClientFactory.getData(uri, getLosHeaders(), byte[].class, webClientParameters);
  }

  public Flux<DocumentDetailResponse> fetchAllDocumentDetailsAgainstLoanAppId(
      String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("los.api.get-all-documents.endpoint")))
            .buildAndExpand(loanApplicationId)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("GET_DOCUMENT_LIST", "los", 0, true, true, false);
    return webClientFactory.getFluxData(
        uri, getLosHeaders(), DocumentDetailResponse.class, webClientParameters);
  }

  public Flux<LoanApplicationIdResponse> fetchAllLoanApplicationIdsAgainstLeadId(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("los.api.get-all-loanApplicationIds.endpoint")))
            .buildAndExpand(leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters("GET_LOAN_APPLICATION_LIST", "los", 0, true, true, false);
    return webClientFactory.getFluxData(
        uri, getLosHeaders(), LoanApplicationIdResponse.class, webClientParameters);
  }

  private HttpHeaders getLosHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, APPLICATION_JSON);
    return headers;
  }

  public Mono<?> getCpRpsLeadData(String leadId, String accountNo) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("los.api.fetch-cpRpsLoanDetails.endpoint")))
            .buildAndExpand(leadId, accountNo)
            .toUriString();

    WebClientParameters webClientParameters =
        new WebClientParameters("FETCH_CP_RPS_LOAN_DETAILS", "lms", 0, true, true, false);

    return webClientFactory.getData(
        uri, getLosHeaders(), ClientDetailsCpResponseDto.class, webClientParameters);
  }
}
