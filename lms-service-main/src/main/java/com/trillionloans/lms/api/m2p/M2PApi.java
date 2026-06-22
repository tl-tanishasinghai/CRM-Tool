package com.trillionloans.lms.api.m2p;

import static com.trillionloans.lms.constant.StringConstants.CREDIT_LINE_REPAYMENT_LOG_HEADER;
import static com.trillionloans.lms.constant.StringConstants.CREDIT_LINE_TRANSACTION_DETAILS;
import static com.trillionloans.lms.constant.StringConstants.LOAN_AGREEMENT_NOTIFICATIONS;
import static com.trillionloans.lms.constant.StringConstants.LOGGING_RESPONSE;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import com.google.gson.Gson;
import com.trillionloans.lms.api.util.WebClientFactory;
import com.trillionloans.lms.api.util.WebClientFactoryImpl;
import com.trillionloans.lms.config.WebClientTimeoutProperties;
import com.trillionloans.lms.model.dto.EmiReportRow;
import com.trillionloans.lms.model.dto.M2pLoanForeClosureDTO;
import com.trillionloans.lms.model.dto.M2pPartRepaymentRequestDTO;
import com.trillionloans.lms.model.dto.internal.M2pCreditLineForeClosurePayload;
import com.trillionloans.lms.model.dto.internal.WebClientParameters;
import com.trillionloans.lms.model.dto.restructure.PartialWaiveChargeDTO;
import com.trillionloans.lms.model.dto.restructure.RestructureApprovalDetailsDTO;
import com.trillionloans.lms.model.dto.restructure.RetrieveLoanResponseDTO;
import com.trillionloans.lms.model.dto.restructure.TentativeRpsResponseDTO;
import com.trillionloans.lms.model.request.AdvancedReportRequestDTO;
import com.trillionloans.lms.model.request.CollectChargeRequest;
import com.trillionloans.lms.model.request.M2PCreditLineMarkRepaymentRequest;
import com.trillionloans.lms.model.request.DocumentUploadRequest;
import com.trillionloans.lms.model.request.MarkCollectionRequest;
import com.trillionloans.lms.model.request.RejectLoanRequest;
import com.trillionloans.lms.model.request.SaveChargesRequest;
import com.trillionloans.lms.model.request.WaiveChargeRequest;
import com.trillionloans.lms.model.request.restructure.ApproveRescheduleRequest;
import com.trillionloans.lms.model.request.restructure.PartialWaiverRequest;
import com.trillionloans.lms.model.request.restructure.RescheduleInitiateRequest;
import com.trillionloans.lms.model.response.CreditLineMarkRepaymentResponse;
import com.trillionloans.lms.model.response.ForeclosureDetailsResponseDto;
import com.trillionloans.lms.model.response.M2pChargeDetailsDTO;
import com.trillionloans.lms.model.response.M2pCkycRecipientsDetailsDTO;
import com.trillionloans.lms.model.response.M2pClosedLoanDetailsDTO;
import com.trillionloans.lms.model.response.M2pExcessAmountResponse;
import com.trillionloans.lms.model.response.M2pFirstUnpaidTransaction;
import com.trillionloans.lms.model.response.M2pLanDetails;
import com.trillionloans.lms.model.response.M2pLoanAgreementDetailsDTO;
import com.trillionloans.lms.model.response.M2pNewLoansDetailsDTO;
import com.trillionloans.lms.model.response.M2pOpenLoanCountDTO;
import com.trillionloans.lms.model.response.M2pOpenLoanDetailsDTO;
import com.trillionloans.lms.model.response.ReKycEligibleLoanDTO;
import com.trillionloans.lms.model.response.TransactionDetailResponse;
import com.trillionloans.lms.model.response.restructure.ApproveRescheduleResponse;
import com.trillionloans.lms.model.response.restructure.RescheduleInitiateResponse;
import com.trillionloans.lms.service.KafkaLoggingService;
import com.trillionloans.lms.util.WebClientUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service class for interacting with the M2P API.
 *
 * @author sofiyan
 */
