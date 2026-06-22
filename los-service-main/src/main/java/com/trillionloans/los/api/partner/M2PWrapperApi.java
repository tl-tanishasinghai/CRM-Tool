package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.GET_LEAD;
import static com.trillionloans.los.constant.StringConstants.LOGGING_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.M2P_CTA_LITERAL;
import static com.trillionloans.los.constant.StringConstants.NOT_FOUND;
import static com.trillionloans.los.constant.StringConstants.PARSING_ERROR;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.UPDATE_GST;
import static com.trillionloans.los.constant.StringConstants.UPLOAD_DOC_LOAN;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.config.WebClientTimeoutProperties;
import com.trillionloans.los.constant.AadhaarXMLType;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.model.M2PCreditLineLoanApplicationRequestDTO;
import com.trillionloans.los.model.PanAadhaarLinkStatusDataTableDTO;
import com.trillionloans.los.model.dto.ActiveLoanDTO;
import com.trillionloans.los.model.dto.ApproveLoanDTO;
import com.trillionloans.los.model.dto.BreApprovedLoanDTO;
import com.trillionloans.los.model.dto.ClientDependentDTO;
import com.trillionloans.los.model.dto.ClientDetailsCpRpsResponseDto;
import com.trillionloans.los.model.dto.GetDocketDetailsResponseDto;
import com.trillionloans.los.model.dto.LeadIdResponse;
import com.trillionloans.los.model.dto.LivelinessScoreDataTableDTO;
import com.trillionloans.los.model.dto.LoanChargesDTO;
import com.trillionloans.los.model.dto.LoanInsuranceRequestDto;
import com.trillionloans.los.model.dto.PanVerificationLog;
import com.trillionloans.los.model.dto.QcChecksDataTableDTO;
import com.trillionloans.los.model.dto.RiskDetailsDataTableDTO;
import com.trillionloans.los.model.dto.TopUpDataTableDTO;
import com.trillionloans.los.model.dto.internal.DocIdDTO;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.entity.ValidationFunnelVerificationResultCallbackLog;
import com.trillionloans.los.model.partner.m2p.M2PGenerateCreditLineRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2PLoanApplicationRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pAdditionalDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pBeneficiaryBankDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pBulkDocumentsUploadDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientDetailsUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pMerchantDetailsRequest;
import com.trillionloans.los.model.partner.m2p.M2pNachMandateRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pUcicUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pVehicleDetailsDTO;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.request.AgreementDocumentUploadRequest;
import com.trillionloans.los.model.request.KycUploadDocumentRequest;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.request.LeadBulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.LoanAccountRejectRequest;
import com.trillionloans.los.model.request.LoanBankAccountDataTableDTO;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.MerchantBankDetails;
import com.trillionloans.los.model.request.MerchantChangeRequest;
import com.trillionloans.los.model.request.PanDetails;
import com.trillionloans.los.model.request.PreviewRpsRequest;
import com.trillionloans.los.model.request.RepaymentScheduleRequest;
import com.trillionloans.los.model.request.RiskOperationTableUpdateRequest;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.request.UpdateLoanApplication;
import com.trillionloans.los.model.request.m2p.AdvancedReportRequestDTO;
import com.trillionloans.los.model.request.m2p.LoanClassificationDetailsM2pRequest;
import com.trillionloans.los.model.request.m2p.M2PDrawdownRequest;
import com.trillionloans.los.model.request.m2p.M2PUpdateLoanRequest;
import com.trillionloans.los.model.request.m2p.M2pBankDetailsRequestDTO;
import com.trillionloans.los.model.request.m2p.M2pConsentRequest;
import com.trillionloans.los.model.request.m2p.M2pDedupeRequest;
import com.trillionloans.los.model.request.m2p.M2pDisburseLoanByLanRequestDTO;
import com.trillionloans.los.model.request.m2p.M2pInitiateDisbursalDTO;
import com.trillionloans.los.model.request.m2p.M2pLoanDisburseRequestDTO;
import com.trillionloans.los.model.response.ClientDetailsCpResponseDto;
import com.trillionloans.los.model.response.ClientDetailsResponseDto;
import com.trillionloans.los.model.response.ClientImageResponse;
import com.trillionloans.los.model.response.GetLoanLanDetailsResponse;
import com.trillionloans.los.model.response.GetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.LoanApprovalDetailsResponseDto;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.AddressFetchResponse;
import com.trillionloans.los.model.response.m2p.DisbursalAmountResponse;
import com.trillionloans.los.model.response.m2p.DisbursalBatchDataResponse;
import com.trillionloans.los.model.response.m2p.DisbursementStatusV2ResponseDTO;
import com.trillionloans.los.model.response.m2p.DrawdownCallbackDetailsDTO;
import com.trillionloans.los.model.response.m2p.ExperianBureauCtaUpdateResponse;
import com.trillionloans.los.model.response.m2p.LoanClassificationDetailsM2pResponse;
import com.trillionloans.los.model.response.m2p.LoanExternalIdResponseDTO;
import com.trillionloans.los.model.response.m2p.M2PActivationCheckDetailDTO;
import com.trillionloans.los.model.response.m2p.M2PApplicationTypeFundlyDTO;
import com.trillionloans.los.model.response.m2p.M2PCkycInfoResponse;
import com.trillionloans.los.model.response.m2p.M2PDisbursementCheckDetailDTO;
import com.trillionloans.los.model.response.m2p.M2PDrawdownResponse;
import com.trillionloans.los.model.response.m2p.M2PTopUpLoanApplicationRequestDTO;
import com.trillionloans.los.model.response.m2p.M2pAddBankDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pAddBeneficiaryBankAccountResponse;
import com.trillionloans.los.model.response.m2p.M2pAmlPepResultDTO;
import com.trillionloans.los.model.response.m2p.M2pBankDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pBreDataResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pClientCreationResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pClientIdTypeResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pClientIdsOfNullUcic;
import com.trillionloans.los.model.response.m2p.M2pConsentResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pDataTableResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pDedupeResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pDisbursementTriggerResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pDocumentTagDTO;
import com.trillionloans.los.model.response.m2p.M2pDocumentsUploadResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pErrorResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pGetBeneficiaryBankDetailsResponse;
import com.trillionloans.los.model.response.m2p.M2pGetChargeDetailResponseDto;
import com.trillionloans.los.model.response.m2p.M2pGetKycStatusResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pGetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pKycIdentifiersResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanAppIDContactDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanRejectResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pMaxDpdResponseDto;
import com.trillionloans.los.model.response.m2p.M2pNachMandateResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pNpaLoanApplicationsDto;
import com.trillionloans.los.model.response.m2p.M2pPanAadhaarDetailsDTO;
import com.trillionloans.los.model.response.m2p.M2pPlatformHealthResponse;
import com.trillionloans.los.model.response.m2p.M2pProductDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pResourceIdTypeResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pSelfieUploadResponseDTO;
import com.trillionloans.los.model.response.m2p.RequestedLoanDetailsResponse;
import com.trillionloans.los.model.response.m2p.RiskDedupeValidationResponseDto;
import com.trillionloans.los.model.response.m2p.RiskDetailResponseDTO;
import com.trillionloans.los.model.response.m2p.TransactionDetailsDTO;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import com.trillionloans.los.service.CustomerChangeDetectorService;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.LeadDataUtil;
import com.trillionloans.los.util.WebClientUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/** Outbound API call wrapper class for M2P APIs */
@Service
@Slf4j
public class M2PWrapperApi {
  private final WebClientFactory webClientFactory;
  private final String authToken;
  private final String fineractTenantId;
  private final Environment environment;
  private final Gson gson;
  private final WebClientUtil util;
  private final WebClientTimeoutProperties webClientTimeoutProperties;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String R_LOAN_APPLICATION_ID = "R_loanApplicationId";
  private static final String R_CLIENT_ID = "R_clientId";
  private static final String GENERIC_RESULT_SET = "genericResultSet";
  private static final String R_EXTERNAL_ID = "R_externalId";
  private static final String REJECT_LOAN_DATE_FORMAT = "dd MMMM yyyy";
  private static final String R_CHARGE_ID = "R_chargeId";
  private final CustomerChangeDetectorService changeDetectorService;