@Slf4j
@Service
public class M2PApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final String authToken;
  private final String fineractTenantId;
  private final WebClientUtil util;
  private final WebClientTimeoutProperties webClientTimeoutProperties;
  private final String documentUrl;

  private static final String ASSOCIATIONS = "associations";
  private static final String EXCLUDE = "exclude";
  private static final String FETCH_SPECIFIED_DATA = "isFetchSpecificData";
  private static final String LOCALE = "locale";
  private static final String COMMAND = "command";
  private final Gson gson;

  public M2PApi(
      @Value("${m2p.api.base-url}") String baseUrl,
      Environment environment,
      @Value("${m2p.auth.token}") String authToken,
      @Value("${m2p.fineract-tenant-id}") String fineractTenantId,
      KafkaLoggingService kafkaLoggingService,
      WebClientTimeoutProperties webClientTimeoutProperties,
      @Value("${m2p.api.ml.fetch-document-url}") String documentUrl) {
    this.webClientFactory =
        new WebClientFactoryImpl(baseUrl, "M2P", environment, kafkaLoggingService);
    this.environment = environment;
    this.authToken = authToken;
    this.fineractTenantId = fineractTenantId;
    this.util = new WebClientUtil();
    this.gson = new Gson();
    this.webClientTimeoutProperties = webClientTimeoutProperties;
    this.documentUrl = documentUrl;
  }

  /**
   * Retrieves repayment schedule data for a specific loan from the M2P API.
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the retrieved repayment schedule data.
   */
  public Mono<?> getRepaymentScheduleByLoanAccountNumber(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.repayment-schedule")))
            .queryParam(ASSOCIATIONS, "repaymentSchedule,originalSchedule")
            .queryParam(EXCLUDE, "loanBasicDetails")
            .queryParam(FETCH_SPECIFIED_DATA, true)
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "REPAYMENT_SCHEDULE", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<ForeclosureDetailsResponseDto> getForeclosureDetailsByLoanAccountNumber(
      String loanAccountNumber,
      String transactionDate,
      Boolean isTotalOutstandingInterest,
      Boolean includePreClosureReason) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.foreclosure-details")))
            .queryParam(COMMAND, "foreclosure")
            .queryParam("dateFormat", "dd-MM-yyyy")
            .queryParam("transactionDate", transactionDate)
            .queryParam(LOCALE, "en")
            .queryParam("isTotalOutstandingInterest", isTotalOutstandingInterest)
            .queryParam("includePreclosureReason", includePreClosureReason)
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_FORECLOSURE_DETAILS", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(
        uri, getHeaders(), ForeclosureDetailsResponseDto.class, webClientParameters);
  }

  public Mono<?> postForeClosureRequest(
      String loanAccountNumber, M2pLoanForeClosureDTO foreClosureData) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.execute-foreclosure")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FORECLOSURE_REQUEST", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, foreClosureData, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> markCollection(
      String loanAccountNumber, MarkCollectionRequest markCollectionRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.mark-collection")))
            .queryParam(COMMAND, "repayment")
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "MARK_COLLECTION", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, markCollectionRequest, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getForeclosure(String lineId, String transactionId, String transactionDate) {

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromUriString(
            Objects.requireNonNull(environment.getProperty("m2p.api.ml.foreclosure-details-v2")));
    if (transactionDate != null && !transactionDate.isBlank()) {
      uriBuilder
          .queryParam("dateFormat", "dd-MM-yyyy")
          .queryParam("transactionDate", transactionDate);
    }
    String uri = uriBuilder.buildAndExpand(lineId, transactionId).toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_FORECLOSURE", 0, true, webClientTimeoutProperties.getLarge());

    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> saveCharges(String loanAccountNumber, SaveChargesRequest saveChargesRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.add-charge")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "SAVE_CHARGES", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, saveChargesRequest, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getCharges(String loanAccountNumber, String exclude, Boolean isFetchSpecificData) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.charges")))
            .queryParam(ASSOCIATIONS, "charges")
            .queryParam(EXCLUDE, exclude)
            .queryParam(FETCH_SPECIFIED_DATA, isFetchSpecificData)
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_CHARGES", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Flux<M2pClosedLoanDetailsDTO> getClosedLoansListBasedOnDate(String date) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.reports.get-closed-loans")))
            .queryParam("R_startDate", date)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_CLOSED_LOANS", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getFluxData(uri, getHeaders(), M2pClosedLoanDetailsDTO.class, webClientParameters)
        .collectList()
        .flatMap(
            list -> {
              log.info(LOGGING_RESPONSE, "GET_CLOSED_LOANS", "M2P", gson.toJson(list));
              return Mono.just(list);
            })
        .flatMapMany(Flux::fromIterable);
  }

  public Flux<M2pChargeDetailsDTO> getChargeDetailsAgainstLoanAccount(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.get-charge-details")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_CHARGES", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getFluxData(uri, getHeaders(), M2pChargeDetailsDTO.class, webClientParameters)
        .collectList()
        .flatMap(
            list -> {
              log.info(LOGGING_RESPONSE, "GET_CHARGES", "M2P", gson.toJson(list));
              return Mono.just(list);
            })
        .flatMapMany(Flux::fromIterable);
  }

  public Mono<?> waiveCharge(
      String loanAccountNumber, String chargeId, WaiveChargeRequest waiveChargeRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.waive-charge")))
            .queryParam("resourceType", "charges")
            .buildAndExpand(loanAccountNumber, chargeId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "WAIVE_CHARGES", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, waiveChargeRequest, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> collectCharge(
      String loanAccountNumber, String chargeType, CollectChargeRequest collectChargeRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.collect-charge")))
            .buildAndExpand(loanAccountNumber, chargeType)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "COLLECT_CHARGES", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, collectChargeRequest, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<byte[]> fetchSOAPdf(String loanAccountNumber, String dateOfGeneration) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.fetch-soa")))
            .queryParam("output-type", "PDF")
            .queryParam(LOCALE, "en")
            .queryParam("R_endDate", dateOfGeneration)
            .queryParam("R_loanId", loanAccountNumber)
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_SOA", 0, false, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), byte[].class, webClientParameters);
  }

  public Mono<?> rejectLoan(String loanAccountNumber, RejectLoanRequest rejectLoanRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.reject-loan")))
            .queryParam(COMMAND, "reject")
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "REJECT_LOAN", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, rejectLoanRequest, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Creates HttpHeaders with necessary authentication and content type information for M2P API
   * requests.
   *
   * @return HttpHeaders for M2P API requests.
   */
  private HttpHeaders getHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("Authorization", "Bearer " + authToken);
    headers.add("Fineract-Platform-TenantId", fineractTenantId);
    return headers;
  }

  public Mono<byte[]> fetchNOCPdf(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.fetch-noc")))
            .queryParam("output-type", "PDF")
            .queryParam("R_loanId", loanAccountNumber)
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_NOC", 0, false, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), byte[].class, webClientParameters);
  }

  public Mono<Object> getLoanDetails(String externalIdOne) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.fetch-loan-details")))
            .queryParam("R_loanExternalId", externalIdOne)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_LOAN_DETAILS", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<TransactionDetailResponse> getLoanTransactionDetails(String id) {

    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.ml.fetch-transaction-details")))
            .queryParam(ASSOCIATIONS, "transactions")
            .buildAndExpand(id)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_TRANSACTION_DETAILS", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(
        uri, getHeaders(), TransactionDetailResponse.class, webClientParameters);
  }

  public Mono<?> uploadDocumentAgainstLoan(
      DocumentUploadRequest documentUploadRequest, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.upload-document")))
            .buildAndExpand(loanId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "UPLOAD_LOAN_DOC", 0, true, webClientTimeoutProperties.getLarge());

    MultiValueMap<String, Object> documentUploadRequestData =
        getDocumentUploadRequestData(documentUploadRequest);
    HttpHeaders headers = getHeaders();
    headers.remove(CONTENT_TYPE);
    headers.add(CONTENT_TYPE, "multipart/form-data");
    return webClientFactory.uploadDocument(
        uri, documentUploadRequestData, headers, Object.class, webClientParameters);
  }

  private MultiValueMap<String, Object> getDocumentUploadRequestData(
      DocumentUploadRequest documentUploadRequest) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("name", documentUploadRequest.getName());
    body.add("tagIdentifier", documentUploadRequest.getTagIdentifier());
    body.add("file", documentUploadRequest.getFile());
    return body;
  }

  public Mono<?> markPartialCollection(
      String loanAccountNumber, M2pPartRepaymentRequestDTO m2pRequestFormatFromDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.part-repayment")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "MARK_PARTIAL_COLLECTION", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, m2pRequestFormatFromDTO, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Retrieves loan summary for a specific loan from the M2P API.
   *
   * @param loanAccountNumber The ID of the loan for which the loan summary is requested.
   * @return A Mono containing the retrieved repayment schedule data.
   */
  public Mono<?> getLoanSummaryByLoanAccountNumber(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.loan-summary")))
            .queryParam(
                ASSOCIATIONS,
                "multiDisburseDetails,repaymentSchedule,originalSchedule,transactions,loanApplicationReferenceId")
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_LOAN_SUMMARY", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Retrieves repayment schedule data with DPD for a specific loan from the M2P API.
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the retrieved repayment schedule data.
   */
  public Mono<Object> getRpsWithDpdByLoanAccountNumber(String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.repayment-schedule")))
            .queryParam(ASSOCIATIONS, "repaymentSchedule,originalSchedule")
            .queryParam(FETCH_SPECIFIED_DATA, true)
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "REPAYMENT_SCHEDULE_WITH_DPD", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Retrieves the due amount for a given loan ID using the M2P API.
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the retrieved repayment schedule data.
   */
  public Mono<Object> fetchDueAsOnDate(String loanAccountNumber, String date) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.due-amount-as-date")))
            .queryParam("asOnDate", date)
            .queryParam(LOCALE, "en")
            .queryParam("dateFormat", "dd-MM-yyyy")
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_DUE_AS_ON_DATE", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Retrieves all the loans due amount using the M2P API.
   *
   * @param date The date for which the due amount is requested.
   * @return A Mono containing the repayment schedule data.
   */
  public Mono<Object> fetchAllLoansDueAsOnDate(String date) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.all-loans-due-as-date")))
            .queryParam("R_startDate", date)
            .queryParam("R_loanProductId", 22)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_ALL_LOANS_DUE_AS_ON_DATE", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Retrieves loan details for a given loan ID using the M2P API.
   *
   * @param loanId The ID of the loan to be retrieved.
   * @param staffInSelectedOfficeOnly Whether to filter based on staff in the selected office.
   * @param associations A set of loan associations to include.
   * @param exclude A set of loan associations to exclude.
   * @param fields A set of specific fields to be included in the response.
   * @return A Mono containing the loan details.
   */
  public Mono<Object> retrieveLoan(
      Long loanId,
      boolean staffInSelectedOfficeOnly,
      Set<String> associations,
      Set<String> exclude,
      Set<String> fields) {

    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.loan-summary")))
            .queryParam("staffInSelectedOfficeOnly", staffInSelectedOfficeOnly)
            .queryParamIfPresent(
                ASSOCIATIONS,
                associations != null && !associations.isEmpty()
                    ? Optional.of(String.join(",", associations))
                    : Optional.empty())
            .queryParamIfPresent(
                EXCLUDE,
                exclude != null && !exclude.isEmpty()
                    ? Optional.of(String.join(",", exclude))
                    : Optional.empty())
            .queryParamIfPresent(
                "fields",
                fields != null && !fields.isEmpty()
                    ? Optional.of(String.join(",", fields))
                    : Optional.empty())
            .buildAndExpand(loanId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "RETRIEVE_LOAN", 0, false, webClientTimeoutProperties.getLarge());

    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Retrieves loan data for restructure eligibility using retrieveLoan with associations.
   *
   * @param loanId The loan ID (LAN) to retrieve.
   * @return A Mono containing the loan data for restructure eligibility.
   */
  public Mono<?> retrieveLoanForRestructure(Long loanId) {
    return retrieveLoan(
            loanId,
            false,
            Set.of(
                "multiDisburseDetails",
                "repaymentSchedule",
                "originalSchedule",
                "transactions",
                "loanApplicationReferenceId"),
            Set.of(),
            Set.of())
        .map(
            response -> {
              if (response instanceof RetrieveLoanResponseDTO) {
                return response;
              }
              return gson.fromJson(gson.toJson(response), RetrieveLoanResponseDTO.class);
            });
  }

  public Mono<Object> getAllLoanDetailsAgainstLead(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.fetch-all-loan-details")))
            .queryParam("R_clientId", leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_ALL_LOAN_DETAILS", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Calls the M2P API to fetch charge details based on the provided external charge ID.
   *
   * @param chargeExternalId The external charge ID for which details are fetched.
   * @return A Mono wrapping the API response.
   */
  public Mono<?> fetchChargeByExternalId(String chargeExternalId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.charge-by-external-id")))
            .buildAndExpand(chargeExternalId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "FETCH_CHARGE_BY_CHARGE-EXTERNALID",
            0,
            true,
            webClientTimeoutProperties.getLarge());

    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Flux<M2pOpenLoanCountDTO> getAllOpenLoansHavingDPDCount() {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-active-loan-with-dpd")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_ALL_OPENED_LOAN_COUNT", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getFluxData(
        uri, getHeaders(), M2pOpenLoanCountDTO.class, webClientParameters);
  }

  public Flux<M2pOpenLoanDetailsDTO> getLoansWithDPD(int limit, int offset) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-active-loan-with-dpd-details")))
            .queryParam("R_excessAmount", limit)
            .queryParam("R_loanId", offset)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_ALL_OPENED_LOAN", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getFluxData(
        uri, getHeaders(), M2pOpenLoanDetailsDTO.class, webClientParameters);
  }

  public Flux<M2pOpenLoanCountDTO> getAllNewLoansCount() {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-new-created-loans-count")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_ALL_NEW_LOAN_COUNT", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getFluxData(
        uri, getHeaders(), M2pOpenLoanCountDTO.class, webClientParameters);
  }

  public Flux<M2pNewLoansDetailsDTO> getNewLoansDetails(int limit, int offset) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-new-created-loans-details")))
            .queryParam("R_excessAmount", limit)
            .queryParam("R_loanId", offset)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_ALL_NEW_LOAN_DETAILS", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getFluxData(
        uri, getHeaders(), M2pNewLoansDetailsDTO.class, webClientParameters);
  }

  public Flux<M2pLoanAgreementDetailsDTO> getDocumentListBasedOnDate() {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-loan-agreement-details")))
            .toUriString();
    WebClientParameters parameters =
        util.getWebClientParameters(
            null, "GET_DOCUMENT_LIST", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getFluxData(uri, getHeaders(), M2pLoanAgreementDetailsDTO.class, parameters)
        .collectList()
        .doOnNext(
            list ->
                log.info(
                    "[{}] fetched documents: {}", LOAN_AGREEMENT_NOTIFICATIONS, gson.toJson(list)))
        .flatMapMany(Flux::fromIterable);
  }

  public Mono<byte[]> fetchDocumentPdf(String loanApplicationId, String documentId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.fetch-document-url")))
            .buildAndExpand(loanApplicationId, documentId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_DOCUMENT_PDF", 0, false, webClientTimeoutProperties.getLarge());
    HttpHeaders headers = getHeaders();
    String token = environment.getProperty("m2p.auth.token");
    headers.set("Authorization", "Bearer " + token);
    return webClientFactory.getData(uri, headers, byte[].class, webClientParameters);
  }

  public Mono<Object> callAdvancedReportApi(AdvancedReportRequestDTO requestDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.advancereports.generate-report")))
            .toUriString();
    String jsonRequestBody = gson.toJson(requestDTO);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "CALL_ADVANCE_REPORT_API", 0, true, null);

    return webClientFactory.postData(
        uri, jsonRequestBody, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> pollForFileLocationId(String resourceId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "m2p.api.advancereports.get-filelocationid-against-resourceid")))
            .buildAndExpand(resourceId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "POLL_FILE_LOCATION_ID", 0, false, null);

    return webClientFactory.getData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Flux<DataBuffer> downloadReportData(String fileLocationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "m2p.api.advancereports.download-report-against-filelocationid")))
            .buildAndExpand(fileLocationId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "DOWNLOAD_REPORT_DATA", 0, false, null);

    return webClientFactory.getFluxData(uri, getHeaders(), DataBuffer.class, webClientParameters);
  }

  /**
   * Fetch one page of overdue EMIs from M2P using limit and offset.
   *
   * @param productNames List of product keys to filter
   * @param asOnDate Report run date
   * @param offset Starting offset for paging
   * @param limit Number of records to fetch
   * @return Mono of list of EmiReportRow for one page
   */
  public Mono<List<EmiReportRow>> fetchOverdueEmis(
      List<String> productNames, LocalDate asOnDate, int offset, int limit) {
    String productsParam =
        productNames.stream().map(p -> "'" + p + "'").collect(Collectors.joining(","));
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-overdue-emi-reports")))
            .queryParam("R_productname", "(" + productsParam + ")")
            .queryParam("R_asOnDate", asOnDate.toString())
            .queryParam("R_transactionId", limit)
            .queryParam("R_external_id", offset)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_OVERDUE_EMIS", 3, true, webClientTimeoutProperties.getLarge());

    return webClientFactory
        .getFluxData(uri, getHeaders(), EmiReportRow.class, webClientParameters)
        .collectList();
  }

  public Flux<M2pCkycRecipientsDetailsDTO> getCkycRecipientsBasedOnDate() {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-ckyc-recipients")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_CKYC_RECIPIENTS", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getFluxData(
        uri, getHeaders(), M2pCkycRecipientsDetailsDTO.class, webClientParameters);
  }

  public Flux<M2pCkycRecipientsDetailsDTO> getHistoricalCkycRecipients() {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-historical-ckyc-recipients")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "FETCH_HISTORICAL_CKYC_RECIPIENTS",
            3,
            true,
            webClientTimeoutProperties.getLarge());
    return webClientFactory.getFluxData(
        uri, getHeaders(), M2pCkycRecipientsDetailsDTO.class, webClientParameters);
  }

  /**
   * get LAN details from M2P using loan agreement number.
   *
   * @param lan List of product keys to filter
   * @return Mono of list of M2pLanDetails for one page
   */
  public Mono<List<M2pLanDetails>> getLanDetails(String lan) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.reports.get-lan-details")))
            .queryParam("R_loanId", lan)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_LAN_DETAILS", 3, true, webClientTimeoutProperties.getLarge());

    return webClientFactory
        .getFluxData(uri, getHeaders(), M2pLanDetails.class, webClientParameters)
        .collectList();
  }

  public Mono<List<M2pFirstUnpaidTransaction>> getFirstUnpaidInstallmentDate(String lan) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-first-unpaid-installment")))
            .queryParam("R_loanId", lan)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "GET_FIRST_UNPAID_INSTALLMENT_DETAILS",
            3,
            true,
            webClientTimeoutProperties.getLarge());

    return webClientFactory
        .getFluxData(uri, getHeaders(), M2pFirstUnpaidTransaction.class, webClientParameters)
        .collectList();
  }

  /**
   * get excess amount using m2p report api get-excess-amount
   *
   * @param lan loan account number
   * @return Mono of list of M2pExcessAmountResponse for one page
   */
  public Mono<List<M2pExcessAmountResponse>> getExcessAmount(String lan) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-excess-amount")))
            .queryParam("R_loanId", lan)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_EXCESS_AMOUNT", 3, true, webClientTimeoutProperties.getLarge());

    return webClientFactory
        .getFluxData(uri, getHeaders(), M2pExcessAmountResponse.class, webClientParameters)
        .collectList();
  }

  public Flux<ReKycEligibleLoanDTO> fetchReKycNotificationEligibleLoans() {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-re-kyc-eligible-loans")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_RE_KYC_ELIGIBLE_LOANS", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.getFluxData(
        uri, getHeaders(), ReKycEligibleLoanDTO.class, webClientParameters);
  }

  public Mono<CreditLineMarkRepaymentResponse> markRepayment(
      String lineId, M2PCreditLineMarkRepaymentRequest markRepaymentRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.ml.credit-line-mark-repayment")))
            .buildAndExpand(lineId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, CREDIT_LINE_REPAYMENT_LOG_HEADER, 3, true, webClientTimeoutProperties.getLarge());

    return webClientFactory.postData(
        uri,
        markRepaymentRequest,
        getHeaders(),
        CreditLineMarkRepaymentResponse.class,
        webClientParameters);
  }

  public Flux<Object> getCreditLineAllTransactionDetails(String lineId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.ml.get-credit-line-all-transaction-details")))
            .buildAndExpand(lineId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, CREDIT_LINE_TRANSACTION_DETAILS, 3, true, webClientTimeoutProperties.getLarge());

    return webClientFactory.getFluxData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Flux<Object> getCreditLineActiveTransactionDetails(String lineId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty(
                        "m2p.api.ml.get-credit-line-active-transaction-details")))
            .buildAndExpand(lineId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, CREDIT_LINE_TRANSACTION_DETAILS, 3, true, webClientTimeoutProperties.getLarge());

    return webClientFactory.getFluxData(uri, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> postForeClosureCreditLineRequest(
      String lineId, String transactionId, M2pCreditLineForeClosurePayload foreClosurePayload) {

    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.ml.execute-credit-line-foreclosure")))
            .buildAndExpand(lineId, transactionId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "CREDIT_LINE_FORECLOSURE_REQUEST",
            0,
            true,
            webClientTimeoutProperties.getLarge());

    return webClientFactory.postData(
        uri, foreClosurePayload, getHeaders(), Object.class, webClientParameters);
  }

  /**
   * Fetches tentative restructured RPS from M2P API.
   *
   * @param restructureRequestId The ID from loan_restructure_eligibility_master table.
   * @return A Mono containing the tentative RPS response.
   */
  public Mono<TentativeRpsResponseDTO> getTentativeRestructuredRps(Integer restructureRequestId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.ml.get-tentative-restructured-rps")))
            .queryParam("command", "previewLoanReschedule")
            .buildAndExpand(restructureRequestId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "GET_TENTATIVE_RESTRUCTURED_RPS",
            3,
            false,
            webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(
        uri, getHeaders(), TentativeRpsResponseDTO.class, webClientParameters);
  }

  /**
   * Initiates a loan reschedule request with M2P.
   *
   * @param request The reschedule initiation request containing loan details and parameters.
   * @return A Mono containing the reschedule response with resourceId.
   */
  public Mono<RescheduleInitiateResponse> initiateRescheduleRequest(
      RescheduleInitiateRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.initiate-reschedule")))
            .queryParam(COMMAND, "reschedule")
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "INITIATE_RESCHEDULE", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, request, getHeaders(), RescheduleInitiateResponse.class, webClientParameters);
  }

  /**
   * Approves a reschedule request with M2P.
   *
   * @param requestId The reschedule request ID from M2P.
   * @param request The approval request with approvedOnDate.
   * @return A Mono containing the approval response.
   */
  public Mono<ApproveRescheduleResponse> approveRescheduleRequest(
      Integer requestId, ApproveRescheduleRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.approve-reschedule")))
            .queryParam(COMMAND, "approve")
            .buildAndExpand(requestId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "APPROVE_RESCHEDULE", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(
        uri, request, getHeaders(), ApproveRescheduleResponse.class, webClientParameters);
  }

  /**
   * Fetches the partial waive template for a loan (list of waivable charges).
   *
   * @param loanId The loan account number.
   * @return A Mono containing the list of waivable charges.
   */
  public Mono<List<PartialWaiveChargeDTO>> getPartialWaiveTemplate(Long loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.ml.partial-waive-template")))
            .buildAndExpand(loanId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_PARTIAL_WAIVE_TEMPLATE", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getFluxData(uri, getHeaders(), PartialWaiveChargeDTO.class, webClientParameters)
        .collectList();
  }

  /**
   * Waives charges for a loan using partial waiver.
   *
   * @param loanId The loan account number.
   * @param request The waiver request with list of charges to waive.
   * @return A Mono containing the waiver response.
   */
  public Mono<Object> waiveCharges(Long loanId, PartialWaiverRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.ml.partial-waiver")))
            .buildAndExpand(loanId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "WAIVE_CHARGES", 0, true, webClientTimeoutProperties.getLarge());
    return webClientFactory.postData(uri, request, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<RestructureApprovalDetailsDTO> getRestrcutureApprovalDetails(
      Integer restructureRequestId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.ml.get-restructure-approval-details")))
            .buildAndExpand(restructureRequestId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "GET_RESTRUCTURE_APPROVAL_DETAILS",
            3,
            true,
            webClientTimeoutProperties.getLarge());
    return webClientFactory.getData(
        uri, getHeaders(), RestructureApprovalDetailsDTO.class, webClientParameters);
  }

  public Flux<M2pLoanAgreementDetailsDTO> getLoanAgreementsForDisbursedLoansOneDayBack() {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty(
                        "m2p.api.reports.get-loan-agreement-details-one-day-back")))
            .toUriString();
    WebClientParameters parameters =
        util.getWebClientParameters(
            null, "GET_DISBURSED_LIST", 3, true, webClientTimeoutProperties.getLarge());
    return webClientFactory
        .getFluxData(uri, getHeaders(), M2pLoanAgreementDetailsDTO.class, parameters)
        .collectList()
        .doOnNext(
            list ->
                log.info(
                    "[{}] fetched loan-agreement documents: {}",
                    LOAN_AGREEMENT_NOTIFICATIONS,
                    gson.toJson(list)))
        .flatMapMany(Flux::fromIterable);
  }

  public Mono<List<EmiReportRow>> fetchOverdueEmisForProduct(
      String productNames, LocalDate activeFromDate, LocalDate asOnDate, int offset, int limit) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.reports.get-overdue-emi-reports")))
            .queryParam("R_productname", productNames)
            .queryParam("R_startDateSelect", activeFromDate)
            .queryParam("R_asOnDate", asOnDate.toString())
            .queryParam("R_transactionId", limit)
            .queryParam("R_external_id", offset)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_OVERDUE_EMIS", 3, true, webClientTimeoutProperties.getLarge());

    return webClientFactory
        .getFluxData(uri, getHeaders(), EmiReportRow.class, webClientParameters)
        .collectList();
  }
}