  public M2PWrapperApi(
      @Value("${m2p.api.base-url}") String baseUrl,
      @Value("${m2p.auth.token}") String authToken,
      @Value("${m2p.fineract-tenant-id}") String fineractTenantId,
      Environment env,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      @Lazy CustomerChangeDetectorService changeDetectorService,
      WebClientTimeoutProperties webClientTimeoutProperties) {
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl, "m2p", env, kafkaLoggingService, kafkaEventProducerService);
    this.authToken = authToken;
    this.environment = env;
    this.fineractTenantId = fineractTenantId;
    this.gson = new GsonBuilder().disableHtmlEscaping().create();
    this.util = new WebClientUtil();
    this.changeDetectorService = changeDetectorService;
    this.webClientTimeoutProperties = webClientTimeoutProperties;
  }

  public Mono<ClientDetailsResponseDto> getLeadData(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-lead")))
            .queryParam("R_clientId", leadId)
            .queryParam("associations", "hierarchyLookup")
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_LEAD, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, GET_LEAD, 0, false, false, eventContext);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), ClientDetailsResponseDto.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, GET_LEAD));
  }

  public Flux<M2pClientIdsOfNullUcic> getClientIdsOfNullUcic() {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-clientId-of-null-ucic")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "FETCH_CLIENT_IDS_OF_NULL_UCIC", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pClientIdsOfNullUcic>() {},
            webClientParameters)
        .doOnNext(res -> logSuccess(res, "FETCH_CLIENT_IDS_OF_NULL_UCIC"));
  }

  public Mono<?> getLoanApplications(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-loans")))
            .queryParam("clientId", leadId)
            .buildAndExpand()
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_LOANS, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LOANS", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> getLoanApplicationByLoanId(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-loan")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_LOAN_BY_ID, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LOAN_BY_ID", 3, true, true, eventContext);
    return webClientFactory.getData(uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<GetLoanV2ResponseDTO> getLoanApplicationByLoanIdV2(String loanId, String reason) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-loan-v2")))
            .queryParam("R_loanApplicationId", loanId)
            .buildAndExpand()
            .toUriString();

    EventContext eventContext = new EventContext(Event.GET_LOAN_BY_ID_V2, loanId);

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("override_event_type", reason);
    eventContext.setMetadata(metadata);
    eventContext.setPublishEvent(StringUtils.isNotEmpty(reason));

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LOAN_BY_ID_V2", 0, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), M2pGetLoanV2ResponseDTO.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .flatMap(
            data ->
                Mono.just(
                    GetLoanV2ResponseDTO.builder()
                        .loanApplicationId(data.getLoanApplicationId())
                        .loanId(data.getLoanId())
                        .clientId(data.getClientId())
                        .utrNumber(data.getUtrNumber())
                        .approvedAmount(data.getApprovedAmount())
                        .netDisburseAmount(data.getNetDisburseAmount())
                        .disbursedDate(data.getDisbursedDate())
                        .loanApplicationStatus(data.getLoanApplicationStatus())
                        .stepName(data.getStepName())
                        .build()))
        .doOnSuccess(res -> logSuccess(res, "GET_LOAN_BY_ID_V2"));
  }

  public Mono<M2pClientCreationResponseDTO> createLead(M2pLeadRequestDTO leadRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.create-lead")))
            .toUriString();

    EventContext eventContext = new EventContext(Event.CLIENT_CREATION);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "CLIENT_CREATION", 0, true, true, eventContext);
    return webClientFactory
        .postDataWithForBiddenHandling(
            uri,
            leadRequest,
            getM2pHeaders(),
            M2pClientCreationResponseDTO.class,
            webClientParameters)
        .onErrorResume(
            ForbiddenException.class,
            forbiddenException -> {
              Object responseBody = forbiddenException.getResponseBody();
              if (responseBody instanceof M2pErrorResponseDTO errorResponse
                  && !errorResponse.getErrors().isEmpty()) {
                M2pErrorResponseDTO.ErrorDetailDTO errorDetailDTO =
                    errorResponse.getErrors().get(0);
                if (errorDetailDTO.getArgs().size() == 2) {
                  M2pErrorResponseDTO.ArgumentDTO firstArgument = errorDetailDTO.getArgs().get(0);
                  M2pErrorResponseDTO.ArgumentDTO secondArgument = errorDetailDTO.getArgs().get(1);
                  if (Objects.equals(firstArgument.getValue(), "PAN")) {
                    String panValue = secondArgument.getValue().toString();

                    return processM2pDedupeRequest(panValue)
                        .flatMap(
                            panM2PDedupeResponseDTO ->
                                Mono.deferContextual(
                                    ctx -> {
                                      if (panM2PDedupeResponseDTO != null
                                          && panM2PDedupeResponseDTO.getClientId() != null) {
                                        Integer clientId = panM2PDedupeResponseDTO.getClientId();

                                        updateGstDetailsAsync(leadRequest, clientId)
                                            .contextWrite(c -> c.putAll(ctx))
                                            .subscribe();

                                        changeDetectorService
                                            .detectChangesAsync(clientId, leadRequest)
                                            .contextWrite(c -> c.putAll(ctx))
                                            .subscribe();
                                      }

                                      return Mono.justOrEmpty(panM2PDedupeResponseDTO);
                                    }));
                  }
                }
              }
              return Mono.error(forbiddenException);
            });
  }

  private Mono<Void> updateGstDetailsAsync(M2pLeadRequestDTO leadRequestDTO, Integer clientId) {

    return Mono.defer(
        () -> {
          if (leadRequestDTO.getAdditionalDetails() == null
              || leadRequestDTO.getAdditionalDetails().size() <= 1) {
            return Mono.empty();
          }

          M2pAdditionalDetailsDTO merchantDetails =
              LeadDataUtil.prepareMerchantAdditionDto(leadRequestDTO);
          if (merchantDetails == null) {
            log.info("{} gst details not present, skipping for client: {}", UPDATE_GST, clientId);
            return Mono.empty();
          }

          log.info("{} updating gst details for client: {}", UPDATE_GST, clientId);

          return addMerchantDetails(merchantDetails, String.valueOf(clientId))
              .doOnSuccess(
                  r ->
                      log.info(
                          "{} successfully updated gst details for client: {}",
                          UPDATE_GST,
                          clientId))
              .doOnError(
                  e ->
                      log.error(
                          "{} failed to update gst details for client {}: {}",
                          UPDATE_GST,
                          clientId,
                          e.getMessage(),
                          e))
              .then();
        });
  }

  public Mono<M2pClientCreationResponseDTO> processM2pDedupeRequest(String panValue) {
    String deDupeUri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.dedupe")))
            .queryParam("verbose", true)
            .toUriString();

    EventContext eventContext = new EventContext(Event.PAN_DEDUPE);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "DEDUPE_CHECK", 0, true, true, eventContext);
    M2pDedupeRequest m2pDedupeRequest = LeadDataUtil.getM2pDedupeRequest(panValue);
    return webClientFactory
        .postDataWithoutStringSerialization(
            deDupeUri,
            m2pDedupeRequest,
            getM2pHeaders(),
            M2pDedupeResponseDTO.class,
            webClientParameters)
        .filter(data -> !data.getClientDedupeResponseList().isEmpty())
        .map(
            data ->
                M2pClientCreationResponseDTO.builder()
                    .clientId(data.getClientDedupeResponseList().get(0).getId())
                    .build());
  }

  public Mono<M2pLoanCreationResponseDTO> createLoan(
      M2PLoanApplicationRequestDTO loanData, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.create-loan")))
            .buildAndExpand(leadId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.CREATE_LOAN_APPLICATION, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "LOAN_CREATION", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, loanData, getM2pHeaders(), M2pLoanCreationResponseDTO.class, webClientParameters);
  }

  public Mono<Object> createCreditLineLoan(
      M2PCreditLineLoanApplicationRequestDTO loanData, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.create-loan")))
            .buildAndExpand(leadId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.CREATE_LOAN_APPLICATION, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "LOAN_CREATION", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, loanData, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2pLoanCreationResponseDTO> createLoan(M2PTopUpLoanApplicationRequestDTO loanData) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.renewal-loan")))
            .toUriString();
    EventContext eventContext =
        new EventContext(
            Event.CREATE_TOP_UP_LOAN_APPLICATION, null, loanData.getClientId().toString());
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "LOAN_CREATION_TOP_UP", 0, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, loanData, getM2pHeaders(), M2pLoanCreationResponseDTO.class, webClientParameters);
  }

  private M2PUpdateLoanRequest sanitizeForM2P(UpdateLoanApplication req) {
    M2PUpdateLoanRequest sanitized = new M2PUpdateLoanRequest();
    sanitized.setLoanAmountRequested(req.getLoanAmountRequested());
    sanitized.setTenure(req.getTenure());
    sanitized.setRateOfInterest(req.getRateOfInterest());
    sanitized.setExpectedDisbursementDate(req.getExpectedDisbursementDate());
    sanitized.setDateFormat(req.getDateFormat());

    return sanitized;
  }

  public <T> Mono<T> updateLoanApplication(
      String loanId, UpdateLoanApplication loanData, Class<T> responseType) {

    M2PUpdateLoanRequest sanitizedLoanData = sanitizeForM2P(loanData);
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.update-loan")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPDATE_LOAN, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "LOAN_UPDATE", 0, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, sanitizedLoanData, getM2pHeaders(), responseType, webClientParameters);
  }

  public Mono<M2pAadhaarXmlResponseDTO> uploadAadhaarXml(
      AadhaarXmlRequest aadhaarXmlRequest,
      String leadId,
      AadhaarXMLType aadhaarXMLType,
      String loanId) {
    String xmlUploadEndpoint = "";
    if (requireNonNull(aadhaarXMLType) == AadhaarXMLType.DIGI_LOCKER) {
      xmlUploadEndpoint = environment.getProperty("m2p.api.upload-aadhaar-xml");
    } else if (aadhaarXMLType == AadhaarXMLType.OKYC) {
      xmlUploadEndpoint = environment.getProperty("m2p.api.upload-okyc-aadhaar-xml");
    }
    String uri =
        UriComponentsBuilder.fromUriString(requireNonNull(xmlUploadEndpoint))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.AADHAR_XML_UPLOAD, loanId, leadId);

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("aadhaarXmlType", aadhaarXMLType.getDisplayName());
    eventContext.setMetadata(metadata);

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "AADHAAR_XML_UPLOAD", 0, false, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        aadhaarXmlRequest,
        getM2pHeaders(),
        M2pAadhaarXmlResponseDTO.class,
        webClientParameters);
  }

  public Mono<?> uploadSelfieAgainstLead(
      SelfieUpload selfieUploadData, String leadId, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.upload-selfie")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.SELFIE_UPLOAD, loanId, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "SELFIE_UPLOAD", 0, false, true, eventContext);
    Map<String, Object> requestData = getSelfieUploadRequestData(selfieUploadData);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, requestData, getM2pHeaders(), M2pSelfieUploadResponseDTO.class, webClientParameters);
  }

  public Mono<?> uploadNachMandateRequest(
      String loanId, M2pNachMandateRequestDTO nachMandateRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.upload-nach")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPLOAD_NACH_MANDATE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "NACH_MANDATE_CREATION", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        nachMandateRequest,
        getM2pHeaders(),
        M2pResourceIdTypeResponseDTO.class,
        webClientParameters);
  }

  public Mono<?> updateLead(M2pLeadUpdateDTO leadRequest, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.update-lead")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.CLIENT_UPDATE, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "LEAD_UPDATE", 3, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, leadRequest, getM2pHeaders(), M2pClientIdTypeResponseDTO.class, webClientParameters);
  }

  public Mono<?> updateClientDetails(
      M2pClientDetailsUpdateDTO clientDetailsUpdateRequest, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.update-client-detail")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.CLIENT_UPDATE, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "CLIENT_DETAILS_UPDATE", 3, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, clientDetailsUpdateRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Flux<?> getKycIdentifiersAgainstLead(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-identifiers")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_KYC_ID, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_KYC_ID", 0, true, true, eventContext);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), M2pKycIdentifiersResponseDTO.class, webClientParameters);
  }

  private Map<String, Object> getSelfieUploadRequestData(SelfieUpload selfieUploadData) {
    Map<String, Object> geoTagData = new HashMap<>();
    Map<String, Object> imageData = new HashMap<>();
    Map<String, Object> requestData = new HashMap<>();
    imageData.put("fileType", selfieUploadData.getFileType());
    imageData.put("storageType", selfieUploadData.getStorageType());
    imageData.put("filePath", selfieUploadData.getFileContent());
    imageData.put("fileName", selfieUploadData.getFileName());
    geoTagData.put("longitude", selfieUploadData.getLongitude());
    geoTagData.put("latitude", selfieUploadData.getLatitude());
    imageData.put("geoTag", geoTagData);
    requestData.put("image", imageData);
    requestData.put("doLiveliness", selfieUploadData.getDoLiveliness());

    return requestData;
  }

  private MultiValueMap<String, Object> getAgreementUploadRequestData(
      AgreementDocumentUploadRequest agreementDocumentUploadRequest) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("name", agreementDocumentUploadRequest.getName());
    body.add("tagIdentifier", agreementDocumentUploadRequest.getTagIdentifier());
    body.add("file", agreementDocumentUploadRequest.getFile());
    return body;
  }

  private MultiValueMap<String, Object> getKycUploadRequestData(
      KycUploadDocumentRequest kycUploadDocumentRequestBody) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("name", kycUploadDocumentRequestBody.getName());
    body.add("side", kycUploadDocumentRequestBody.getSide());
    body.add("tagIdentifier", kycUploadDocumentRequestBody.getTagIdentifier());
    body.add("file", kycUploadDocumentRequestBody.getFile());
    return body;
  }

  public Flux<?> getNachMandateRequest(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-nach")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_NACH_MANDATE, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_NACH_MANDATE", 0, true, true, eventContext);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), M2pNachMandateResponseDTO.class, webClientParameters);
  }

  public Mono<?> uploadAgreementDocumentAgainstLoan(
      AgreementDocumentUploadRequest agreementDocumentUploadRequest, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.upload-agreement")))
            .buildAndExpand(loanId)
            .toUriString();

    MultiValueMap<String, Object> agreementUploadRequestData =
        getAgreementUploadRequestData(agreementDocumentUploadRequest);
    HttpHeaders headers = getM2pHeaders();
    headers.remove(HttpHeaders.CONTENT_TYPE);
    headers.add(CONTENT_TYPE, "multipart/form-data");

    EventContext eventContext = new EventContext(Event.UPLOAD_AGREEMENT, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "UPLOAD_AGREEMENT", 0, true, true, eventContext);
    return webClientFactory.uploadDocumentWithoutStringSerialization(
        uri, agreementUploadRequestData, headers, Object.class, webClientParameters);
  }

  public Mono<?> uploadKycDocumentAgainstLeadId(
      KycUploadDocumentRequest kycUploadDocumentRequestBody, String leadId, String kycId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.upload-kyc")))
            .buildAndExpand(leadId, kycId)
            .toUriString();

    MultiValueMap<String, Object> kycUploadRequestData =
        getKycUploadRequestData(kycUploadDocumentRequestBody);
    HttpHeaders headers = getM2pHeaders();
    headers.remove(HttpHeaders.CONTENT_TYPE);
    headers.add(CONTENT_TYPE, "multipart/form-data");
    EventContext eventContext = new EventContext(Event.UPLOAD_KYC_DOCUMENT, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "UPLOAD_KYC_DOC", 0, true, true, eventContext);
    return webClientFactory.uploadDocumentWithoutStringSerialization(
        uri, kycUploadRequestData, headers, Object.class, webClientParameters);
  }

  public Mono<?> createConsent(M2pConsentRequest consentRequest, String leadId, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.create-consent")))
            .buildAndExpand(loanId, leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.CREATE_CONSENT, loanId, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "CREATE_CONSENT", 3, true, true, eventContext);
    M2pConsentRequest m2PConsentRequest =
        new M2pConsentRequest(
            consentRequest.getConsentKey(),
            consentRequest.getIpAddress(),
            consentRequest.getIsAccepted(),
            consentRequest.getAdditionalDetails());
    return webClientFactory.postDataWithoutStringSerialization(
        uri, m2PConsentRequest, getM2pHeaders(), M2pConsentResponseDTO.class, webClientParameters);
  }

  public Mono<M2pLoanRejectResponseDTO> rejectLoanApplication(
      LoanReject rejectionData, String loanId) {
    return Mono.deferContextual(
        ctx -> {
          String traceId = ctx.getOrDefault(TRACE_ID, UUID.randomUUID().toString());
          String uri =
              UriComponentsBuilder.fromUriString(
                      requireNonNull(environment.getProperty("m2p.api.reject-loan-appl")))
                  .queryParam("command", "reject")
                  .buildAndExpand(loanId)
                  .toUriString();
          EventContext eventContext = new EventContext(Event.REJECT_LOAN_APPLICATION, loanId, null);
          WebClientParameters webClientParameters =
              util.getWebClientParameters(null, "LOAN_REJECT", 0, true, true, eventContext);
          return webClientFactory
              .putDataWithoutStringSerialization(
                  uri,
                  rejectionData,
                  getM2pHeaders(),
                  M2pLoanRejectResponseDTO.class,
                  webClientParameters)
              .contextWrite(Context.of(TRACE_ID, traceId));
        });
  }

  public Mono<?> getRepaymentScheduleByLoanId(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-rps")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_REPAYMENT, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_REPAYMENT", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2pProductDetailsResponseDTO> getProductDetailsByProductCode(String productCode) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-product-details")))
            .buildAndExpand(productCode)
            .toUriString();
    EventContext eventContext = new EventContext(Event.FETCH_PRODUCT);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "FETCH_PRODUCT", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), M2pProductDetailsResponseDTO.class, webClientParameters);
  }

  public Mono<?> getRepaymentScheduleWithoutLoan(
      RepaymentScheduleRequest repaymentScheduleRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-rps-without-loan")))
            .queryParam("command", "calculateLoanSchedule")
            .buildAndExpand()
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_RPS_WITHOUT_LOAN", 0, true, true, null);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, repaymentScheduleRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  /**
   * Preview RPS schedule without creating a loan. Uses same M2P endpoint as get-rps-without-loan
   * with request format provided by upstream (drawdown preview).
   *
   * @param request preview RPS request body (same as upstream and M2P)
   * @return generic response (upstream will use typed contract)
   */
  public Mono<Object> getPreviewRpsSchedule(PreviewRpsRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-rps-without-loan")))
            .queryParam("command", "calculateLoanSchedule")
            .buildAndExpand()
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_RPS_WITHOUT_LOAN", 0, true, true, null);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, request, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2pDisbursementTriggerResponseDTO> triggerDisbursement(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.trigger-disb")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.TRIGGER_DISBURSEMENT, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "TRIGGER_DISB", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, null, getM2pHeaders(), M2pDisbursementTriggerResponseDTO.class, webClientParameters);
  }

  public Mono<?> uploadDocumentsAgainstLoan(
      String loanId, M2pBulkDocumentsUploadDTO bulkDocumentsUploadRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.upload-documents")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPLOAD_DOCUMENT, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, UPLOAD_DOC_LOAN, 0, false, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        bulkDocumentsUploadRequest,
        getM2pHeaders(),
        M2pDocumentsUploadResponseDTO.class,
        webClientParameters);
  }

  public Mono<byte[]> getDocumentByLoanIdAndDocumentId(String loanId, String documentId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.download-document")))
            .buildAndExpand(loanId, documentId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_DOC, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DOC", 0, false, false, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), byte[].class, webClientParameters);
  }

  public Mono<M2pBreDataResponseDTO> postBreData(Object requestBody, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.bre-post")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.POST_BRE, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "POST_BRE", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, requestBody, getM2pHeaders(), M2pBreDataResponseDTO.class, webClientParameters);
  }

  public Mono<?> undoApproveLoan(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.undoapprove-loan")))
            .queryParam("command", "undoapprove")
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UNDO_APPROVE_LOAN, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "UNDO_APPROVE", 0, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, null, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> addCharges(String loanId, LoanChargesDTO chargeData) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.add-topup-charges")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_CHARGES, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ADD_CHARGES", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, chargeData, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> addTopUpDataTable(TopUpDataTableDTO topUpDataTable, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.topup-data-table")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_TOP_UP_DATATABLE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ADD_TOP_UP_TABLE", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, topUpDataTable, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> approveLoan(String loanId, ApproveLoanDTO approveLoanDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.approve-loan")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.APPROVE_LOAN, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "APPROVE_LOAN",
            Integer.parseInt(
                Objects.requireNonNull(environment.getProperty("m2p.config.approve-loan-retry"))),
            true,
            true,
            eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, approveLoanDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<LoanApprovalDetailsResponseDto> getLoanApprovalDetails(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.approved-loan-details")))
            .queryParam("R_loanId", loanId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.GET_APPROVE_LOAN, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_APPROVED_LOAN_DETAILS", 0, false, false, eventContext);

    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), LoanApprovalDetailsResponseDto.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, "GET_APPROVED_LOAN_DETAILS"));
  }

  public Mono<Object> registerCta(String identifier, String ctaName) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty(M2P_CTA_LITERAL + ctaName + ".endpoint")))
            .buildAndExpand(identifier)
            .toUriString();

    EventContext eventContext = new EventContext(Event.GENERIC_CTA, identifier, null);
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("cta-name", environment.getProperty(M2P_CTA_LITERAL + ctaName + ".logger-header"));
    eventContext.setMetadata(metadata);

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            requireNonNull(environment.getProperty(M2P_CTA_LITERAL + ctaName + ".logger-header")),
            Integer.parseInt(
                requireNonNull(
                    environment.getProperty(M2P_CTA_LITERAL + ctaName + ".retry-count"))),
            Boolean.parseBoolean(
                Objects.requireNonNull(
                    environment.getProperty(M2P_CTA_LITERAL + ctaName + ".log-required"))),
            true,
            eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, null, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> uploadDocumentsAgainstLead(
      String leadId, LeadBulkDocumentsUploadRequest leadBulkDocumentsUploadRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.upload-documents-lead")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPLOAD_DOCUMENT, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "UPLOAD_DOC_LEAD", 0, false, false, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, leadBulkDocumentsUploadRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  /**
   * Stamps vehicle details using a POST request to M2P API endpoint.
   *
   * @param vehicleDetailsRequest The DTO containing vehicle details to be stamped.
   * @param loanId The unique identifier of the loan associated with the vehicle details.
   * @return A Mono emitting the response object from the external API.
   */
  public Mono<?> stampVehicleDetails(M2pVehicleDetailsDTO vehicleDetailsRequest, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.vehicle-details")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.VEHICLE_DETAILS, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "VEHICLE_DETAILS", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        vehicleDetailsRequest,
        getM2pHeaders(),
        M2pResourceIdTypeResponseDTO.class,
        webClientParameters);
  }

  /**
   * Stamps merchant details using a POST request to M2P API endpoint.
   *
   * @param merchantDetailsRequest The DTO containing merchant details to be stamped.
   * @return A Mono emitting the response object from the external API.
   */
  public Mono<Object> stampMerchantDetails(M2pMerchantDetailsRequest merchantDetailsRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.merchant-details")))
            .buildAndExpand()
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "MERCHANT_DETAILS", 0, true, true, null);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, merchantDetailsRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  /**
   * Stamps merchant bank account details using a POST request to M2P API endpoint.
   *
   * @param merchantBankDetails The DTO containing merchant bank account details to be stamped.
   * @return A Mono emitting the response object from the external API.
   */
  public Mono<Object> stampMerchantBankAccountDetails(
      MerchantBankDetails merchantBankDetails, String identifier) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.merchant-bank-details")))
            .buildAndExpand(identifier)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "MERCHANT_BANK_DETAILS", 0, true, true, null);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, merchantBankDetails, getM2pHeaders(), Object.class, webClientParameters);
  }

  /**
   * Updates the merchant details associated with a specified loan application.
   *
   * <p>This method constructs a URI for the external API endpoint using the provided loan ID and
   * performs a PUT request to update the merchant details.
   *
   * @param merchantChangeRequest the request containing the updated merchant information
   * @param loanId the ID of the loan application associated with the merchant
   * @return a Mono wrapping the response object from the update operation
   */
  public Mono<Object> updateMerchantAgainstLoanApplication(
      MerchantChangeRequest merchantChangeRequest, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.merchant-update")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.MERCHANT_UPDATE, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "MERCHANT_UPDATE", 0, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, merchantChangeRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  /**
   * Retrieves the merchant details associated with a specified loan application.
   *
   * <p>This method constructs a URI for the external API endpoint using the provided loan ID and
   * performs a GET request to fetch the merchant details.
   *
   * @param loanId the ID of the loan application for which to retrieve merchant details
   * @return a Mono wrapping the response object containing the merchant details
   */
  public Mono<Object> getMerchantAgainstLoanApplication(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.merchant-get")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.MERCHANT_GET, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "MERCHANT_GET", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  private HttpHeaders getM2pHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, "application/json");
    headers.add("Fineract-Platform-TenantId", fineractTenantId);
    headers.add("Authorization", "Bearer " + authToken);
    return headers;
  }

  private void logSuccess(Object responseBody, String loggerHeader) {
    log.info(LOGGING_RESPONSE, loggerHeader, "M2P", gson.toJson(responseBody));
  }

  public Mono<String> fetchBureauData(String bureauId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-bureau-data")))
            .buildAndExpand(bureauId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "BRE_GET_BUREAU_DATA", 3, true, true, null);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), String.class, webClientParameters);
  }

  public Mono<Object> callExperian(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.call-experian")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.BRE_CRIF_HARD_PULL, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "BRE_EXPERIAN_HARD_PULL", 3, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> updateBreDataToM2P(Object breDatatableDTO, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.bre-datatable")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.BRE_M2P_UPDATE, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "BRE_M2P_UPDATE", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, breDatatableDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> saveAmlPepResult(Object amlPepDatatableDTO, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.aml-pep-datatable")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_AML_PEP_DATATABLE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "AML_PEP_M2P_UPDATE", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, amlPepDatatableDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getLeadLoanExternalId(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-lan-loan-external-id")))
            .queryParam("R_clientId", leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.FETCH_LAN_LOAN_EXTERNAL_ID, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_LAN_LOAN_EXTERNAL_ID", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2pAmlPepResultDTO> getAmlPepResult(String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-aml-pep-result")))
            .queryParam(R_LOAN_APPLICATION_ID, loanApplicationId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "FETCH_AML_PEP_RESULT", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), M2pAmlPepResultDTO.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, "FETCH_AML_PEP_RESULT"));
  }

  public Mono<M2pPanAadhaarDetailsDTO> getPanAadhaarDetailsByClientId(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-pan-aadhaar-client-id")))
            .queryParam("R_clientId", leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.FETCH_PAN_AADHAAR_CLIENT_ID, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_PAN_AADHAAR_CLIENT_ID", 0, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pPanAadhaarDetailsDTO>() {},
            webClientParameters)
        .collectMap(M2pPanAadhaarDetailsDTO::getPanNumber)
        .flatMap(
            panAadhaarDetailsDTOMap -> {
              if (!panAadhaarDetailsDTOMap.isEmpty()) {
                for (Map.Entry<String, M2pPanAadhaarDetailsDTO> entry :
                    panAadhaarDetailsDTOMap.entrySet()) {
                  return Mono.just(entry.getValue());
                }
              }
              return null;
            });
  }

  public Mono<Object> callAdvancedReportApi(AdvancedReportRequestDTO requestDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.advancereports.generate-report")))
            .toUriString();
    String jsonRequestBody = gson.toJson(requestDTO);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "CALL_ADVANCE_REPORT_API", 0, true, true, null);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, jsonRequestBody, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> pollForFileLocationId(String resourceId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "m2p.api.advancereports.get-filelocationid-against-resourceid")))
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "POLL_FILE_LOCATION_ID", 0, false, false, null);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
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
        util.getWebClientParameters(null, "DOWNLOAD_REPORT_DATA", 0, false, false, null);

    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), DataBuffer.class, webClientParameters);
  }

  public Flux<M2pBankDetailsResponseDTO> fetchBankAccountDetails(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.fetch-bank-account-details")))
            .buildAndExpand(clientId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.FETCH_BANK_ACCOUNT_DETAILS, null, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_BANK_ACCOUNT_DETAILS", 3, true, true, eventContext);
    return webClientFactory.getFluxDataWithTypeRef(
        uri,
        getM2pHeaders(),
        new ParameterizedTypeReference<M2pBankDetailsResponseDTO>() {},
        webClientParameters);
  }

  public Mono<M2pAddBankDetailsResponseDTO> addBankAccountDetails(
      String leadId, M2pBankDetailsRequestDTO bankDetailsDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.add-bank-account-details")))
            .queryParam("isPennydropAsync", false)
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_BANK_ACCOUNT_DETAILS, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ADD_BANK_ACCOUNT_DETAILS", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        bankDetailsDTO,
        getM2pHeaders(),
        M2pAddBankDetailsResponseDTO.class,
        webClientParameters);
  }

  public Mono<?> addBankDetailsDataTable(
      LoanBankAccountDataTableDTO loanBankAccountDataTableDTO, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.bank-account-data-table")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_BANK_DETAILS_DATATABLE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ADD_BANK_DETAILS_TABLE", 3, true, true, eventContext);
    return webClientFactory
        .postDataWithForBiddenHandling(
            uri, loanBankAccountDataTableDTO, getM2pHeaders(), Object.class, webClientParameters)
        .onErrorResume(
            ForbiddenException.class,
            forbiddenException -> {
              Object responseBody = forbiddenException.getResponseBody();
              if (responseBody instanceof M2pErrorResponseDTO errorResponse
                  && !errorResponse.getErrors().isEmpty()) {
                M2pErrorResponseDTO.ErrorDetailDTO errorDetailDTO =
                    errorResponse.getErrors().get(0);
                if (errorDetailDTO.getArgs().size() == 2) {
                  M2pErrorResponseDTO.ArgumentDTO secondArgument = errorDetailDTO.getArgs().get(1);
                  if (Objects.nonNull(secondArgument.getValue())) {
                    return getMappedBankIdFromDataTable(loanId);
                  }
                }
              }
              return Mono.error(forbiddenException);
            });
  }

  public Mono<?> addPanAadhaarLinkDetailsDataTable(
      PanAadhaarLinkStatusDataTableDTO panAadhaarLinkStatusDataTableDTO, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.pan-aadhaar-details-data-table")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_PAN_AADHAR_LINK_DATATABLE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "ADD_PAN_AADHAAR_LINK_DETAILS_TABLE", 3, true, true, eventContext);
    return webClientFactory.postDataWithForBiddenHandling(
        uri, panAadhaarLinkStatusDataTableDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> saveInsuranceResult(Object insuranceDatatableDTO, Integer loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.insurance-datatable")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();

    EventContext eventContext =
        new EventContext(Event.ADD_INSURANCE_DATATABLE, loanId.toString(), null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "INSURANCE_M2P_UPDATE", 3, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, insuranceDatatableDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  private Mono<M2pAddBankDetailsResponseDTO> getMappedBankIdFromDataTable(String loanId) {
    return getBankDetailsDataTable(loanId)
        .flatMap(
            bankDetails ->
                Mono.just(
                    new M2pAddBankDetailsResponseDTO(
                        bankDetails.getColumnData().get(0).getRow().get(0).getValue(),
                        null,
                        null)));
  }

  public Mono<?> updateUcic(M2pUcicUpdateDTO ucicRequest, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.update-ucic")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UCIC_UPDATE, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "UCIC_UPDATE", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, ucicRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2pDataTableResponseDTO> getBankDetailsDataTable(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.bank-account-data-table")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_BANK_DETAILS_TABLE, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_BANK_DETAILS_TABLE", 3, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), M2pDataTableResponseDTO.class, webClientParameters);
  }

  public Mono<?> initiateLoanDisbursement(String loanId, M2pInitiateDisbursalDTO disbursalDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.initiate-loan-disbursal")))
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.INITIATE_LOAN_DISBURSAL, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "INITIATE_LOAN_DISBURSAL", 3, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, disbursalDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> getClientLoanPerformanceReport(
      String clientId, String reportApi, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-client-loan-performance-report")))
            .queryParam(R_CLIENT_ID, clientId)
            .queryParam(R_LOAN_APPLICATION_ID, loanId)
            .buildAndExpand(reportApi)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.CLIENT_LOAN_PERFORMANCE_REPORT, loanId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "CLIENT_LOAN_PERFORMANCE_REPORT", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getCkycStatus(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-ckyc-status")))
            .queryParam("R_loanApplicationId", loanId)
            .buildAndExpand()
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_CKYC_STATUS, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_CKYC_STATUS", 3, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getCpvStatus(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-cpv-status")))
            .queryParam(R_LOAN_APPLICATION_ID, loanId)
            .buildAndExpand()
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_CPV_STATUS, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_CPV_STATUS", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> initiateCkyc(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.initiate-ckyc")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.INITIATE_CKYC, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "INITIATE_CKYC", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, null, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getLoanIdsByLeadId(String leadId, String loanProductId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-all-loans-by-lead-id")))
            .queryParam("R_clientId", leadId)
            .queryParam("R_loanProductId", loanProductId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "FETCH_ALL_LOANS_BY_LEAD_ID", 0, true, true, null);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2PCkycInfoResponse> getCkycInfo(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-ckyc-info")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_CKYC_INFO, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_CKYC_INFO", 3, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), M2PCkycInfoResponse.class, webClientParameters);
  }

  public Mono<?> addCoApplicant(String loanId, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.add-co-applicant")))
            .queryParam("clientId", leadId)
            .queryParam("relationshipType", "31")
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_CO_APPLICANT, loanId, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ADD_CO_APPLICANT", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, null, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getLoanDisbursementStatus(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-disbursement-status")))
            .queryParam(R_LOAN_APPLICATION_ID, loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_LOAN_DISBURSE_STATUS, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LOAN_DISBURSE_STATUS", 3, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2pAddBeneficiaryBankAccountResponse> addBeneficiaryBankAccountDetails(
      String leadId, M2pBeneficiaryBankDetailsDTO m2pBeneficiaryBankDetailsDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.add-beneficiary-bank-details")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.ADD_BENEFICIARY_BANK_ACCOUNT_DETAILS, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "ADD_BENEFICIARY_BANK_ACCOUNT_DETAILS", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        m2pBeneficiaryBankDetailsDTO,
        getM2pHeaders(),
        M2pAddBeneficiaryBankAccountResponse.class,
        webClientParameters);
  }

  public Flux<M2pGetBeneficiaryBankDetailsResponse> getMappedBeneficiaryBankId(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.get-beneficiary-bank-details")))
            .buildAndExpand(leadId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_BENEFICIARY_BANK_ACCOUNT_DETAILS", 3, true, true, null);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pGetBeneficiaryBankDetailsResponse>() {},
            webClientParameters)
        .doOnNext(res -> logSuccess(res, "FETCH_BENEFICIARY_BANK_ACCOUNT_DETAILS"));
  }

  public Mono<?> activateBeneficiaryBankAccount(
      String bankAssociationId, String loanId, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.activate-beneficiary-bank-account")))
            .buildAndExpand(bankAssociationId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.ACTIVATE_BENEFICIARY_BANK_ACCOUNT, loanId, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "ACTIVATE_BENEFICIARY_BANK_ACCOUNT", 3, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, null, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> uploadScoreAgainstLead(LivelinessScoreDataTableDTO scoreUploadDto, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.add-liveliness-score")))
            .queryParam(GENERIC_RESULT_SET, true)
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPLOAD_LIVLINESS_SCORE, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "LIVELINESS_SCORE_CREATE", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, scoreUploadDto, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Flux<M2pNpaLoanApplicationsDto> getNpaLoansByClientId(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-npa-loans")))
            .queryParam(R_CLIENT_ID, leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.FETCH_NPA_LOANS_BY_CLIENT_ID, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_NPA_LOANS_BY_CLIENT_ID", 0, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pNpaLoanApplicationsDto>() {},
            webClientParameters)
        .doOnNext(res -> logSuccess(res, "FETCH_NPA_LOANS_BY_CLIENT_ID"));
  }

  public Flux<M2pMaxDpdResponseDto> getMaxDpdByClientId(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-max-dpd-by-client-id")))
            .queryParam(R_CLIENT_ID, leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.FETCH_MAX_DPD_BY_CLIENT_ID, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_MAX_DPD_BY_CLIENT_ID", 0, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pMaxDpdResponseDto>() {},
            webClientParameters)
        .doOnNext(res -> logSuccess(res, "FETCH_MAX_DPD_BY_CLIENT_ID"));
  }

  public Mono<RiskDedupeValidationResponseDto> getRiskDedupeValidationData(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-risk-dedupe-validation-data")))
            .queryParam(R_CLIENT_ID, clientId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.FETCH_RISK_DEDUPE_VALIDATION_DATA, null, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "FETCH_RISK_DEDUPE_VALIDATION_DATA", 0, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<RiskDedupeValidationResponseDto>() {},
            webClientParameters)
        .next()
        .doOnNext(res -> logSuccess(res, "FETCH_RISK_DEDUPE_VALIDATION_DATA"));
  }

  public Mono<M2pLoanAppIDContactDTO> getContactDetailByloanAppId(String loanAppId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "m2p.api.reports.get-contact-details-by-loanApplicationId")))
            .queryParam("R_loanApplicationId", loanAppId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "FETCH_CONTACT_BY_LOANAPPID", 0, true, true, null);

    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pLoanAppIDContactDTO>() {},
            webClientParameters)
        .next()
        .switchIfEmpty(Mono.just(new M2pLoanAppIDContactDTO()));
  }

  public Mono<RiskDetailResponseDTO> getRiskDetailsAgainstLoanId(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.reports.get-risk-details")))
            .queryParam(R_LOAN_APPLICATION_ID, loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_RISK_DETAILS, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_RISK_DETAILS", 3, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), RiskDetailResponseDTO.class, webClientParameters)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  logSuccess("[]", "GET_RISK_DETAILS");
                  return Mono.empty();
                }))
        .next()
        .doOnSuccess(
            res -> {
              if (res != null) {
                logSuccess(res, "GET_RISK_DETAILS");
              }
            });
  }

  public Mono<?> uploadRiskAgainstLead(
      RiskDetailsDataTableDTO riskDetailsDataTableDTO, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.add-risk-details")))
            .queryParam(GENERIC_RESULT_SET, true)
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.RISK_DETAILS_UPLOAD, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "RISK_DETAILS_UPLOAD", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, riskDetailsDataTableDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getLeadInfo(String mobileNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-lead-against-mobilenumber")))
            .queryParam("mobileNo", mobileNumber)
            .queryParam("resource", "clients")
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LEAD_INFO", 0, true, true, null);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> getDocumentList(String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-document-list")))
            .buildAndExpand(loanApplicationId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_DOC_LIST, loanApplicationId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DOCUMENT_LIST", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> addAutoDisbursalStatusInDatatable(Object autoDisbursalDTO, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.add-auto-disburse")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_DISBURSE_TYPE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "AUTO_DISB_STATUS_CREATE",
            3,
            true,
            true,
            webClientTimeoutProperties.getSmall(),
            eventContext);
    return webClientFactory
        .postDataWithForBiddenHandling(
            uri, autoDisbursalDTO, getM2pHeaders(), Object.class, webClientParameters)
        .onErrorResume(
            ForbiddenException.class,
            forbiddenException -> {
              Object responseBody = forbiddenException.getResponseBody();
              if (responseBody instanceof M2pErrorResponseDTO errorResponse
                  && !errorResponse.getErrors().isEmpty()
                  && Objects.equals(
                      errorResponse.getUserMessageGlobalisationCode(),
                      "error.msg.datatable.entry.duplicate")) {
                return updateAutoDisbursalStatusInDatatable(autoDisbursalDTO, loanId);
              }
              return Mono.empty();
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[AUTO_DISB_STATUS] error while adding auto disbursal status, returning success:"
                      + " {}",
                  error.getMessage());
              return Mono.empty();
            })
        .thenReturn(Mono.just(ResponseDTO.builder().status(ResponseStatus.SUCCESS).build()));
  }

  public Mono<?> updateAutoDisbursalStatusInDatatable(Object autoDisbursalDTO, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.update-auto-disburse")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPDATE_DISBURSE_TYPE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null,
            "AUTO_DISB_STATUS_UPDATE",
            3,
            true,
            true,
            webClientTimeoutProperties.getSmall(),
            eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, autoDisbursalDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> markLoanDisbursed(M2pLoanDisburseRequestDTO loanDisburseRequest, String lan) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.mark-loan-application-disbursed")))
            .buildAndExpand(lan)
            .toUriString();
    EventContext eventContext = new EventContext(Event.DISBURSAL_MARKING, lan);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "MARK_LOAN_DISBURSED", 3, true, true, eventContext);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, loanDisburseRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  /**
   * disburse a loan by loan account number (LAN) using the fineract api.
   *
   * @param request the disbursement request containing date, utr, etc.
   * @param loanAccountNumber the loan account number (reference id 2)
   * @return mono with the response
   */
  public Mono<?> disburseLoanByLan(
      M2pDisburseLoanByLanRequestDTO request, String loanAccountNumber) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.disburse-loan-by-lan")))
            .queryParam("command", "disburse")
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    EventContext eventContext = new EventContext(Event.DISBURSAL_MARKING, loanAccountNumber);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "DISBURSE_LOAN_BY_LAN", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, request, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> updateVerifyBankDetails(String clientId, Object data) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.penny-drop-datatable")))
            .buildAndExpand(clientId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.UPDATE_VERIFY_BANK_DETAILS_IN_M2P, null, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "UPDATE_VERIFY_BANK_DETAILS_IN_M2P", 2, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, data, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> updatePanDeatils(String leadId, PanDetails panDetails) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.pan-digilocker-data-table")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPDATE_PAN_DETAILS_IN_M2P, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "UPDATE_PAN_DETAILS_IN_M2P", 2, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, panDetails, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> updateRiskCategorisationTable(String clientId, Object requestBody) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.update-risk-categorisation-table")))
            .queryParam(GENERIC_RESULT_SET, "true")
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "UPDATE_RISK_CATEGORISATION_TABLE_IN_M2P", 2, true, true, null);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, requestBody, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> updateRiskCategorisationTablePut(String clientId, Object requestBody) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.update-risk-categorisation-table")))
            .queryParam(GENERIC_RESULT_SET, "true")
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "PUT_RISK_CATEGORISATION_TABLE_IN_M2P", 2, true, true, null);
    return webClientFactory.putDataWithoutStringSerialization(
        uri, requestBody, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> updateRiskOperationTable(
      RiskOperationTableUpdateRequest riskOperationTableUpdateRequest, String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.risk-operation-datatable")))
            .queryParam(GENERIC_RESULT_SET, "true")
            .buildAndExpand(loanApplicationId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.RISK_OPERATION_TABLE_UPDATE);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "UPDATE_RISK_OPERATION_TABLE_IN_M2P", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, riskOperationTableUpdateRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> addClientDependentDataTable(ClientDependentDTO clientDependentDTO, String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.store-client-dependent-datatable")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.STORE_CLIENT_DEPENDENT, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "STORE_CLIENT_DEPENDENT", 3, true, true, eventContext);
    return webClientFactory.postDataWithForBiddenHandling(
        uri, clientDependentDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> persistPanVerificationResults(
      PanVerificationLog panVerificationLog, String leadId) {
    String baseUrl = environment.getProperty("m2p.api.store-pan-verification-results");

    if (baseUrl == null) {
      log.error(
          "[PAN_VERIFY][STORE_PAN_VERIFICATION_RESULT] Missing config"
              + " property 'm2p.api.store-pan-verification-results'");
      return Mono.just("SKIPPED");
    }

    String uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("genericResultSet", true)
            .buildAndExpand(leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "STORE_PAN_VERIFICATION_RESULTS", 3, true, true, null);
    return webClientFactory.postDataWithForBiddenHandling(
        uri, panVerificationLog, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> persistValidationFunnelResult(
      ValidationFunnelVerificationResultCallbackLog data, String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "m2p.api.store-validation-funnel-verification-results")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "STORE_VALIDATION_FUNNEL_RESULT", 3, true, true, null);
    return webClientFactory.postDataWithForBiddenHandling(
        uri, data, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2pGetKycStatusResponseDTO> getKycStatus(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-kyc-status")))
            .queryParam(R_LOAN_APPLICATION_ID, loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_KYC_STATUS, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_KYC_STATUS", 3, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pGetKycStatusResponseDTO>() {},
            webClientParameters)
        .next()
        .switchIfEmpty(Mono.just(new M2pGetKycStatusResponseDTO()));
  }

  public Mono<ClientDetailsCpRpsResponseDto> getCpRpsLeadData(String leadId, String accountNo) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-cp-rps-lead")))
            .queryParam("R_clientId", leadId)
            .queryParam("R_loanId", accountNo)
            .queryParam("associations", "hierarchyLookup")
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_CP_RPS_LEAD", 0, false, false, null);

    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), ClientDetailsCpRpsResponseDto.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, "GET_CP_RPS_LEAD"));
  }

  public Mono<LoanInsuranceRequestDto> getLoanInsuranceData(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-loan-insurance")))
            .queryParam("R_loanId", loanId)
            .queryParam("associations", "hierarchyLookup")
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LOAN_INSURANCE_DETAILS", 0, false, false, null);

    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), LoanInsuranceRequestDto.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, "GET_LOAN_INSURANCE_DETAILS"));
  }

  public Mono<ClientDetailsCpResponseDto> getCpLeadData(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-cp-lead")))
            .queryParam("R_clientId", leadId)
            .queryParam("associations", "hierarchyLookup")
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_CP_LEAD", 0, false, false, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), ClientDetailsCpResponseDto.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, "GET_CP_LEAD"));
  }

  public Mono<ClientDetailsResponseDto> getLeadData(Map<String, String> queryParams) {
    MultiValueMap<String, String> multiValueMap =
        CollectionUtils.toMultiValueMap(
            queryParams.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue()))));

    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-lead")))
            .queryParams(multiValueMap)
            .queryParam("associations", "hierarchyLookup")
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, GET_LEAD, 0, false, false, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), ClientDetailsResponseDto.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, GET_LEAD));
  }

  public Mono<Object> getVkycStatus(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-vkyc-status")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_VKYC_STATUS, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_VKYC_STATUS", 3, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> initiateVkyc(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.initiate-vkyc")))
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.INITIATE_VKYC, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "INITIATE_VKYC", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, null, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Flux<LeadIdResponse> getLeadInfoWithDOB(String mobileNumber, String dateOfBirth) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-lead-against-mobilenumber-dob")))
            .queryParam("R_mobileNumber", mobileNumber)
            .queryParam("R_dateOfBirth", dateOfBirth)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LEAD_INFO_DOB", 0, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), LeadIdResponse.class, webClientParameters);
  }

  public Flux<LeadIdResponse> getLeadInfoWithDOBAndPAN(
      String mobileNumber, String dateOfBirth, String panLast4Digits) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty(
                        "m2p.api.reports.get-lead-against-mobilenumber-dob-pan")))
            .queryParam("R_mobileNumber", mobileNumber)
            .queryParam("R_dateOfBirth", dateOfBirth)
            .queryParam("R_panLast4Digits", panLast4Digits)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LEAD_INFO_DOB_PAN", 0, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), LeadIdResponse.class, webClientParameters);
  }

  public Mono<List<LoanExternalIdResponseDTO>> getLoanByExternalId(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-loan-externalId")))
            .queryParam(R_EXTERNAL_ID, loanId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LOAN_BY_EXTERNAL_ID", 3, true, true, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), LoanExternalIdResponseDTO.class, webClientParameters)
        .collectList()
        .doOnSuccess(list -> logSuccess(list, "GET_LOAN_BY_EXTERNAL_ID"));
  }

  public Mono<List<DisbursementStatusV2ResponseDTO>> getLoanDisbursementStatusV2(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-disbursement-status-v2")))
            .queryParam(R_LOAN_APPLICATION_ID, loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_LOAN_DISBURSE_STATUS_V2, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_LOAN_DISBURSE_STATUS_V2", 3, true, true, eventContext);

    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), DisbursementStatusV2ResponseDTO.class, webClientParameters)
        .collectList()
        .doOnSuccess(list -> logSuccess(list, "GET_LOAN_DISBURSE_STATUS_V2"));
  }

  public Mono<?> getKycUseDetails(String clientId, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-kyc-reuse-details")))
            .queryParam("R_clientId", clientId)
            .queryParam("R_loanId", loanId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.KYC_STATUS_PREVIOUS_LOAN_APPLICATION, loanId, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "KYC_STATUS_PREVIOUS_LOAN_APPLICATION", 0, true, true, eventContext);
    return webClientFactory.getData(uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<String> getLatestAadhaarXml(String leadId, String docId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-aadhaar-by-docid")))
            .buildAndExpand(leadId, docId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DOCUMENT_ID", 0, true, true, null);
    return webClientFactory.getData(uri, getM2pHeaders(), String.class, webClientParameters);
  }

  public Mono<DocIdDTO> getLatestAadhaarXmlDocId(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-latest-aadhaar-document-id")))
            .queryParam("R_clientId", leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.GET_LATEST_AADHAARXML, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LATEST_AADHAARXML", 0, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<DocIdDTO>() {},
            webClientParameters)
        .collectMap(DocIdDTO::getId)
        .flatMap(
            docIddtoMap -> {
              if (!docIddtoMap.isEmpty()) {
                for (Map.Entry<String, DocIdDTO> entry : docIddtoMap.entrySet()) {
                  return Mono.just(entry.getValue());
                }
              }
              return null;
            });
  }

  public Mono<GetDocketDetailsResponseDto> getLoanAndClientDetailsForDocketPopulationByLoanId(
      String loanId) {
    try {
      long parsedLoanId = Long.parseLong(loanId);
      if (parsedLoanId <= 0 || parsedLoanId > 999999999L) {
        return Mono.error(new IllegalArgumentException("Invalid Loan ID range"));
      }

      String uri =
          UriComponentsBuilder.fromUriString(
                  requireNonNull(environment.getProperty("m2p.api.reports.get-docket-details")))
              .queryParam("R_loanApplicationId", parsedLoanId)
              .toUriString();

      WebClientParameters webClientParameters =
          util.getWebClientParameters(null, "DOCKET_DETAILS", 0, true, true, null);

      return webClientFactory
          .getFluxDataWithTypeRef(
              uri,
              getM2pHeaders(),
              new ParameterizedTypeReference<GetDocketDetailsResponseDto>() {},
              webClientParameters)
          .next()
          .switchIfEmpty(Mono.just(new GetDocketDetailsResponseDto()));
    } catch (NumberFormatException e) {
      return Mono.error(new IllegalArgumentException("Invalid Loan ID format"));
    }
  }

  public Mono<List<M2PDisbursementCheckDetailDTO>> getDisbursalCheckData(String leadId) {

    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-disbursement-check-data")))
            .queryParam(R_LOAN_APPLICATION_ID, leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DISB_CHECK_DATA", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), M2PDisbursementCheckDetailDTO.class, webClientParameters)
        .collectList()
        .doOnSuccess(
            resList -> {
              objectMapper.registerModule(
                  new JavaTimeModule()); // handles LocalDate, LocalDateTime, etc.
              objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
              try {
                String jsonString = objectMapper.writeValueAsString(resList);
                logSuccess(jsonString, "GET_DISB_CHECK_DATA");
              } catch (JsonProcessingException e) {
                log.error("Error serializing response for logging", e);
              }
            });
  }

  public Flux<BreApprovedLoanDTO> getBreApprovedLoans() {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-bre-approved-loans")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "OFFER_EXPIRY", 0, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), BreApprovedLoanDTO.class, webClientParameters);
  }

  public Mono<Object> getRiskCategorisationTable(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.update-risk-categorisation-table")))
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_RISK_CATEGORISATION_TABLE_IN_M2P", 2, true, true, null);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<ExperianBureauCtaUpdateResponse> updateExperianReportData(
      String leadId, Object bureauData) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.update-experian-report-data")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(leadId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.BRE_EXPERIAN_REPORT_UPDATE, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "BRE_EXPERIAN_REPORT_UPDATE", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        bureauData,
        getM2pHeaders(),
        ExperianBureauCtaUpdateResponse.class,
        webClientParameters);
  }

  public Flux<AddressFetchResponse> fetchClientAddresses(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-addresses-by-client")))
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_ADDRESS_BY_CLIENT", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), AddressFetchResponse.class, webClientParameters)
        .doOnNext(res -> logSuccess(res, "GET_ADDRESS_BY_CLIENT"));
  }

  public Mono<Object> generateCreditLine(
      Mono<M2PGenerateCreditLineRequestDTO> generateBNPLRequestDTOMono, String loanId) {

    String uri =
        UriComponentsBuilder.fromUriString(environment.getProperty("m2p.api.create-credit-line"))
            .buildAndExpand(loanId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.GENERATE_CREDIT_LINE);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GENERATE_CREDIT_LINE", 0, true, true, eventContext);

    return generateBNPLRequestDTOMono.flatMap(
        dto ->
            webClientFactory.postDataWithoutStringSerialization(
                uri, dto, getM2pHeaders(), Object.class, webClientParameters));
  }

  public Mono<Object> fetchCreditLine(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.fetch-credit-line")))
            .buildAndExpand(loanId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "FETCH_CREDIT_LINE", 0, true, true, null);

    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> activateCreditLine(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.activate-credit-line")))
            .buildAndExpand(loanId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.ACTIVATE_CREDIT_LINE);

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ACTIVATE_CREDIT_LINE", 3, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, Collections.emptyMap(), getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<Object> approveCreditLine(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.approve-credit-line")))
            .buildAndExpand(loanId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.APPROVE_CREDIT_LINE);

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "APPROVE_CREDIT_LINE", 3, true, true, eventContext);

    return webClientFactory.putDataWithoutStringSerialization(
        uri, Collections.emptyMap(), getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<M2PDrawdownResponse> triggerDrawdown(
      String loanAccountNumber, M2PDrawdownRequest data) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.drawdown-credit-line")))
            .buildAndExpand(loanAccountNumber)
            .toUriString();

    EventContext eventContext = new EventContext(Event.DRAWDOWN_CREDIT_LINE);

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "DRAWDOWN_CREDIT_LINE", 3, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, data, getM2pHeaders(), M2PDrawdownResponse.class, webClientParameters);
  }

  /**
   * Approves a drawdown transaction. Endpoint: POST
   * {BaseUrl}/{lineId}/transactions/{transactionId}/approve
   *
   * @param lineId the credit line ID
   * @param transactionId the transaction ID to approve
   * @param data the approval request data (M2PDrawdownApproveRequest)
   * @return the approval response
   */
  public Mono<Object> approveDrawdown(String lineId, String transactionId, Object data) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.approve-drawdown")))
            .buildAndExpand(lineId, transactionId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.APPROVE_DRAWDOWN);

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "APPROVE_DRAWDOWN", 3, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, data, getM2pHeaders(), Object.class, webClientParameters);
  }

  /**
   * Fetches drawdown callback details (disbursal details) from M2P report API. Similar to
   * getAmlPepResult() - uses DrawdownCallbackDetails report.
   *
   * @param transactionId the transaction ID to fetch details for
   * @return the drawdown callback details
   */
  public Mono<DrawdownCallbackDetailsDTO> getDrawdownCallbackDetails(String transactionId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.drawdown-callback-details")))
            .queryParam("R_transactionId", transactionId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "DRAWDOWN_CALLBACK_DETAILS", 0, true, true, null);

    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), DrawdownCallbackDetailsDTO.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException("Drawdown callback details not found")));
  }

  /**
   * Rejects a drawdown transaction. Endpoint: POST
   * {BaseUrl}/{lineId}/transactions/{transactionId}/reject
   *
   * @param lineId the credit line ID
   * @param transactionId the transaction ID to reject
   * @param data the rejection request data (M2PDrawdownRejectRequest)
   * @return the rejection response
   */
  public Mono<Object> rejectDrawdown(String lineId, String transactionId, Object data) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.reject-drawdown")))
            .buildAndExpand(lineId, transactionId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.REJECT_DRAWDOWN);

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "REJECT_DRAWDOWN", 3, true, true, eventContext);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, data, getM2pHeaders(), Object.class, webClientParameters);
  }

  private Mono<?> addMerchantDetails(M2pAdditionalDetailsDTO additionalDetails, String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.add-merchant-details")))
            .buildAndExpand(clientId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.ADD_MERCHANT, null, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "ADD_MERCHANT", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        additionalDetails,
        getM2pHeaders(),
        M2pClientIdTypeResponseDTO.class,
        webClientParameters);
  }

  public Flux<ActiveLoanDTO> getActiveLoans() {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-active-loans")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_ACTIVE_LOANS", 0, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), ActiveLoanDTO.class, webClientParameters);
  }

  public Mono<M2pDocumentTagDTO> getLatestBusinessProofDocumentByClientId(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-latest-business-proof-document")))
            .queryParam("R_clientId", clientId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.PSL_TAGGING, null, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "GET_LATEST_BUSINESS_PROOF_DOCUMENT", 0, true, true, eventContext);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pDocumentTagDTO>() {},
            webClientParameters)
        .next()
        .switchIfEmpty(Mono.just(new M2pDocumentTagDTO()));
  }

  public Mono<Object> updateAAIdentityMatchResultDataToM2P(
      String loanId, Object aaIdentityMatchResultDatatableDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.aa-identity-match-result-datatable")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.AA_IDENTITY_MATCH_M2P_UPDATE, loanId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "AA_IDENTITY_MATCH_RESULT_M2P_UPDATE", 3, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, aaIdentityMatchResultDatatableDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Flux<DisbursalBatchDataResponse> getDisbursalBatchDataM2p(String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-disbursal-batch-data")))
            .queryParam("R_loanApplicationId", loanApplicationId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DISBURSAL_BATCH_DATA", 3, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), DisbursalBatchDataResponse.class, webClientParameters);
  }

  public Flux<DisbursalBatchDataResponse> getDisbursalBatchDataM2pForLineProducts(
      String transactionId, String anchorId, String lineId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-disbursal-batch-data-scf")))
            .queryParam("R_loanApplicationId", transactionId)
            .queryParam("R_batchIdentifier", anchorId)
            .queryParam("R_accountNumber", lineId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DISBURSAL_BATCH_DATA", 3, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), DisbursalBatchDataResponse.class, webClientParameters);
  }

  public Flux<DisbursalAmountResponse> getDisbursalAmountScf(String referenceId1) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-disbursal-amount-scf")))
            .queryParam("R_transactionId", referenceId1)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DISBURSAL_AMOUNT_SCF", 3, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), DisbursalAmountResponse.class, webClientParameters);
  }

  public Flux<DisbursalAmountResponse> getDisbursalAmount(String referenceId1) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-disbursal-amount")))
            .queryParam("R_loanApplicationId", referenceId1)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DISBURSAL_AMOUNT", 3, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), DisbursalAmountResponse.class, webClientParameters);
  }

  public Mono<?> rejectLoan(
      String loanApplicationId,
      String loanAccountNumber,
      LoanAccountRejectRequest loanAccountRejectRequest) {
    loanAccountRejectRequest.setDateFormat(REJECT_LOAN_DATE_FORMAT);
    loanAccountRejectRequest.setLocale("en");
    return rejectLoanAccount(loanApplicationId, loanAccountNumber, loanAccountRejectRequest);
  }

  public Mono<?> rejectLoanAccount(
      String loanApplicationId,
      String loanAccountNumber,
      LoanAccountRejectRequest rejectLoanRequest) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.reject-loan")))
            .queryParam("command", "reject")
            .buildAndExpand(loanAccountNumber)
            .toUriString();
    EventContext eventContext = new EventContext(Event.PSL_TAGGING, loanApplicationId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "REJECT_LOAN", 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri, rejectLoanRequest, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Flux<TransactionDetailsDTO> getTransactionDetails(String lineId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-transaction-details")))
            .buildAndExpand(lineId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.TRANSACTION_DETAILS, null, lineId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_TRANSACTION_DETAILS", 0, true, true, eventContext);

    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), TransactionDetailsDTO.class, webClientParameters);
  }

  public Mono<GetLoanLanDetailsResponse> getDetailsByLoanId(String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-loanDetails")))
            .queryParam("R_loanApplicationId", loanId)
            .buildAndExpand()
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LOAN_DETAILS", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), M2pGetLoanV2ResponseDTO.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .flatMap(
            data ->
                Mono.just(
                    GetLoanLanDetailsResponse.builder()
                        .loanApplicationId(data.getLoanApplicationId())
                        .lanId(data.getLoanId())
                        .clientId(data.getClientId())
                        .losProductKey(data.getLosProductKey())
                        .build()))
        .doOnSuccess(res -> logSuccess(res, "GET_LOAN_DETAILS"));
  }

  public Mono<GetLoanLanDetailsResponse> getDetailsByLanId(String lanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-lanDetails")))
            .queryParam("R_lanId", lanId)
            .buildAndExpand()
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LAN_DETAILS", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), M2pGetLoanV2ResponseDTO.class, webClientParameters)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .flatMap(
            data ->
                Mono.just(
                    GetLoanLanDetailsResponse.builder()
                        .loanApplicationId(data.getLoanApplicationId())
                        .lanId(data.getLoanId())
                        .clientId(data.getClientId())
                        .losProductKey(data.getLosProductKey())
                        .build()))
        .doOnSuccess(res -> logSuccess(res, "GET_LAN_DETAILS"));
  }

  public Mono<?> updateQcChecksDataTable(QcChecksDataTableDTO qcChecksDTO, String loanId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.qc-checks-data-table")))
            .queryParam("genericResultSet", true)
            .buildAndExpand(loanId)
            .toUriString();
    EventContext eventContext = new EventContext(Event.UPDATE_QC_CHECKS_DATATABLE, loanId, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "UPDATE_QC_CHECKS_DATATABLE", 3, true, true, eventContext);
    return webClientFactory.postDataWithForBiddenHandling(
        uri, qcChecksDTO, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<ClientImageResponse> getClientImageId(String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-image-tag")))
            .buildAndExpand(clientId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_CLIENT_IMAGE_ID", 2, true, true, null);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), ClientImageResponse.class, webClientParameters);
  }

  public Mono<String> getClientImageByImageId(String clientId, Long imageId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("m2p.api.get-image-by-image-id")))
            .buildAndExpand(clientId, imageId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_IMAGE_BY_IMAGE_ID", 2, true, true, null);
    return webClientFactory.getDataAsTextResponse(
        uri, getM2pHeaders(), String.class, webClientParameters);
  }

  public Mono<M2pGetChargeDetailResponseDto> getChargeDetail(String loanId, Integer chargeId) {

    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.get-charge-detail")))
            .queryParam(R_LOAN_APPLICATION_ID, loanId)
            .queryParam(R_CHARGE_ID, chargeId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_CHARGE_DETAIL", 3, true, true, null);

    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2pGetChargeDetailResponseDto>() {},
            webClientParameters)
        .next()
        .switchIfEmpty(Mono.just(new M2pGetChargeDetailResponseDto()));
  }

  public Flux<TransactionDetailsDTO> getAllTransactionsDetailsOnALine(String lineId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.get-all-transaction-details")))
            .buildAndExpand(lineId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.TRANSACTION_DETAILS, null, lineId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_TRANSACTION_DETAILS", 0, true, true, eventContext);

    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), TransactionDetailsDTO.class, webClientParameters);
  }

  public Flux<TransactionDetailsDTO> getActiveTransactionsDetailsOnALine(String lineId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("m2p.api.get-active-transaction-details")))
            .buildAndExpand(lineId)
            .toUriString();

    EventContext eventContext = new EventContext(Event.TRANSACTION_DETAILS, null, lineId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_TRANSACTION_DETAILS", 0, true, true, eventContext);

    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), TransactionDetailsDTO.class, webClientParameters);
  }

  public Mono<M2PActivationCheckDetailDTO> getActivationCheckData(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-activation-check-data")))
            .queryParam(R_LOAN_APPLICATION_ID, leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_DISB_CHECK_DATA", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2PActivationCheckDetailDTO>() {},
            webClientParameters)
        .next()
        .switchIfEmpty(Mono.just(new M2PActivationCheckDetailDTO()));
  }

  public Mono<M2PApplicationTypeFundlyDTO> getApplicationTypeForFundly(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.application-type-fundly")))
            .queryParam(R_LOAN_APPLICATION_ID, leadId)
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_APPLICATION_TYPE_SCF", 0, true, true, null);
    return webClientFactory
        .getFluxDataWithTypeRef(
            uri,
            getM2pHeaders(),
            new ParameterizedTypeReference<M2PApplicationTypeFundlyDTO>() {},
            webClientParameters)
        .next()
        .switchIfEmpty(Mono.just(new M2PApplicationTypeFundlyDTO()));
  }

  public Flux<RequestedLoanDetailsResponse> getRequestedLoanDetailsM2p(String loanApplicationId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.reports.requested_loan_details")))
            .queryParam("R_loanApplicationId", loanApplicationId)
            .toUriString();

    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_REQUESTED_LOAN_DETAILS", 3, true, true, null);
    return webClientFactory.getFluxDataWithoutStringSerialization(
        uri, getM2pHeaders(), RequestedLoanDetailsResponse.class, webClientParameters);
  }

  public Mono<Object> getDrawdownPerformanceReport(String accountNumber, String reportApi) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(
                    environment.getProperty("m2p.api.reports.get-drawdown-performance-report")))
            .queryParam("R_accountNumber", accountNumber)
            .buildAndExpand(reportApi)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.CLIENT_LOAN_PERFORMANCE_REPORT, accountNumber, null);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "DRAWDOWN_LOAN_PERFORMANCE_REPORT", 0, true, true, eventContext);
    return webClientFactory.getDataWithoutStringSerialization(
        uri, getM2pHeaders(), Object.class, webClientParameters);
  }

  public Mono<LoanClassificationDetailsM2pResponse> postLoanClassificationDetailsToM2p(
      String clientId, LoanClassificationDetailsM2pRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.loan-type-classification-table")))
            .buildAndExpand(clientId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.POST_LOAN_CLASSIFICATION_DETAILS_IN_M2P, null, clientId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(
            null, "POST_LOAN_CLASSIFICATION_DETAILS_IN_M2P", 2, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        request,
        getM2pHeaders(),
        LoanClassificationDetailsM2pResponse.class,
        webClientParameters);
  }

  public Mono<Lead> getLeadFullData(String leadId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("m2p.api.get-lead-full")))
            .queryParam("R_clientId", leadId)
            .queryParam("associations", "hierarchyLookup")
            .toUriString();

    EventContext eventContext = new EventContext(Event.GET_LEAD_FULL, null, leadId);
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "GET_LEAD_FULL", 0, false, false, eventContext);

    return webClientFactory
        .getFluxDataWithoutStringSerialization(
            uri, getM2pHeaders(), JsonNode.class, webClientParameters)
        .next()
        .flatMap(
            node -> {
              try {
                String rawLeadJson = node.get("lead_dto_json").asText();

                Lead lead = objectMapper.readValue(rawLeadJson, Lead.class);
                return Mono.just(lead);
              } catch (JsonProcessingException | NullPointerException e) {
                log.error(
                    "[GET_LEAD_FULL][ERROR] Mapping failure for leadId: {}. Error: {}",
                    leadId,
                    e.getMessage());
                return Mono.error(
                    new BaseException(
                        PARSING_ERROR, PARSING_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
              }
            })
        .switchIfEmpty(Mono.error(new NotFoundException(NOT_FOUND)))
        .doOnSuccess(res -> logSuccess(res, "GET_LEAD_FULL"));
  }

  public Mono<M2pPlatformHealthResponse> getM2pHealthStatus() {
    String uri =
            UriComponentsBuilder.fromUriString(
                            requireNonNull(environment.getProperty("m2p.api.health-status")))
                    .toUriString();
    WebClientParameters webClientParameters =
            util.getWebClientParameters(null, "GET_M2P_HEALTH_STATUS", 0, false, false, null);
    return webClientFactory
            .getFluxDataWithTypeRef(
                    uri,
                    getM2pHeaders(),
                    new ParameterizedTypeReference<M2pPlatformHealthResponse>() {
                    },
                    webClientParameters)
            .next()
            .switchIfEmpty(Mono.just(new M2pPlatformHealthResponse()));


  }
}
