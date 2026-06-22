package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.AMOUNT;
import static com.trillionloans.los.constant.StringConstants.AMOUNT_EXCEEDS_APPROVED;
import static com.trillionloans.los.constant.StringConstants.AMOUNT_MISMATCH_BRE;
import static com.trillionloans.los.constant.StringConstants.APPROVED;
import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.BASE64;
import static com.trillionloans.los.constant.StringConstants.BRE_AMT_CHECK;
import static com.trillionloans.los.constant.StringConstants.BRE_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.BRE_PF_CHECK;
import static com.trillionloans.los.constant.StringConstants.BRE_QC;
import static com.trillionloans.los.constant.StringConstants.BRE_ROI_CHECK;
import static com.trillionloans.los.constant.StringConstants.BRE_STATUS;
import static com.trillionloans.los.constant.StringConstants.BRE_TENURE_CHECK;
import static com.trillionloans.los.constant.StringConstants.BUSINESS_LOAN_CONFIG_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.CANNOT_BE_DONE;
import static com.trillionloans.los.constant.StringConstants.CLIENT_ERROR;
import static com.trillionloans.los.constant.StringConstants.COMPLETED;
import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;
import static com.trillionloans.los.constant.StringConstants.DUPLICATE_EXTERNAL_ID_MESSAGE_CODE;
import static com.trillionloans.los.constant.StringConstants.ELIGIBLE;
import static com.trillionloans.los.constant.StringConstants.EMPTY_FILE_CONTENT;
import static com.trillionloans.los.constant.StringConstants.ERROR_CODE;
import static com.trillionloans.los.constant.StringConstants.ERROR_NO_LOAN_FOUND;
import static com.trillionloans.los.constant.StringConstants.FAIL;
import static com.trillionloans.los.constant.StringConstants.FI_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.INELIGIBLE;
import static com.trillionloans.los.constant.StringConstants.INVALID_VALIDATION_PARAMETER;
import static com.trillionloans.los.constant.StringConstants.IN_PROGRESS;
import static com.trillionloans.los.constant.StringConstants.IS_INSURANCE_CHARGE_ADDED;
import static com.trillionloans.los.constant.StringConstants.KYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.LIMIT_BRE;
import static com.trillionloans.los.constant.StringConstants.LOAN_CREATE_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.LOAN_CREATION_FAIL;
import static com.trillionloans.los.constant.StringConstants.LOGGING_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.M2P_ERROR_CODE;
import static com.trillionloans.los.constant.StringConstants.MULTIPLE_BRE;
import static com.trillionloans.los.constant.StringConstants.NO_AMOUNT_IN_BRE;
import static com.trillionloans.los.constant.StringConstants.NO_BRE_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.NO_DISBURSAL_BANK_ACCOUNT;
import static com.trillionloans.los.constant.StringConstants.NO_FAILED_CASE_FOR_RISK_CATEGORIZATION_FOUND;
import static com.trillionloans.los.constant.StringConstants.OFFER_DOWNGRADE;
import static com.trillionloans.los.constant.StringConstants.OFFER_DOWN_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.OKYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.PAN_AADHAAR_LINKAGE_AUTO_DISB_STATUS;
import static com.trillionloans.los.constant.StringConstants.PAN_AADHAAR_LINKAGE_HEADER;
import static com.trillionloans.los.constant.StringConstants.PAN_AADHAAR_NOT_LINKED;
import static com.trillionloans.los.constant.StringConstants.PAN_NOT_MATCHED;
import static com.trillionloans.los.constant.StringConstants.PAN_VERIFY;
import static com.trillionloans.los.constant.StringConstants.PASS;
import static com.trillionloans.los.constant.StringConstants.PENNY_DROP_NAME_MATCH_REJECTION;
import static com.trillionloans.los.constant.StringConstants.PRE_DISBURSAL_VALIDATION;
import static com.trillionloans.los.constant.StringConstants.PRE_DISB_QC;
import static com.trillionloans.los.constant.StringConstants.PROCESSING_FEE;
import static com.trillionloans.los.constant.StringConstants.REJECTED;
import static com.trillionloans.los.constant.StringConstants.RISK_CATEGORIZATION_RETRY;
import static com.trillionloans.los.constant.StringConstants.RISK_CATEGORIZATION_RETRY_FAILED;
import static com.trillionloans.los.constant.StringConstants.RISK_LOAN_APPLICATION_NOT_FOUND;
import static com.trillionloans.los.constant.StringConstants.RISK_RETRY_PROCESSED_SUCCESSFULLY;
import static com.trillionloans.los.constant.StringConstants.SCIENAPTIC_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.SINGLE_BRE;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.START_KYC_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.constant.StringConstants.TENURE;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.UPDATE_LOAN_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.UPDATE_LOAN_LOG_HEADER;
import static com.trillionloans.los.constant.StringConstants.UPLOAD_DOC_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.UPLOAD_DOC_LOAN;
import static com.trillionloans.los.constant.StringConstants.UPLOAD_NACH_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.UPLOAD_NACH_LOAN;
import static com.trillionloans.los.constant.StringConstants.USER_MESSAGE_CODE;
import static com.trillionloans.los.constant.StringConstants.VERIFIED;
import static com.trillionloans.los.util.DateTimeConverterUtil.convertEpochMilliToIst;
import static com.trillionloans.los.util.JsonUtils.extractFieldValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.config.BharatPeProductConfig;
import com.trillionloans.los.config.InsuranceConfig;
import com.trillionloans.los.config.RejectionReasonCodeFactory;
import com.trillionloans.los.config.RiskCodeConfig;
import com.trillionloans.los.constant.*;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.exception.PanValidationExceptions.PanBasedLoanRejectionException;
import com.trillionloans.los.mapper.OpvResponseCode;
import com.trillionloans.los.mapper.PanValidationServiceType;
import com.trillionloans.los.model.AttachBankDetailsDTO;
import com.trillionloans.los.model.ClientCacheDTO;
import com.trillionloans.los.model.PanAadhaarLinkStatusDataTableDTO;
import com.trillionloans.los.model.PanVerificationResult;
import com.trillionloans.los.model.dto.ApproveLoanDTO;
import com.trillionloans.los.model.dto.GetDocketDetailsResponseDto;
import com.trillionloans.los.model.dto.LoanChargesDTO;
import com.trillionloans.los.model.dto.PanVerificationLog;
import com.trillionloans.los.model.dto.TopUpDataTableDTO;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.LoanFunnelDTO;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.*;
import com.trillionloans.los.model.partner.m2p.M2pBulkDocumentsUploadDTO;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.request.AgreementDocumentUploadRequest;
import com.trillionloans.los.model.request.BulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.BusinessLoanDetailsDTO;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.request.KycUploadDocumentRequest;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.request.LoanBankAccountDataTableDTO;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.NachMandateRequest;
import com.trillionloans.los.model.request.SaveChargeRequest;
import com.trillionloans.los.model.request.TopupDataRequest;
import com.trillionloans.los.model.request.UpdateLoanApplication;
import com.trillionloans.los.model.request.m2p.M2pConsentRequest;
import com.trillionloans.los.model.response.BreStatusResponse;
import com.trillionloans.los.model.response.BusinessLoanUpdateResponse;
import com.trillionloans.los.model.response.GetLoanLanDetailsResponse;
import com.trillionloans.los.model.response.GetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.PanAadhaarLinkageResult;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.InternalLoanCreationResult;
import com.trillionloans.los.model.response.m2p.M2PDisbursementCheckDetailDTO;
import com.trillionloans.los.model.response.m2p.M2pAddBankDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pDocumentsUploadResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pErrorResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pGetKycStatusResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanRejectResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pResourceIdTypeResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pResourceResponseDTO;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import com.trillionloans.los.repository.AmlPepResultsRepository;
import com.trillionloans.los.repository.LoanApplicationRestructureDetailsRepository;
import com.trillionloans.los.repository.LoanClientPartnerMapRepository;
import com.trillionloans.los.repository.LoanInsuranceDetailsRepository;
import com.trillionloans.los.repository.PanAadhaarLinkageRepository;
import com.trillionloans.los.repository.RiskCategorizationFailureRepository;
import com.trillionloans.los.service.ckyc.AadhaarXmlService;
import com.trillionloans.los.service.db.BreStatusService;
import com.trillionloans.los.service.db.LeadMiscellaneousDetailsService;
import com.trillionloans.los.service.db.LoanApplicationMiscellaneousDetailsService;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.QcCheckStoreService;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaFunnelLoggingService;
import com.trillionloans.los.service.validationservice.DOBWaterfallValidationService;
import com.trillionloans.los.service.validationservice.ValidationFunnelService;
import com.trillionloans.los.util.DateValidationUtil;
import com.trillionloans.los.util.FileValidatorUtil;
import com.trillionloans.los.util.LoanDataUtil;
import com.trillionloans.los.util.PanValidationUtil;
import io.r2dbc.postgresql.codec.Json;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@AllArgsConstructor
@Slf4j
public class LoanApplicationService {

  private final M2PWrapperApi m2PWrapperApi;
  private final Gson gson;
  private final ProductConfigMasterService productConfigMasterService;
  private final BreStatusService breStatusService;
  private final LeadService leadService;
  private final Environment environment;
  private final RiskCategorizationFailureRepository riskCategorizationFailureRepository;
  private final M2pFacadeService m2pFacadeService;
  private final AadhaarXmlService aadhaarXmlService;
  private final RejectionReasonCodeFactory reasonCodeFactory;
  private final PartnerMasterService partnerMasterService;
  private final ClientCacheService clientCacheService;
  private final KafkaFunnelLoggingService kafkaFunnelLoggingService;
  private final PanAadhaarLinkageRepository panAadhaarLinkageRepository;
  private final AmlPepValidationService amlPepValidationService;
  private final AmlPepResultsRepository amlPepResultsRepository;
  private final LoanClientPartnerMapRepository loanClientPartnerMapRepository;
  private final LoanApplicationCacheService loanApplicationCacheService;
  private final QcCheckStoreService qcCheckStoreService;
  private RiskCodeConfig riskCodeConfig;
  private final BharatPeProductConfig bharatPeProductConfig;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final DOBWaterfallValidationService dobWaterfallValidationService;
  private final BankVerificationService bankVerificationService;
  private final ValidationFunnelService validationFunnelService;
  private final StandalonePanValidationService standalonePanValidationService;
  private final KafkaEventProducerService eventProducerService;

  private static final String LOAN_APPLICATION_LOG_LITERAL = " loan application id: {}";
  private static final String APPLICATION_LOG_LITERAL = " application id: {}";
  private static final String CLIENT_ID_LOG_LITERAL = " client id: {}";
  private final InsuranceService insuranceService;
  private final LoanInsuranceDetailsRepository loanInsuranceDetailsRepository;
  private final DocS3UploadService docS3UploadService;
  private final DigioRestructureEsignService digioRestructureEsignService;
  private final TagEligibilityValidatorRegistry tagEligibilityValidatorRegistry;
  private final LoanApplicationRestructureDetailsRepository restructureDetailsRepository;
  private final LoanApplicationMiscellaneousDetailsService
      loanApplicationMiscellaneousDetailsService;
  private final LeadMiscellaneousDetailsService leadMiscellaneousDetailsService;
  private final LoanClientLookupService loanClientLookupService;
  private final FunnelRuleService funnelRuleService;
  private final BusinessLoanEvaluationService businessLoanEvaluationService;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  public Mono<M2pLoanCreationResponseDTO> proceedWithLoanApplication(
      LoanApplication loanData,
      String leadId,
      String productCode,
      boolean ctaRegistrationRequested) {
    return createLoanApplication(loanData, leadId, productCode)
        .map(dto -> new InternalLoanCreationResult(dto, false)) // normal result
        .onErrorResume(
            error ->
                handleDuplicateExternalIdError(
                        error, loanData.getExternalId(), loanData.getLosProductKey())
                    .map(fallbackDto -> new InternalLoanCreationResult(fallbackDto, false)))
        .flatMap(
            result -> {
              if (result.isFallback()) {
                return Mono.just(
                    result.m2pLoanCreationResponseDTO()); // return early, skip further processing
              }
              M2pLoanCreationResponseDTO data = result.m2pLoanCreationResponseDTO();
              if (Objects.isNull(data) || Objects.isNull(data.getResourceId())) {
                return Mono.error(
                    new BaseException(LOAN_CREATION_FAIL, null, HttpStatus.INTERNAL_SERVER_ERROR));
              }
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlConfigData -> {
                        ProductControl.Flow flowData =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControlConfigData.getT2(), LOAN_CREATE_CTA_IDENTIFIER);
                        if (Objects.isNull(flowData)) {
                          return Mono.error(
                              new BaseException(
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                        Integer loanId = data.getResourceId();
                        if (ctaRegistrationRequested && flowData.isCtaCallFlag()) {
                          return m2PWrapperApi
                              .registerCta(loanId.toString(), flowData.getCtaName())
                              .flatMap(ctaResponse -> Mono.just(data));
                        }
                        return Mono.just(data);
                      });
            });
  }

  public Mono<M2pLoanCreationResponseDTO> triggerLoanAppCreationBasedOnPanValidationServiceType(
      LoanApplication request, String leadId, String productCode) {

    return validationFunnelService
        .determineActiveService(productCode)
        .flatMap(
            serviceType -> {
              Mono<M2pLoanCreationResponseDTO> loanCreationMono;

              if (serviceType == PanValidationServiceType.VALIDATION_FUNNEL) {
                log.info("[CREATE_LOAN_APPLICATION] Validation service type: VALIDATION_FUNNEL");
                loanCreationMono =
                    createLoanApplicationWithPhase2PanValidation(
                        request, leadId, productCode, true);

              } else if (serviceType == PanValidationServiceType.STANDALONE_NSDL_PAN_VALIDATION) {
                log.info(
                    "[CREATE_LOAN_APPLICATION] Validation service type:"
                        + " STANDALONE_NSDL_PAN_VALIDATION");
                loanCreationMono =
                    createLoanApplicationWithPhase1PanValidation(request, leadId, productCode);

              } else {
                log.info("[CREATE_LOAN_APPLICATION] Validation service type: NO_ACTIVE_SERVICE");
                loanCreationMono = createLoanApplication(request, leadId, productCode, true);
              }

              return loanCreationMono.doOnSuccess(
                  response -> {
                    saveLoanApplicationMiscellaneousDetailsAsync(
                        response.getResourceId(), response.getClientId(), request);
                    initializeLoanClassificationIfBusinessLoanAsync(
                        String.valueOf(response.getResourceId()), request.getLosProductKey());
                  });
            });
  }

  /**
   * Async (fire-and-forget): if product has BUSINESS_LOAN_CONFIG with isBusinessLoan=true, creates
   * an entry in loan_type_classification with DATA_PENDING if not already exists.
   */
  private void initializeLoanClassificationIfBusinessLoanAsync(String loanId, String productCode) {
    productConfigMasterService
        .getProductConfigMasterData(productCode)
        .subscribe(
            productControlTuple -> {
              ProductControl.Flow businessLoanFlow =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlTuple.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
              if (businessLoanFlow != null
                  && Boolean.TRUE.equals(businessLoanFlow.getIsBusinessLoan())) {
                businessLoanEvaluationService.initializeLoanClassificationAsync(
                    loanId, productCode);
              }
            },
            error ->
                log.warn(
                    "[CREATE_LOAN] Failed to resolve product config for business loan init,"
                        + " loanId: {}, productCode: {}",
                    loanId,
                    productCode,
                    error));
  }

  /**
   * Saves miscellaneous details for a loan application asynchronously after successful creation.
   * This method fires and forgets - it doesn't block the response.
   *
   * @param loanApplicationId the loan application ID from the response
   * @param clientId the client ID from the response
   * @param request the original loan application request containing miscellaneous details
   */
  private void saveLoanApplicationMiscellaneousDetailsAsync(
      Integer loanApplicationId, Integer clientId, LoanApplication request) {

    loanApplicationMiscellaneousDetailsService.saveMiscellaneousDetailsAsync(
        loanApplicationId, clientId, request.getLosProductKey(), request.getMiscellaneousDetails());
  }

  public Mono<M2pLoanCreationResponseDTO> createLoanApplicationWithPhase2PanValidation(
      LoanApplication loanData,
      String leadId,
      String productCode,
      boolean ctaRegistrationRequested) {

    return createLoanApplication(loanData, leadId, productCode, ctaRegistrationRequested)
        .doOnSuccess(
            loanCreationResponseDTO -> {
              String loanApplicationId =
                  java.util.Objects.toString(loanCreationResponseDTO.getResourceId(), null);

              String parentTraceId =
                  StringUtils.defaultIfBlank(
                      MDC.get(TRACE_ID), UUID.randomUUID().toString().substring(0, 8));

              loanLevelClientDetailsService
                  .persistToLoanLevelClientDetailsTableAndRedis(
                      loanCreationResponseDTO, leadId, productCode)
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.info(
                                "[LOAN_LEVEL_CLIENT_DATA][M2P_FALLBACK] fetching client details"
                                    + " from M2P for loanApplicationId: {}, clientId: {},"
                                    + " productCode: {}.",
                                loanApplicationId,
                                leadId,
                                productCode);
                            return loanLevelClientDetailsService.fetchClientDetailsFromLeadService(
                                leadId, loanApplicationId, productCode);
                          }))
                  .onErrorResume(
                      e -> {
                        log.error(
                            "[ERROR][LOAN_LEVEL_CLIENT_DATA] DB/Redis persistence failed for"
                                + " loanApplicationId: {}. proceeding to validation funnel anyway."
                                + " error: {}",
                            loanApplicationId,
                            e.getMessage());
                        return loanLevelClientDetailsService.fetchClientDetailsFromLeadService(
                            leadId, loanApplicationId, productCode);
                      })
                  .flatMap(
                      loanLevelClientDetailsCacheDTO -> {
                        // CRITICAL : USE PRODUCT_CODE OF REQUEST BODY ONLY
                        Mono<Void> sideEffects =
                            handlePostLoanCreationSideEffects(
                                loanCreationResponseDTO,
                                leadId,
                                loanData.getLosProductKey(),
                                loanLevelClientDetailsCacheDTO);
                        Mono<Void> validationFunnel =
                            validationFunnelService
                                .runValidationFunnelAtLoanApplicationCreation(
                                    productCode,
                                    leadId,
                                    loanApplicationId,
                                    loanLevelClientDetailsCacheDTO)
                                .then()
                                .doOnError(
                                    e ->
                                        log.error(
                                            "[VALIDATION_FUNNEL][ERROR] failed for leadId={},"
                                                + " loanApplicationId={}, productCode={}, error={}",
                                            leadId,
                                            loanApplicationId,
                                            productCode,
                                            e.getMessage()));
                        return Mono.when(sideEffects, validationFunnel);
                      })
                  .contextWrite(ctx -> ctx.put(TRACE_ID, parentTraceId))
                  .subscribeOn(Schedulers.boundedElastic())
                  .subscribe(
                      success ->
                          log.info(
                              "[POST_LOAN_CREATION][SUCCESS] validation funnel and side effects"
                                  + " completed for leadId: {}, loanApplicationId: {}.",
                              leadId,
                              loanApplicationId),
                      error ->
                          log.error(
                              "[ERROR][POST_LOAN_CREATION] validation funnel and side effects"
                                  + " failed for leadId: {}, loanApplicationId: {}, error: {}.",
                              leadId,
                              loanApplicationId,
                              error.getMessage()));
            });
  }

  public Mono<M2pLoanCreationResponseDTO> createLoanApplicationWithPhase1PanValidation(
      LoanApplication request, String leadId, String productCode) {
    log.info(
        "[LOAN_APPLICATION_CREATION][PAN_VERIFY] Create loan application with PAN validation."
            + " leadId={}, productCode={}",
        leadId,
        productCode);

    // Step 1: Loan creation (materialized: captures success/error as signal)
    Mono<Signal<M2pLoanCreationResponseDTO>> loanSignalMono =
        Mono.defer(() -> createOnlyLoanWithoutCTA(request, leadId, productCode)).materialize();

    // Step 2a: Fetch client details (materialized: captures success/error as signal)
    Mono<Signal<ClientCacheDTO>> clientSignalMono =
        Mono.deferContextual(
            ctx -> {
              return clientCacheService
                  .fetchClientDetails(leadId, productCode, PAN_VERIFY)
                  .contextWrite(ctx)
                  .materialize();
            });

    // Step 2b: PAN validation service (depends on client fetch) (materialized: captures
    // success/error as signal)
    Mono<Tuple2<Optional<ClientCacheDTO>, Signal<PanVerificationResult>>> clientAndPanMono =
        clientSignalMono.flatMap(
            clientSignal -> {
              if (clientSignal.isOnError()) {
                // Client fetch failed → propagate as PAN error signal
                Signal<PanVerificationResult> errorSignal =
                    Signal.error(Objects.requireNonNull(clientSignal.getThrowable()));
                return Mono.just(Tuples.of(Optional.empty(), errorSignal));
              }

              // Client fetch succeeded → perform PAN validation
              ClientCacheDTO clientDetails = clientSignal.get();

              return Mono.fromCallable(
                      () -> PanValidationUtil.buildPanVerificationRequestPhase1(clientDetails))
                  .flatMap(
                      panRequest -> {
                        assert clientDetails != null;
                        return standalonePanValidationService.validatePan(
                            panRequest,
                            clientDetails.getProductCode(),
                            clientDetails.getClientId().toString(),
                            null);
                      })
                  .materialize()
                  .map(
                      panSignal -> {
                        assert clientDetails != null;
                        return Tuples.of(Optional.of(clientDetails), panSignal);
                      });
            });

    // Step 3: 1a + (2a+2b) in parallel
    return Mono.zip(loanSignalMono, clientAndPanMono)
        .flatMap(
            tuple ->
                Mono.deferContextual(
                    parentContext -> {
                      Signal<M2pLoanCreationResponseDTO> loanSignal = tuple.getT1();

                      if (loanSignal.isOnError()) {
                        log.error(
                            "[PAN_VERIFY] Loan creation failed. leadId={}, productCode={},"
                                + " error={}",
                            leadId,
                            productCode,
                            Objects.requireNonNull(loanSignal.getThrowable()).getMessage());
                        return Mono.error(loanSignal.getThrowable());
                      }

                      Tuple2<Optional<ClientCacheDTO>, Signal<PanVerificationResult>> clientAndPan =
                          tuple.getT2();
                      Optional<ClientCacheDTO> clientOpt = clientAndPan.getT1();
                      Signal<PanVerificationResult> panSignal = clientAndPan.getT2();
                      M2pLoanCreationResponseDTO loanResponse = loanSignal.get();

                      if (panSignal.isOnError() || clientOpt.isEmpty()) {
                        // loan is created and the service is down, we hit the CTA if enabled ->
                        // async store
                        // the resp

                        log.warn(
                            "[PAN_VERIFY] PAN validation service unavailable/errored or client"
                                + " fetch failed for leadId={}",
                            leadId);
                        return handlePanServiceUnavailableFlow(
                                loanResponse,
                                leadId,
                                clientOpt.orElse(null),
                                productCode,
                                parentContext)
                            .contextWrite(parentContext);
                      }

                      ClientCacheDTO clientDetails = clientOpt.get();
                      PanVerificationResult panResult = panSignal.get();

                      // PAN validation service responded but was not successful
                      if (Objects.nonNull(panResult)
                          && !OpvResponseCode.SUCCESS
                              .getCode()
                              .equals(panResult.getResponseCode())) {
                        // loan is created and the service is down, we hit the CTA if enabled ->
                        // async store
                        // the resp

                        assert loanResponse != null;
                        log.warn(
                            "[PAN_VERIFY] PAN validation service responded but not successful for"
                                + " leadId={}, loanId={}",
                            leadId,
                            loanResponse.getResourceId());
                        return handlePanServiceUnavailableFlow(
                                loanResponse, leadId, clientDetails, productCode, parentContext)
                            .contextWrite(parentContext);
                      }

                      // Both loan + PAN succeeded → process and take rejection decision
                      assert loanResponse != null;
                      log.info(
                          "[PAN_VERIFY] PAN validation process succeeded. loanId={},"
                              + " clientId={}",
                          loanResponse.getResourceId(),
                          clientDetails.getClientId());

                      return processPanValidationResultAndTakeRejectionDecision(
                              panResult,
                              loanResponse,
                              leadId,
                              clientDetails,
                              productCode,
                              parentContext)
                          .contextWrite(parentContext);
                    }))
        .onErrorResume(
            e -> {
              if (e instanceof PanBasedLoanRejectionException) {
                log.warn(
                    "[PAN_VERIFY][LOAN_REJECTED] Loan rejected due to PAN verification. leadId={},"
                        + " productCode={}, error={}",
                    leadId,
                    productCode,
                    e.getMessage());
                return Mono.error(e); // propagate PAN rejection
              }

              log.error(
                  "[PAN_VERIFY][UNEXPECTED] Unexpected error during loan application creation."
                      + " leadId={}, productCode={}, error={}",
                  leadId,
                  productCode,
                  e.getMessage(),
                  e);
              return Mono.error(e); // propagate unexpected error
            });
  }

  private Mono<M2pLoanCreationResponseDTO> createOnlyLoanWithoutCTA(
      LoanApplication loanData, String leadId, String productCode) {
    return createLoanApplication(loanData, leadId, productCode, false);
  }

  private Mono<M2pLoanCreationResponseDTO> handleCtaIfRequired(
      M2pLoanCreationResponseDTO data, String productCode) {

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), LOAN_CREATE_CTA_IDENTIFIER);

              if (Objects.isNull(flowData)) {
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }

              Integer loanId = data.getResourceId();
              if (flowData.isCtaCallFlag()) {
                return m2PWrapperApi
                    .registerCta(loanId.toString(), flowData.getCtaName())
                    .thenReturn(data); // return the original data after CTA
              }

              return Mono.just(data);
            });
  }

  private Mono<M2pLoanCreationResponseDTO> handlePanServiceUnavailableFlow(
      M2pLoanCreationResponseDTO loanResponse,
      String leadId,
      ClientCacheDTO clientCacheDTO,
      String productCode,
      ContextView parentContext) {
    // Fire-and-forget async persist in db
    persistPanValidationResponseAsync(
            null,
            leadId,
            loanResponse,
            clientCacheDTO,
            PanVerificationLog.FinalVerificationResult.FAILURE)
        .contextWrite(parentContext)
        .subscribe();

    return handleCtaIfRequired(loanResponse, productCode);
  }

  private Mono<M2pLoanCreationResponseDTO> processPanValidationResultAndTakeRejectionDecision(
      PanVerificationResult results,
      M2pLoanCreationResponseDTO loanResponse,
      String leadId,
      ClientCacheDTO clientCacheDTO,
      String productCode,
      ContextView parentContext) {

    boolean isValid = standalonePanValidationService.isPanValid(results);

    // Always persist async in parallel
    persistPanValidationResponseAsync(
            results,
            leadId,
            loanResponse,
            clientCacheDTO,
            PanVerificationLog.FinalVerificationResult.SUCCESS)
        .contextWrite(parentContext)
        .subscribe();

    if (isValid) {
      publishEventKafkaAsync(
          () ->
              eventProducerService.publishEvent(
                  new EventContext(Event.NSDL_PHASE1_PAN_APPROVED, null, leadId), null, null));
      log.info(
          "[PAN_VERIFY] PAN validation PASSED for leadId={}, loanId={}",
          leadId,
          loanResponse.getResourceId());

      return handleCtaIfRequired(loanResponse, productCode);
    } else {
      log.warn(
          "[PAN_VERIFY] PAN Validation REJECTED for leadId={}, loanId={}",
          leadId,
          loanResponse.getResourceId());

      return panBasedLoanApplicationRejection(loanResponse.getResourceId().toString())
          .then(
              Mono.defer(
                  () -> {
                    log.info(
                        "[PAN_VERIFY] Loan REJECTED due to PAN validation rejection. leadId={},"
                            + " loanId={}",
                        leadId,
                        loanResponse.getResourceId());
                    publishEventKafkaAsync(
                        () ->
                            eventProducerService.publishEvent(
                                new EventContext(Event.NSDL_PHASE1_PAN_REJECTED, null, leadId),
                                null,
                                null));
                    String rejectionMessage =
                        results.getPanVerificationResults().stream()
                            .findFirst()
                            .map(PanVerificationResult.VerificationResult::getEvaluationResult)
                            .map(PanVerificationResult.EvaluationResult::getRejectionMessage)
                            .filter(StringUtils::isNotBlank)
                            .orElse("PAN verification rejected");

                    return Mono.error(
                        new PanBasedLoanRejectionException(
                            rejectionMessage, rejectionMessage, HttpStatus.BAD_REQUEST));
                  }));
    }
  }

  private Mono<Void> persistPanValidationResponseAsync(
      PanVerificationResult results,
      String leadId,
      M2pLoanCreationResponseDTO loanApplicationResp,
      ClientCacheDTO clientDetails,
      PanVerificationLog.FinalVerificationResult status) {
    return Mono.defer(
            () -> {
              PanVerificationLog verificationLog;

              // Handle null vendor response (service unavailable flow)
              if (results == null) {
                verificationLog =
                    PanValidationUtil.buildVerificationLogForNullResponse(
                        leadId, loanApplicationResp, clientDetails, status);
              } else {
                PanVerificationResult.VerificationResult result =
                    results.getPanVerificationResults().get(0);

                log.info(
                    "[PAN_VERIFY][STORE_PAN_VERIFICATION_RESULT] Persisting PAN validation result."
                        + " clientId={}, status={}, panStatus={}, nameMatch={}, dobMatch={}",
                    clientDetails.getClientId(),
                    status,
                    result.getVendorResponse().getPanStatus(),
                    result.getVendorResponse().getNameMatch(),
                    result.getVendorResponse().getDobMatch());

                verificationLog =
                    PanValidationUtil.buildVerificationLogForVendorResponse(
                        leadId, loanApplicationResp, clientDetails, result, status);
              }

              return standalonePanValidationService.logInDb(verificationLog, leadId);
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[PAN_VERIFY][STORE_PAN_VERIFICATION_RESULT] Failed to persist PAN validation"
                      + " result. leadId={}, error={}",
                  leadId,
                  e.getMessage());

              return Mono.empty();
            });
  }

  private Mono<M2pLoanRejectResponseDTO> panBasedLoanApplicationRejection(String loanId) {
    log.warn(
        "[PAN_VERIFY][LOAN_APPLICATION_REJECTION] Initiating loan application rejection due to PAN"
            + " REJECTION. loanId={}",
        loanId);

    LoanReject loanReject =
        LoanReject.builder()
            .reasonCode(reasonCodeFactory.getKycFailReasonCode())
            .description("PAN verification rejected")
            .build();

    return m2PWrapperApi
        .rejectLoanApplication(loanReject, loanId)
        .onErrorResume(
            e -> {
              log.error(
                  "[PAN_VERIFY][LOAN_APPLICATION_REJECTION] Loan rejection fAILED. LoanId={},"
                      + " error={}",
                  loanId,
                  e.getMessage());

              return Mono.error(e);
            })
        .doOnSuccess(
            resp ->
                log.info(
                    "[PAN_VERIFY][LOAN_APPLICATION_REJECTION] Loan rejection completed"
                        + " successfully. loanId={}, response={}",
                    loanId,
                    resp));
  }

  private Mono<M2pLoanCreationResponseDTO> handleDuplicateExternalIdError(
      Throwable error, String externalId, String losProductKey) {
    if (error instanceof ClientSideException clientEx
        && clientEx.getHttpStatusCode() == HttpStatus.FORBIDDEN) {

      Object responseBody = clientEx.getResponseBody();

      if (responseBody instanceof Map<?, ?> responseMap) {
        String errorCode = String.valueOf(responseMap.get(ERROR_CODE));
        String messageCode = String.valueOf(responseMap.get(USER_MESSAGE_CODE));

        if (M2P_ERROR_CODE.equals(errorCode)
            && DUPLICATE_EXTERNAL_ID_MESSAGE_CODE.equals(messageCode)) {
          return m2PWrapperApi
              .getLoanByExternalId(externalId)
              .flatMapMany(Flux::fromIterable)
              .next()
              .switchIfEmpty(Mono.error(error))
              .flatMap(
                  loanExternalIdResponse ->
                      m2PWrapperApi
                          .getLoanApplicationByLoanId(loanExternalIdResponse.getLoanApplicationId())
                          .flatMap(
                              loanDetails -> {
                                Map<String, Object> loanDataMap = (Map<String, Object>) loanDetails;
                                Object fetchedProductKey = loanDataMap.get("losProductKey");

                                if (fetchedProductKey != null
                                    && losProductKey.equals(String.valueOf(fetchedProductKey))) {
                                  M2pLoanCreationResponseDTO.AdditionalResponseData additionalData =
                                      new M2pLoanCreationResponseDTO.AdditionalResponseData(
                                          loanExternalIdResponse.getLoanApplicationReferenceNo());

                                  return Mono.just(
                                      new M2pLoanCreationResponseDTO(
                                          Integer.valueOf(loanExternalIdResponse.getClientId()),
                                          Integer.valueOf(
                                              loanExternalIdResponse.getLoanApplicationId()),
                                          false,
                                          additionalData));
                                }
                                return Mono.error(error);
                              }));
        }
      }
    }
    return Mono.error(error);
  }

  public Mono<M2pLoanCreationResponseDTO> createLoanApplication(
      LoanApplication loanData,
      String leadId,
      String productCode,
      boolean ctaRegistrationRequested) {
    log.info("loan application data los product key {} ", loanData.getLosProductKey());
    AtomicReference<String> rejectionReason = new AtomicReference<>();
    if ("enable".equals(environment.getProperty("risk.dedupe.check"))) {

      return funnelRuleService
          .applyRiskDedupeChecks(
              loanData,
              leadId,
              productCode,
              ctaRegistrationRequested,
              () ->
                  proceedWithLoanApplication(
                      loanData, leadId, productCode, ctaRegistrationRequested),
              rejectionReason)
          .ofType(M2pLoanCreationResponseDTO.class)
          .doOnError(
              error -> {
                String parentTraceId = MDC.get(TRACE_ID);
                Mono.fromRunnable(
                        () ->
                            kafkaFunnelLoggingService.sendLogEvent(
                                null,
                                leadId,
                                LoanFunnelDTO.Stage.LOAN_CREATION,
                                LoanFunnelDTO.SubStage.TOP_OF_FUNNEL,
                                FAIL,
                                rejectionReason.get(),
                                error.getMessage()))
                    .subscribeOn(Schedulers.parallel())
                    .contextWrite(context -> context.put(TRACE_ID, parentTraceId))
                    .subscribe();
              });
    }
    // If risk dedupe check is disabled, proceed normally
    return proceedWithLoanApplication(loanData, leadId, productCode, ctaRegistrationRequested)
        .ofType(M2pLoanCreationResponseDTO.class)
        .doOnError(
            error -> {
              String parentTraceId = MDC.get(TRACE_ID);
              Mono.fromRunnable(
                      () ->
                          kafkaFunnelLoggingService.sendLogEvent(
                              null,
                              leadId,
                              LoanFunnelDTO.Stage.LOAN_CREATION,
                              LoanFunnelDTO.SubStage.TOP_OF_FUNNEL,
                              FAIL,
                              null,
                              error.getMessage()))
                  .subscribeOn(Schedulers.parallel())
                  .contextWrite(context -> context.put(TRACE_ID, parentTraceId))
                  .subscribe();
            });
  }

  private Mono<Void> handlePostLoanCreationSideEffects(
      M2pLoanCreationResponseDTO result,
      String leadId,
      String productCode,
      LoanLevelClientDetailsCacheDTO clientDetails) {
    if (result == null) {
      return Mono.empty();
    }

    String parentTraceId =
        StringUtils.defaultIfBlank(MDC.get(TRACE_ID), UUID.randomUUID().toString().substring(0, 8));
    String loanId = result.getResourceId().toString();
    String clientId = result.getClientId().toString();

    Mono<Void> kafkaLogging =
        Mono.fromRunnable(
                () ->
                    kafkaFunnelLoggingService.sendLogEvent(
                        loanId,
                        leadId,
                        LoanFunnelDTO.Stage.LOAN_CREATION,
                        LoanFunnelDTO.SubStage.TOP_OF_FUNNEL,
                        SUCCESS,
                        null,
                        null))
            .subscribeOn(Schedulers.parallel())
            .then();

    Mono<Void> insurance =
        insuranceService
            .getInsuranceConfig(productCode)
            .doOnNext(
                config ->
                    log.info(
                        "[INSURANCE][CONFIG] FeatureFlag: {}, insuranceGrid: {}",
                        config.getInsuranceFeatureFlag(),
                        config.getInsuranceGrid()))
            .filter(InsuranceConfig::getInsuranceFeatureFlag)
            .flatMap(
                config ->
                    insuranceService.savePremiumGridToTable(
                        loanId, clientId, config.getInsuranceGrid(), productCode))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(err -> log.error("[INSURANCE][STORAGE] Error saving premium", err))
            .then();

    Mono<Void> amlPepValidation =
        amlPepValidationService
            .launchAmlPepValidationIfEnabled(productCode, loanId, clientDetails)
            .flatMap(
                amlPep -> {
                  if (amlPep == null) {
                    return Mono.empty();
                  }
                  amlPep.setLeadId(loanId);
                  return amlPepResultsRepository.save(amlPep);
                })
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(err -> Mono.empty())
            .then();

    return Mono.when(kafkaLogging, insurance, amlPepValidation)
        .contextWrite(ctx -> ctx.put(TRACE_ID, parentTraceId));
  }

  private Mono<M2pLoanCreationResponseDTO> getLoanApplicationByExternalId(String externalId) {
    return m2PWrapperApi
        .getLoanByExternalId(externalId)
        .flatMapMany(Flux::fromIterable)
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException(ERROR_NO_LOAN_FOUND + externalId)))
        .map(
            dto -> {
              M2pLoanCreationResponseDTO.AdditionalResponseData additionalData =
                  new M2pLoanCreationResponseDTO.AdditionalResponseData(
                      dto.getLoanApplicationReferenceNo());

              return new M2pLoanCreationResponseDTO(
                  Integer.valueOf(dto.getClientId()),
                  Integer.valueOf(dto.getLoanApplicationId()),
                  false,
                  additionalData);
            });
  }

  public Mono<Object> updateLoan(
      UpdateLoanApplication loanData, String loanId, String productCode) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), UPDATE_LOAN_IDENTIFIER);
              if (Objects.isNull(flowData)) {
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }
              HashMap<String, Object> conditions = flowData.getConditions();

              if (shouldSkipLoanUpdateAndOnlyAddInsuranceCharge(loanData, flowData)) {
                log.info(
                    "[{}] insurance-only update for loan: {}, skipping loan update",
                    UPDATE_LOAN_LOG_HEADER,
                    loanId);
                return addInsuranceCharge(loanData, loanId, flowData)
                    .defaultIfEmpty(Collections.emptyMap())
                    .map(resp -> (Object) resp);
              }

              if (Boolean.TRUE.equals(loanData.getPloBreOfferAcceptance())) {
                log.info(
                    "[{}] proceeding with aa based bre approval check for loan: {}",
                    UPDATE_LOAN_LOG_HEADER,
                    loanId);
                return updateLoanAndRegisterCTA(loanId, loanData)
                    .flatMap(
                        m2pResponse ->
                            addInsuranceCharge(loanData, loanId, flowData)
                                .defaultIfEmpty(Collections.emptyMap())
                                .map(
                                    insuranceResp -> {
                                      Map<String, Object> finalResponse = new HashMap<>();

                                      if (m2pResponse instanceof Map) {
                                        Map<?, ?> raw = (Map<?, ?>) m2pResponse;
                                        raw.forEach(
                                            (k, v) -> finalResponse.put(String.valueOf(k), v));
                                      }
                                      insuranceResp.forEach(
                                          (k, v) -> finalResponse.put(String.valueOf(k), v));
                                      return finalResponse;
                                    }));
              }

              // if breApprovalCheck is true then moving to checking the amount present in request
              // body
              // the value should be equal to amount present in the latest approved bre response
              // if the value doesn't match, then throw forbidden exception to the client
              if (!Objects.isNull(conditions)
                  && conditions.containsKey("breApprovalCheck")
                  && conditions.get("breApprovalCheck").equals(true)
                  && conditions.containsKey("validation")
                  && conditions.containsKey(OFFER_DOWNGRADE)
                  && conditions.get(OFFER_DOWNGRADE).equals(true)) {
                log.info(
                    "[{}] proceeding with bre approval check for loan: {}",
                    UPDATE_LOAN_LOG_HEADER,
                    loanId);
                String validation = (String) conditions.get("validation");

                boolean offerDowngrade =
                    flowData.getConditions() != null
                        && flowData.getConditions().containsKey(OFFER_DOWNGRADE)
                        && Boolean.TRUE.equals(flowData.getConditions().get(OFFER_DOWNGRADE));

                return updateLoanApplicationWithBreApprovalCheck(
                        loanData, loanId, validation, offerDowngrade)
                    .flatMap(
                        m2pResponse ->
                            addInsuranceCharge(loanData, loanId, flowData)
                                .defaultIfEmpty(Collections.emptyMap())
                                .map(
                                    insuranceResp -> {
                                      Map<String, Object> finalResponse = new HashMap<>();

                                      if (m2pResponse instanceof Map) {
                                        Map<?, ?> raw = (Map<?, ?>) m2pResponse;
                                        raw.forEach(
                                            (k, v) -> finalResponse.put(String.valueOf(k), v));
                                      }
                                      insuranceResp.forEach(
                                          (k, v) -> finalResponse.put(String.valueOf(k), v));
                                      return finalResponse;
                                    }));
              }
              // proceeding with update loan application with additional feature for undo approve
              // and update
              // no bre check implemented in this section
              log.info(
                  "[{}] proceeding without bre approval check for loan: {}",
                  UPDATE_LOAN_LOG_HEADER,
                  loanId);
              return updateLoanApplication(loanData, loanId)
                  .flatMap(
                      updateLoanResponse -> {
                        if (updateLoanResponse instanceof Throwable updateLoanError) {
                          return Mono.error(updateLoanError);
                        }
                        return addInsuranceCharge(loanData, loanId, flowData)
                            .defaultIfEmpty(Collections.emptyMap())
                            .map(
                                insuranceResp -> {
                                  Map<String, Object> finalResponse = new HashMap<>();
                                  finalResponse.put("updateLoanResponse", updateLoanResponse);
                                  finalResponse.putAll(insuranceResp);
                                  return finalResponse;
                                });
                      });
            });
  }

  private boolean shouldSkipLoanUpdateAndOnlyAddInsuranceCharge(
      UpdateLoanApplication loanData, ProductControl.Flow flowData) {
    return Objects.nonNull(loanData)
        && Objects.nonNull(loanData.getIsInsuranceOpted())
        && Objects.nonNull(flowData)
        && Objects.nonNull(flowData.getInsuranceConfig())
        && Boolean.TRUE.equals(flowData.getInsuranceConfig().getSkipUpdateAddVas());
  }

  public Mono<Map<String, Object>> addInsuranceCharge(
      UpdateLoanApplication loanData, String loanId, ProductControl.Flow flow) {

    // step 1: feature flag check
    if (!insuranceService.isInsuranceEnabled(loanData, flow)) {
      log.info("[INSURANCE] [ADD_CHARGE] [SKIP] insurance is disabled for loan: {}", loanId);
      return Mono.empty();
    }
    log.info("[INSURANCE] [ADD_CHARGE] starting insurance add-charge flow for loan: {}", loanId);

    // step 2: fetch loan amount details from loan application response
    return getLoanApplicationByLoanIdV3(loanId)
        .flatMap(
            loanApplicationResponse -> {
              final Double loanAmount;
              if (loanApplicationResponse instanceof Map<?, ?> responseMap) {
                Number loanAmountApproved = (Number) responseMap.get("loanAmountApproved");
                Number loanAmountRequested = (Number) responseMap.get("loanAmountRequested");
                loanAmount =
                    loanAmountApproved != null
                        ? Double.valueOf(loanAmountApproved.doubleValue())
                        : loanAmountRequested != null
                            ? Double.valueOf(loanAmountRequested.doubleValue())
                            : null;
              } else {
                loanAmount = null;
              }
              log.info(
                  "[INSURANCE] [ADD_CHARGE] resolved loanAmount={} for loanId: {}",
                  loanAmount,
                  loanId);

              // step 3: create charge request
              return insuranceService
                  .createInsuranceChargeRequest(loanId, flow, loanAmount)

                  // step 4: attempting to add a charge
                  .flatMap(
                      saveChargeRequest ->
                          addCharges(saveChargeRequest, loanId)
                              // save charge info success
                              .flatMap(
                                  addChargeResult ->
                                      insuranceService
                                          .saveChargeInfo(
                                              loanId,
                                              loanData.getIsInsuranceOpted(),
                                              true,
                                              saveChargeRequest.getAmount(),
                                              loanAmount)
                                          .thenReturn(
                                              Map.of(IS_INSURANCE_CHARGE_ADDED, (Object) true)))

                              // save charge info failure
                              .onErrorResume(
                                  err ->
                                      insuranceService
                                          .saveChargeInfo(
                                              loanId,
                                              loanData.getIsInsuranceOpted(),
                                              false,
                                              saveChargeRequest.getAmount(),
                                              loanAmount)
                                          .doOnError(
                                              saveErr ->
                                                  log.error(
                                                      "[INSURANCE] [ADD_CHARGE] [SAVE] error saving"
                                                          + " charge info for loan: {}, error: {}",
                                                      loanId,
                                                      saveErr.getMessage()))
                                          .then(Mono.error(err)))

                              // step 5: handle client-side errors
                              .onErrorResume(
                                  ClientSideException.class,
                                  ex -> {
                                    M2pErrorResponseDTO errorBody =
                                        getM2pErrorResponse(ex.getResponseBody());
                                    int httpStatusCode = ex.getHttpStatusCode().value();

                                    // duplicate charge handling
                                    if (isChargeAlreadyExistsError(errorBody, httpStatusCode)) {
                                      log.warn(
                                          "[INSURANCE] [ADD_CHARGE] duplicate charge exists for"
                                              + " loan: {}, marking success.",
                                          loanId);

                                      return insuranceService
                                          .saveChargeInfo(
                                              loanId,
                                              loanData.getIsInsuranceOpted(),
                                              true,
                                              saveChargeRequest.getAmount(),
                                              loanAmount)
                                          .onErrorResume(
                                              err3 -> {
                                                log.error(
                                                    "[INSURANCE] [ADD_CHARGE] save duplicate charge"
                                                        + " info failed for loan: {}, error: {}",
                                                    loanId,
                                                    err3.getMessage());
                                                return Mono.empty();
                                              })
                                          .thenReturn(
                                              Map.of(IS_INSURANCE_CHARGE_ADDED, (Object) true));
                                    }

                                    // existing 403 handling
                                    boolean loanNotModifiable =
                                        isLoanNotModifiableError(errorBody, httpStatusCode);

                                    if (loanNotModifiable) {
                                      log.warn(
                                          "[INSURANCE] [ADD_CHARGE] loan: {} cannot be modified."
                                              + " undo + retry flow.",
                                          loanId);

                                      LocalDate today = LocalDate.now(ZoneId.of(ASIA_KOLKATA));
                                      String date =
                                          today.format(DateTimeFormatter.ofPattern(DATE_FORMAT));

                                      // undo_approve → add → approve
                                      return undoApproveLoan(loanId)
                                          .flatMap(
                                              undoResp ->
                                                  addCharges(saveChargeRequest, loanId)

                                                      // save charge info in retry flow (success)
                                                      .flatMap(
                                                          addChargeResult ->
                                                              insuranceService
                                                                  .saveChargeInfo(
                                                                      loanId,
                                                                      loanData
                                                                          .getIsInsuranceOpted(),
                                                                      true,
                                                                      saveChargeRequest.getAmount(),
                                                                      loanAmount)
                                                                  .thenReturn(addChargeResult))

                                                      // save charge info in retry flow (failure)
                                                      .onErrorResume(
                                                          err2 ->
                                                              insuranceService
                                                                  .saveChargeInfo(
                                                                      loanId,
                                                                      loanData
                                                                          .getIsInsuranceOpted(),
                                                                      false,
                                                                      saveChargeRequest.getAmount(),
                                                                      loanAmount)
                                                                  .doOnError(
                                                                      saveErr ->
                                                                          log.error(
                                                                              "[INSURANCE]"
                                                                                  + " [ADD_CHARGE]"
                                                                                  + " error saving"
                                                                                  + " charge info"
                                                                                  + " after retry"
                                                                                  + " for loan: {},"
                                                                                  + " error: {}",
                                                                              loanId,
                                                                              saveErr.getMessage()))
                                                                  .then(Mono.empty())))
                                          .flatMap(
                                              addResp ->
                                                  approveLoan(loanId, date)
                                                      .onErrorResume(
                                                          err3 -> {
                                                            log.error(
                                                                "[INSURANCE] [ADD_CHARGE] retry"
                                                                    + " approve failed for loan:"
                                                                    + " {}",
                                                                loanId);
                                                            return Mono.empty();
                                                          }))
                                          .thenReturn(Map.of(IS_INSURANCE_CHARGE_ADDED, true));
                                    }

                                    // other client-side errors
                                    log.error(
                                        "[INSURANCE] [ADD_CHARGE] add charge failed for loan: {},"
                                            + " error {}",
                                        loanId,
                                        ex.getMessage());
                                    return Mono.just(Map.of(IS_INSURANCE_CHARGE_ADDED, false));
                                  }))
                  // step 6: unexpected system error
                  .onErrorResume(
                      err -> {
                        log.error(
                            "[INSURANCE] [ADD_CHARGE] unexpected error during insurance flow for"
                                + " loan: {}, error: {}",
                            loanId,
                            err.getMessage());
                        return Mono.just(Map.of(IS_INSURANCE_CHARGE_ADDED, false));
                      });
            });
  }

  private boolean isChargeAlreadyExistsError(M2pErrorResponseDTO error, int httpStatusCode) {
    if (error == null) return false;

    // must be http 400 for m2p duplicate-charge errors
    if (httpStatusCode != 400) return false;

    // we check only for message containing this exact phrase
    final String KEY_PHRASE = "this charge already exists for id";

    // check root-level defaultusermessage
    if (error.getDefaultUserMessage() != null
        && error.getDefaultUserMessage().toLowerCase().contains(KEY_PHRASE)) {
      return true;
    }

    // check inside "errors" list
    if (error.getErrors() != null) {
      return error.getErrors().stream()
          .anyMatch(
              err ->
                  err.getDefaultUserMessage() != null
                      && err.getDefaultUserMessage().toLowerCase().contains(KEY_PHRASE));
    }

    return false;
  }

  public Mono<Object> updateLoanApplicationWithBreApprovalCheck(
      UpdateLoanApplication loanData, String loanId, String validation, boolean offerDowngrade) {
    Map<String, Object> noBreFound = getErrorResponseBody(NO_BRE_RESPONSE, NO_BRE_RESPONSE);
    return breStatusService
        .findByExternalIdAndSuccessStatusAndApprovedStateOrFiState(loanId, offerDowngrade)
        .switchIfEmpty(
            Mono.error(new ForbiddenException(NO_BRE_RESPONSE, noBreFound, HttpStatus.FORBIDDEN)))
        .flatMap(
            breStatus ->
                validateAndProcessLoan(breStatus, loanData, loanId, validation, offerDowngrade))
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error occurred while update loan for loan: {}",
                  UPDATE_LOAN_LOG_HEADER,
                  loanId);
              return Mono.error(error);
            });
  }

  private Mono<Object> validateAndProcessLoan(
      BreStatus breStatus,
      UpdateLoanApplication loanData,
      String loanId,
      String validation,
      boolean offerDowngrade) {
    Json requestJson = breStatus.getRequest();
    if (Objects.isNull(requestJson)) {
      return Mono.error(
          new ForbiddenException(
              SOMETHING_WENT_WRONG, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    HashMap<String, Object> requestObject =
        gson.fromJson(
            cleanJsonString(requestJson.toString()),
            new TypeToken<HashMap<String, Object>>() {}.getType());

    String amountFromRequest = loanData.getLoanAmountRequested();
    if (Objects.isNull(amountFromRequest)) {
      log.info(
          "[{}] Amount in body is null, proceeding with update loan for loanId: {}",
          UPDATE_LOAN_LOG_HEADER,
          loanId);
      return m2PWrapperApi.updateLoanApplication(loanId, loanData, Object.class);
    }

    String amountFromBreRequest = requestObject.get(AMOUNT).toString();
    if ("-".equals(amountFromBreRequest)) {
      return Mono.error(
          new ForbiddenException(NO_AMOUNT_IN_BRE, NO_AMOUNT_IN_BRE, HttpStatus.FORBIDDEN));
    }

    double breAmount = Double.parseDouble(amountFromBreRequest);
    double requestedAmount = Double.parseDouble(amountFromRequest);

    if (!isValidationSuccessful(breAmount, requestedAmount, validation)) {
      Map<String, Object> amountMismatch =
          getErrorResponseBody(AMOUNT_MISMATCH_BRE, AMOUNT_MISMATCH_BRE);
      Map<String, Object> amountExceed =
          getErrorResponseBody(AMOUNT_MISMATCH_BRE, AMOUNT_MISMATCH_BRE);

      return Mono.error(
          new ForbiddenException(
              "==".equalsIgnoreCase(validation) ? AMOUNT_MISMATCH_BRE : AMOUNT_EXCEEDS_APPROVED,
              "==".equalsIgnoreCase(validation) ? amountMismatch : amountExceed,
              HttpStatus.FORBIDDEN));
    }

    log.info(
        "[{}] validation successful, proceeding with update for loanId: {}",
        UPDATE_LOAN_LOG_HEADER,
        loanId);

    return m2PWrapperApi
        .updateLoanApplication(loanId, loanData, Object.class)
        .flatMap(
            updateLoanResponse -> {
              if (offerDowngrade) {
                return m2PWrapperApi
                    .registerCta(loanId, OFFER_DOWN_CTA_IDENTIFIER)
                    .thenReturn(updateLoanResponse);
              } else {
                return Mono.just(updateLoanResponse);
              }
            });
  }

  private Mono<Object> updateLoanAndRegisterCTA(String loanId, UpdateLoanApplication loanData) {

    // Fetch BRE status to check if loan should be rejected
    return breStatusService
        .findByExternalIdAndSuccessStatus(loanId)
        .flatMap(
            breStatus -> {
              String scienapticStatus = breStatus.getScienapticStatus();
              // Check if BRE status is not APPROVED or ELIGIBLE, then reject the loan
              if (!APPROVED.equalsIgnoreCase(scienapticStatus)
                  && !ELIGIBLE.equalsIgnoreCase(scienapticStatus)) {
                LoanReject loanReject =
                    LoanReject.builder()
                        .reasonCode(reasonCodeFactory.getBreFailReasonCode())
                        .description("BRE Rejected")
                        .build();
                return m2PWrapperApi.rejectLoanApplication(loanReject, loanId);
              }

              // If approved or eligible, proceed with update and CTA registration
              return m2PWrapperApi
                  .updateLoanApplication(loanId, loanData, Object.class)
                  .flatMap(
                      updateLoanResponse ->
                          m2PWrapperApi
                              .registerCta(loanId, SCIENAPTIC_CTA_IDENTIFIER)
                              .thenReturn(updateLoanResponse));
            });
  }

  private boolean isValidationSuccessful(
      double breAmount, double requestedAmount, String validation) {
    if ("==".equalsIgnoreCase(validation)) {
      return breAmount == requestedAmount;
    } else if ("<=".equalsIgnoreCase(validation)) {
      return requestedAmount <= breAmount;
    } else {
      throw new ForbiddenException(
          INVALID_VALIDATION_PARAMETER, INVALID_VALIDATION_PARAMETER, HttpStatus.BAD_REQUEST);
    }
  }

  private Map<String, Object> getErrorResponseBody(
      String developerMessage, String defaultUserMessage) {
    List<Map<String, String>> errorsList = new ArrayList<>();
    Map<String, String> errorsMap = new HashMap<>();
    errorsMap.put("developerMessage", developerMessage);
    errorsMap.put("defaultUserMessage", defaultUserMessage);
    errorsList.add(errorsMap);

    Map<String, Object> result = new HashMap<>();
    result.put("errors", errorsList);

    return result;
  }

  private String cleanJsonString(String jsonString) {
    String cleanedJsonString = jsonString;
    if (cleanedJsonString.startsWith("JsonByteArrayInput{")) {
      cleanedJsonString = cleanedJsonString.substring("JsonByteArrayInput{".length());
    }
    if (cleanedJsonString.endsWith("}}")) {
      cleanedJsonString = cleanedJsonString.substring(0, cleanedJsonString.length() - 1);
    }
    return cleanedJsonString.trim();
  }

  public Mono<Object> updateLoanApplication(UpdateLoanApplication loanData, String loanId) {
    return m2PWrapperApi
        .updateLoanApplication(loanId, loanData, Object.class)
        .flatMap(Mono::just)
        .onErrorResume(
            ClientSideException.class,
            clientSideException -> {
              M2pErrorResponseDTO errorBody =
                  getM2pErrorResponse(clientSideException.getResponseBody());
              if (checkForApprovedLoanUpdateError(
                  errorBody, clientSideException.getHttpStatusCode())) {
                log.info(
                    "[{}] undoing approved loan to pending state: {}",
                    UPDATE_LOAN_LOG_HEADER,
                    loanId);
                return undoApproveLoan(loanId)
                    .flatMap(
                        undoApproveLoanResponse -> {
                          LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
                          DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
                          String formattedDate = currentDate.format(formatter);
                          log.info(
                              "[{}] undoing approved loan completed, moving to update loan: {}",
                              UPDATE_LOAN_LOG_HEADER,
                              loanId);
                          // Proceeding with loan update after undoing approved status
                          loanData.setExpectedDisbursementDate(formattedDate);
                          loanData.setDateFormat(DATE_FORMAT);
                          return m2PWrapperApi
                              .updateLoanApplication(loanId, loanData, Object.class)
                              .flatMap(
                                  updateLoanResponse -> {
                                    log.info(
                                        "[{}] update loan completed, moving to approve the loan:"
                                            + " {}",
                                        UPDATE_LOAN_LOG_HEADER,
                                        loanId);
                                    return approveLoan(loanId, formattedDate)
                                        .flatMap(
                                            approveLoanResult -> {
                                              log.info(
                                                  "[{}] approve loan success: {}",
                                                  UPDATE_LOAN_LOG_HEADER,
                                                  loanId);
                                              return Mono.just(updateLoanResponse);
                                            })
                                        .onErrorResume(
                                            approveLoanError -> {
                                              log.error(
                                                  "[{}] error while approving loan: {}",
                                                  UPDATE_LOAN_LOG_HEADER,
                                                  loanId);
                                              return Mono.just(approveLoanError);
                                            });
                                  })
                              .onErrorResume(
                                  updateLoanError ->
                                      approveLoan(loanId, formattedDate)
                                          .flatMap(
                                              approveLoanResultAfterUpdateFail ->
                                                  Mono.error(updateLoanError))
                                          .onErrorResume(
                                              approveLoanErrorAfterUpdateFail ->
                                                  Mono.error(updateLoanError)));
                        });
              }
              return Mono.error(clientSideException);
            });
  }

  public M2pErrorResponseDTO getM2pErrorResponse(Object responseBody) {
    return gson.fromJson(gson.toJson(responseBody), M2pErrorResponseDTO.class);
  }

  private boolean checkForApprovedLoanUpdateError(
      M2pErrorResponseDTO errorResponse, HttpStatusCode httpStatusCode) {
    if (Objects.isNull(errorResponse)
        || errorResponse.getErrors().isEmpty()
        || httpStatusCode != HttpStatus.FORBIDDEN) {
      return false;
    }
    M2pErrorResponseDTO.ErrorDetailDTO errorDetails = errorResponse.getErrors().get(0);
    return Objects.equals(errorDetails.getDeveloperMessage(), "Loan Application cant be modified");
  }

  private boolean isLoanNotModifiableError(M2pErrorResponseDTO error, int httpStatusCode) {
    if (error == null || error.getErrors() == null || error.getErrors().isEmpty()) return false;

    // Must be 403 Forbidden
    if (httpStatusCode != 403) return false;

    return error.getErrors().stream()
        .anyMatch(
            err ->
                "error.msg.loan.cannot.modify.loan.in.its.present.state"
                    .equals(err.getUserMessageGlobalisationCode()));
  }

  public Mono<?> getLoanApplications(String leadId) {
    return m2PWrapperApi.getLoanApplications(leadId);
  }

  public Mono<Object> getLoanApplicationByLoanId(String loanId) {
    return m2PWrapperApi.getLoanApplicationByLoanId(loanId);
  }

  public Mono<Object> getLoanApplicationByLoanIdV3(String loanId) {
    log.info("[GET_LOAN_BY_ID] Starting fetch for loanId={}", loanId);

    return m2PWrapperApi
        .getLoanApplicationByLoanId(loanId)
        .flatMap(
            m2pResponse -> {
              log.info("[M2P_SUCCESS] Loan details fetched successfully for loanId={}", loanId);
              return enrichLoanResponseWithInsuranceAndBusinessLoan(m2pResponse, loanId);
            })
        .onErrorResume(
            err -> {
              log.error(
                  "[M2P_FAILURE] Failed to fetch loan details for loanId={}, returning original"
                      + " error. Error={}",
                  loanId,
                  err.getMessage());
              return Mono.error(err);
            });
  }

  public Mono<BusinessLoanUpdateResponse> updateBusinessLoanDetails(
      String loanId, BusinessLoanDetailsDTO businessLoanDetails) {
    if (businessLoanDetails == null) {
      return Mono.just(
          BusinessLoanUpdateResponse.builder()
              .status(ResponseStatus.FAIL)
              .message("businessLoanDetails is required")
              .build());
    }
    // Persist business_loan_details (+ documents), then async analytics + evaluation rows
    // (non-blocking).
    return businessLoanEvaluationService
        .getTrillionTerminalTupleForLoanUpdate(loanId)
        .onErrorResume(
            error -> {
              log.warn(
                  "[UPDATE_BUSINESS_LOAN] Classification read failed for loanId={}, proceeding with"
                      + " update. error={}",
                  loanId,
                  error.getMessage());
              return Mono.just(false);
            })
        .flatMap(
            earlyExit -> {
              if (Boolean.TRUE.equals(earlyExit)) {
                log.info(
                    "[UPDATE_BUSINESS_LOAN] loanId={} evaluation already complete"
                        + " (terminal trillion_status); skipping persistence",
                    loanId);
                return Mono.just(
                    BusinessLoanUpdateResponse.builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Evaluation is already completed for this loan.")
                        .build());
              }
              return businessLoanEvaluationService
                  .saveBusinessLoanDetailsAsync(loanId, businessLoanDetails)
                  .doOnSuccess(
                      unused ->
                          businessLoanEvaluationService.afterBusinessLoanDetailsUpdateAsync(
                              loanId, businessLoanDetails))
                  .thenReturn(
                      BusinessLoanUpdateResponse.builder()
                          .status(ResponseStatus.SUCCESS)
                          .message("Business loan details updated successfully")
                          .build())
                  .onErrorResume(
                      error -> {
                        log.error(
                            "[UPDATE_BUSINESS_LOAN] Failed to update business loan details for"
                                + " loanId: {}, error: {}",
                            loanId,
                            error.getMessage());
                        return Mono.just(
                            BusinessLoanUpdateResponse.builder()
                                .status(ResponseStatus.SERVER_ERROR)
                                .message(
                                    "Failed to update business loan details: " + error.getMessage())
                                .build());
                      });
            });
  }

  /**
   * Validates {@code isBusinessLoanUpdate}, ensures {@code BUSINESS_LOAN_CONFIG} has {@code
   * isBusinessLoan=true} in ProductControl for the loan's product, then maps {@link
   * BusinessLoanUpdateResponse} to HTTP status (200 / 400 / 500) for the business-loan update API.
   *
   * <p>Validation failures before the core update (bad flag, product not business-loan, missing
   * product config) are signaled via {@link BaseException} for {@code GlobalExceptionHandler}.
   */
  public Mono<ResponseEntity<BusinessLoanUpdateResponse>> updateBusinessLoanDetailsAsResponse(
      String loanId, BusinessLoanDetailsDTO businessLoanDetails, Boolean isBusinessLoanUpdate) {
    if (!Boolean.TRUE.equals(isBusinessLoanUpdate)) {
      return Mono.error(
          new BaseException("isBusinessLoanUpdate must be true", null, HttpStatus.BAD_REQUEST));
    }
    return getClientIdAndProductCode(loanId)
        .flatMap(
            tuple -> {
              String productCode = tuple.getT2();
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlData -> {
                        ProductControl.Flow businessLoanConfig =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControlData.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
                        boolean isBusinessLoanProduct =
                            businessLoanConfig != null
                                && Boolean.TRUE.equals(businessLoanConfig.getIsBusinessLoan());
                        if (!isBusinessLoanProduct) {
                          log.info(
                              "[UPDATE_BUSINESS_LOAN] Rejecting update: product {} has no"
                                  + " BUSINESS_LOAN_CONFIG with isBusinessLoan=true for loanId={}",
                              productCode,
                              loanId);
                          return Mono.error(
                              new BaseException(
                                  "Business loan is not enabled for this product",
                                  null,
                                  HttpStatus.BAD_REQUEST));
                        }
                        return updateBusinessLoanDetails(loanId, businessLoanDetails)
                            .map(LoanApplicationService::mapBusinessLoanUpdateToResponseEntity);
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () ->
                              Mono.error(
                                  new BaseException(
                                      "Product configuration not found for product: " + productCode,
                                      null,
                                      HttpStatus.INTERNAL_SERVER_ERROR))));
            });
  }

  private static ResponseEntity<BusinessLoanUpdateResponse> mapBusinessLoanUpdateToResponseEntity(
      BusinessLoanUpdateResponse response) {
    ResponseStatus status = response.getStatus();
    if (status == ResponseStatus.SUCCESS) {
      return ResponseEntity.ok(response);
    }
    if (status == ResponseStatus.FAIL) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * After a successful M2P loan fetch: merge insurance from DB when present; attach {@code
   * kycDetails.panAadhaarLinkedStatus} from {@code pan_aadhaar_linkage_details}; when product has
   * {@code BUSINESS_LOAN_CONFIG} with {@code isBusinessLoan=true}, attach {@code
   * businessLoanDetails.status} from {@link
   * BusinessLoanEvaluationService#getOrCreateLoanTypeStatus}.
   */
  private Mono<Object> enrichLoanResponseWithInsuranceAndBusinessLoan(
      Object m2pResponse, String loanId) {
    Mono<Object> withInsurance =
        loanInsuranceDetailsRepository
            .findFirstByLoanApplicationIdOrderByIdDesc(Integer.parseInt(loanId))
            .flatMap(
                insurance -> {
                  log.info("[INSURANCE_LOOKUP] Insurance record found for loanId={}", loanId);
                  return Mono.just(mergeM2pAndInsurance(m2pResponse, insurance, loanId));
                })
            .switchIfEmpty(
                Mono.fromSupplier(
                    () -> {
                      log.info(
                          "[INSURANCE_LOOKUP] No insurance record found for loanId={}, skipping"
                              + " merge",
                          loanId);
                      return m2pResponse;
                    }))
            .onErrorResume(
                err -> {
                  log.warn(
                      "[INSURANCE_ERROR] DB lookup failed for loanId={}, skipping insurance merge."
                          + " Error={}",
                      loanId,
                      err.getMessage());
                  return Mono.just(m2pResponse);
                })
            .flatMap(merged -> attachKycPanAadhaarLinkage(merged, loanId));

    return withInsurance.flatMap(
        responseWithInsurance ->
            resolveProductCodeFromResponse(responseWithInsurance)
                .flatMap(
                    productCode ->
                        productConfigMasterService
                            .getProductConfigMasterData(productCode)
                            .flatMap(
                                tuple -> {
                                  ProductControl.Flow businessLoanFlow =
                                      productConfigMasterService.getFlowFromProductConfig(
                                          tuple.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
                                  if (businessLoanFlow == null
                                      || !Boolean.TRUE.equals(
                                          businessLoanFlow.getIsBusinessLoan())) {
                                    log.info(
                                        "[BUSINESS_LOAN_SKIP] Product {} has no business loan"
                                            + " config for loanId={}, skipping merge",
                                        productCode,
                                        loanId);
                                    return Mono.just(responseWithInsurance);
                                  }
                                  return businessLoanEvaluationService
                                      .getOrCreateLoanTypeStatus(loanId, productCode)
                                      .map(
                                          status ->
                                              mergeBusinessLoanDetails(
                                                  responseWithInsurance, status, loanId))
                                      .onErrorResume(
                                          err -> {
                                            log.warn(
                                                "[BUSINESS_LOAN_ERROR] Failed to get business loan"
                                                    + " status for loanId={}, skipping merge."
                                                    + " Error={}",
                                                loanId,
                                                err.getMessage());
                                            return Mono.just(responseWithInsurance);
                                          });
                                }))
                .switchIfEmpty(Mono.just(responseWithInsurance))
                .onErrorResume(
                    err -> {
                      log.warn(
                          "[BUSINESS_LOAN_CONFIG_ERROR] Failed to resolve product/config for"
                              + " loanId={}, skipping business loan merge. Error={}",
                          loanId,
                          err.getMessage());
                      return Mono.just(responseWithInsurance);
                    }));
  }

  /**
   * Resolves product code from M2P loan map ({@code losProductKey}), when response is a {@link
   * Map}.
   */
  private Mono<String> resolveProductCodeFromResponse(Object response) {
    if (!(response instanceof Map)) {
      return Mono.empty();
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) response;
    Object key = map.get("losProductKey");
    if (key == null) {
      return Mono.empty();
    }
    String productCode = String.valueOf(key).trim();
    if (productCode.isEmpty() || "null".equalsIgnoreCase(productCode)) {
      return Mono.empty();
    }
    return Mono.just(productCode);
  }

  /** Adds {@code businessLoanDetails} with {@code status} (= trillion classification status). */
  private Object mergeBusinessLoanDetails(Object response, String status, String loanId) {
    if (!(response instanceof Map)) {
      log.warn(
          "[BUSINESS_LOAN_MERGE_SKIPPED] M2P response is not a Map, cannot attach"
              + " businessLoanDetails for loanId={}",
          loanId);
      return response;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) response;
    Map<String, Object> businessLoanDetails = new HashMap<>();
    businessLoanDetails.put("status", status);
    map.put("businessLoanDetails", businessLoanDetails);
    log.info(
        "[BUSINESS_LOAN_MERGE_SUCCESS] Attached businessLoanDetails.status={} for loanId={}",
        status,
        loanId);
    return map;
  }

  // Helper to merge insurance info into M2P response
  private Object mergeM2pAndInsurance(
      Object m2pResponse, LoanInsuranceDetailsEntity insurance, String loanId) {

    if (m2pResponse instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) m2pResponse;

      Map<String, Object> insuranceDetails = new HashMap<>();
      if (insurance.getPremiumAmount() != null) {
        insuranceDetails.put("premiumAmount", insurance.getPremiumAmount());
      }
      if (insurance.getSumInsured() != null) {
        insuranceDetails.put("sumInsured", insurance.getSumInsured());
      }
      if (insurance.getPolicyNumber() != null) {
        insuranceDetails.put("policyNumber", insurance.getPolicyNumber());
      }
      if (insurance.getM2pDocId() != null) {
        insuranceDetails.put("documentId", insurance.getM2pDocId());
      }
      if (insurance.getIsOpted() != null) {
        insuranceDetails.put("insuranceOpted", insurance.getIsOpted());
      }
      if (insurance.getIsChargeAdded() != null) {
        insuranceDetails.put("chargeAdded", insurance.getIsChargeAdded());
      }
      if (insurance.getStatus() != null) {
        insuranceDetails.put("insuranceStatus", insurance.getStatus());
      }
      if (insurance.getDocUrl() != null) {
        insuranceDetails.put("insuranceDocURL", insurance.getDocUrl());
      }
      if (insurance.getPremiumGrid() != null) {
        insuranceDetails.put(
            "premiumGrid", getPremiumGridResponseValue(insurance.getPremiumGrid(), loanId));
      }

      map.put("insuranceDetails", insuranceDetails);

      log.info("[MERGE_SUCCESS] Insurance details merged for loanId={}", loanId);
      return map;
    }

    log.warn(
        "[MERGE_SKIPPED] M2P response is not a Map, cannot attach insurance for loanId={}", loanId);
    return m2pResponse;
  }

  private Object getPremiumGridResponseValue(Json premiumGrid, String loanId) {
    try {
      JsonNode parsedGrid = objectMapper.readTree(premiumGrid.asString());
      if (parsedGrid.isArray()) {
        ArrayNode sanitizedGrid = objectMapper.createArrayNode();
        for (JsonNode row : parsedGrid) {
          if (row instanceof ObjectNode rowObject) {
            rowObject.remove("assureKitPlanName");
            rowObject.remove("assurekitPlanName");
          }
          sanitizedGrid.add(row);
        }
        return sanitizedGrid;
      }
      if (parsedGrid instanceof ObjectNode rowObject) {
        rowObject.remove("assureKitPlanName");
        rowObject.remove("assurekitPlanName");
      }
      return parsedGrid;
    } catch (Exception ex) {
      log.warn(
          "[PREMIUM_GRID_PARSE_ERROR] Failed to parse premiumGrid for loanId={}, returning string"
              + " value. Error={}",
          loanId,
          ex.getMessage());
      return premiumGrid.asString();
    }
  }

  public Mono<GetLoanV2ResponseDTO> getLoanApplicationByLoanIdV2(String loanId) {
    return m2PWrapperApi.getLoanApplicationByLoanIdV2(loanId, null);
  }

  public Flux<?> getNachMandateRequest(String loanId) {
    return m2PWrapperApi.getNachMandateRequest(loanId);
  }

  public Mono<?> uploadAgreementDocumentAgainstLoan(
      AgreementDocumentUploadRequest agreementDocumentUploadRequest, String loanId) {
    return m2PWrapperApi.uploadAgreementDocumentAgainstLoan(agreementDocumentUploadRequest, loanId);
  }

  public Mono<M2pAadhaarXmlResponseDTO> uploadAadhaarXml(
      AadhaarXmlRequest aadhaarXmlRequest, String leadId, String productCode) {

    return aadhaarXmlService
        .uploadAadhaarXml(aadhaarXmlRequest, leadId, AadhaarXMLType.DIGI_LOCKER, null, productCode)
        .doOnSuccess(
            response -> {
              validationFunnelService.runDOBWaterfallValidationFunnelAtAadharXMLUpload(
                  aadhaarXmlRequest.getRequestString(), leadId, productCode, null);
            });
  }

  public Mono<?> uploadKycDocumentAgainstLeadId(
      KycUploadDocumentRequest kycUploadDocumentRequestBody, String leadId, String kycId) {
    return m2PWrapperApi.uploadKycDocumentAgainstLeadId(
        kycUploadDocumentRequestBody, leadId, kycId);
  }

  public Mono<?> uploadNachMandateRequest(
      String loanId, NachMandateRequest nachMandateRequest, String productCode) {
    try {
      DateValidationUtil.validateDateOrder(
          nachMandateRequest.getPeriodStartDate(),
          "periodStartDate",
          nachMandateRequest.getPeriodEndDate(),
          "periodEndDate");
    } catch (IllegalArgumentException ex) {
      return Mono.error(ex);
    }
    return m2PWrapperApi
        .uploadNachMandateRequest(loanId, LoanDataUtil.getM2pNachMandate(nachMandateRequest))
        .flatMap(
            data -> {
              Long resourceId = extractNachMandateResourceId(data);
              if (StringUtils.isBlank(productCode)) {
                log.info(
                    "[{}] productCode is null or empty, skipping CTA check for loanId: {}",
                    UPLOAD_NACH_LOAN,
                    loanId);
                return Mono.just(data);
              }
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlConfigData -> {
                        ProductControl.Flow flowData =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControlConfigData.getT2(), UPLOAD_NACH_IDENTIFIER);
                        if (Objects.isNull(flowData)) {
                          log.info(
                              "[{}] NACH upload CTA config not found for product: {}, loanId: {}",
                              UPLOAD_NACH_LOAN,
                              productCode,
                              loanId);
                          return Mono.just(data);
                        }
                        if (flowData.getConditions() != null
                            && flowData.getConditions().containsKey("pennyDrop")
                            && Boolean.TRUE.equals(flowData.getConditions().get("pennyDrop"))) {
                          log.info(
                              "[UPLOAD_NACH_MANDATE] pennyDrop flag is enabled for loanId: {}",
                              loanId);
                          boolean verificationFlag =
                              (boolean)
                                  flowData.getConditions().getOrDefault("verificationFlag", true);
                          String traceId = MDC.get(TRACE_ID);
                          return processPennyDrop(
                                  loanId, nachMandateRequest, productCode, verificationFlag)
                              .contextWrite(ctx -> ctx.put(TRACE_ID, traceId))
                              .flatMap(
                                  outcome ->
                                      outcome.loanRejected() && resourceId != null
                                          ? Mono.just(
                                              Map.<String, Object>of(
                                                  "resourceId",
                                                  resourceId,
                                                  "isBankVerified",
                                                  "No",
                                                  "errorMessage",
                                                  outcome.errorMessage()))
                                          : Mono.just(data));
                        }
                        if (flowData.isCtaCallFlag()) {
                          log.info(
                              "[{}] triggering post NACH upload CTA: {}", UPLOAD_NACH_LOAN, loanId);
                          Mono<Object> ctaMono =
                              m2PWrapperApi.registerCta(loanId, flowData.getCtaName());

                          // Check if OMR (OPS Manual Review) CTA should be called after NMA CTA
                          boolean omrCtaEnabled =
                              flowData.getConditions() != null
                                  && flowData.getConditions().containsKey("omrCtaEnabled")
                                  && Boolean.TRUE.equals(
                                      flowData.getConditions().get("omrCtaEnabled"));

                          if (omrCtaEnabled) {
                            log.info(
                                "[{}] triggering OMR CTA after NACH upload for loanId: {}",
                                UPLOAD_NACH_LOAN,
                                loanId);
                            return ctaMono.then(
                                m2PWrapperApi.registerCta(loanId, "omr-status").thenReturn(data));
                          }
                          return ctaMono.thenReturn(data);
                        }
                        log.info(
                            "[{}] post NACH upload CTA cancelled: {}", UPLOAD_NACH_LOAN, loanId);
                        return Mono.just(data);
                      });
            });
  }

  private static Long extractNachMandateResourceId(Object uploadResponse) {
    if (uploadResponse instanceof M2pResourceIdTypeResponseDTO dto) {
      return dto.resourceId();
    }
    return null;
  }

  private record PennyDropOutcome(boolean loanRejected, String errorMessage) {
    private static PennyDropOutcome notRejected() {
      return new PennyDropOutcome(false, "");
    }
  }

  private Mono<PennyDropOutcome> processPennyDrop(
      String loanId,
      NachMandateRequest nachMandateRequest,
      String productCode,
      boolean verificationFlag) {
    return getClientIdForPennyDrop(loanId)
        .flatMap(
            clientId ->
                executePennyDrop(
                    loanId, clientId, nachMandateRequest, productCode, verificationFlag))
        .switchIfEmpty(Mono.just(PennyDropOutcome.notRejected()))
        .onErrorResume(e -> Mono.just(PennyDropOutcome.notRejected()));
  }

  public Mono<String> getClientIdForPennyDrop(String loanId) {
    return loanClientPartnerMapRepository
        .findByLoanApplicationId(Integer.valueOf(loanId))
        .map(
            entity -> {
              String clientId = String.valueOf(entity.getClientId());
              log.info(
                  "[UPLOAD_NACH_MANDATE] ClientId: {} fetched from loanClientPartnerMap for"
                      + " loanId: {}",
                  clientId,
                  loanId);
              return clientId;
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[UPLOAD_NACH_MANDATE] No data found in loanClientPartnerMap for loanId: {},"
                          + " fetching from getLoanApplicationByLoanId",
                      loanId);
                  return m2PWrapperApi
                      .getLoanApplicationByLoanId(loanId)
                      .map(
                          response -> {
                            String clientId = extractFieldValue(response, "clientId");
                            log.info(
                                "[UPLOAD_NACH_MANDATE] ClientId: {} fetched from"
                                    + " getLoanApplicationByLoanId for loanId: {}",
                                clientId,
                                loanId);
                            return clientId;
                          });
                }));
  }

  private Mono<PennyDropOutcome> executePennyDrop(
      String loanId,
      String clientId,
      NachMandateRequest nachMandateRequest,
      String productCode,
      boolean verificationFlag) {
    log.info(
        "[UPLOAD_NACH_MANDATE] Executing penny drop for loanId: {}, clientId: {}",
        loanId,
        clientId);
    return getBankDetailsFromLoanLevelDataTable(loanId)
        .doOnError(
            error ->
                log.error(
                    "[UPLOAD_NACH_MANDATE] Failed to fetch bank details from loan level data table"
                        + " for loanId: {}",
                    loanId,
                    error))
        .onErrorResume(error -> Mono.empty())
        .flatMap(
            bankDetails -> {
              String dbBankAccountNumber = bankDetails.get("bank_account_number");
              String requestBankAccountNumber = nachMandateRequest.getBankAccountNumber();

              boolean isBankAccountMismatch =
                  !Objects.equals(dbBankAccountNumber, requestBankAccountNumber);

              if (isBankAccountMismatch) {
                return bankVerificationService
                    .addClientBankAccount(clientId, nachMandateRequest)
                    .flatMap(
                        bankId -> {
                          LoanBankAccountDataTableDTO bankAccountDataTableRequest =
                              LoanBankAccountDataTableDTO.builder()
                                  .bankAccountNumber(nachMandateRequest.getBankAccountNumber())
                                  .ifscCode(nachMandateRequest.getIfsc())
                                  .bankName(nachMandateRequest.getBankName())
                                  .bankId(bankId)
                                  .accountHolderName(nachMandateRequest.getBankAccountHolderName())
                                  .accountType(nachMandateRequest.getBankAccountType())
                                  .build();

                          return bankVerificationService
                              .verifyBank(
                                  bankAccountDataTableRequest,
                                  clientId,
                                  productCode,
                                  bankId,
                                  loanId)
                              .flatMap(
                                  response -> {
                                    log.info(
                                        "[UPLOAD_NACH_MANDATE] Bank verification completed for"
                                            + " loanId: {}",
                                        loanId);
                                    boolean isVerified =
                                        PASS.equalsIgnoreCase(response.getBankVerificationStatus());
                                    if (!verificationFlag && !isVerified) {
                                      String errorMessage =
                                          bankVerificationService.getErrorMessage(response);
                                      String errorMessageForResponse =
                                          StringUtils.isNotBlank(errorMessage)
                                              ? errorMessage
                                              : CLIENT_ERROR;
                                      LoanReject loanReject =
                                          LoanReject.builder()
                                              .reasonCode(reasonCodeFactory.getPennyDropCode())
                                              .description(PENNY_DROP_NAME_MATCH_REJECTION)
                                              .build();

                                      return m2PWrapperApi
                                          .rejectLoanApplication(loanReject, loanId)
                                          .doOnSuccess(
                                              v ->
                                                  log.info(
                                                      "[NACH_PENNY_DROP] successfully processed"
                                                          + " rejection for {}",
                                                      loanId))
                                          .doOnError(
                                              e ->
                                                  log.error(
                                                      "[ERROR][NACH_PENNY_DROP] failed to process"
                                                          + " rejection for {}: {}",
                                                      loanId,
                                                      e.getMessage()))
                                          .map(
                                              v ->
                                                  new PennyDropOutcome(
                                                      true, errorMessageForResponse))
                                          .onErrorResume(
                                              e -> Mono.just(PennyDropOutcome.notRejected()));
                                    }

                                    return Mono.just(PennyDropOutcome.notRejected());
                                  });
                        });
              }

              log.info(
                  "[UPLOAD_NACH_MANDATE] Bank details match, skipping penny drop API call for"
                      + " loanId: {}",
                  loanId);
              return Mono.just(PennyDropOutcome.notRejected());
            })
        .switchIfEmpty(Mono.just(PennyDropOutcome.notRejected()));
  }

  public Mono<?> createConsent(ConsentRequest consentRequest, String leadId, String loanId) {
    return buildM2PConsentRequest(consentRequest)
        .flatMap(request -> m2PWrapperApi.createConsent(request, leadId, loanId));
  }

  public Mono<M2pConsentRequest> buildM2PConsentRequest(ConsentRequest consentRequest) {
    return Mono.defer(
        () -> {
          if (ObjectUtils.isEmpty(consentRequest)) {
            return Mono.error(
                new ClientSideException(
                    "consentRequest cannot be null", null, HttpStatus.BAD_REQUEST));
          }
          try {
            String additionalDetails = consentRequest.getAdditionalDetails();
            JsonObject additionalDetailsObj;
            if (StringUtils.isNotBlank(additionalDetails)) {
              additionalDetailsObj = JsonParser.parseString(additionalDetails).getAsJsonObject();
            } else {
              additionalDetailsObj = new JsonObject();
            }
            String dateTime = consentRequest.getDateTime();
            if (StringUtils.isNotBlank(dateTime)) {
              additionalDetailsObj.addProperty("approvedDateTime", dateTime);
            }
            M2pConsentRequest result =
                M2pConsentRequest.builder()
                    .consentKey(consentRequest.getConsentKey())
                    .ipAddress(consentRequest.getIpAddress())
                    .isAccepted(Boolean.TRUE.equals(consentRequest.getIsAccepted()))
                    .additionalDetails(gson.toJson(additionalDetailsObj))
                    .build();
            return Mono.just(result);
          } catch (Exception e) {
            return Mono.error(
                new ClientSideException(
                    "failed to parse additionalDetails json in the request body",
                    null,
                    HttpStatus.BAD_REQUEST));
          }
        });
  }

  public Mono<M2pLoanRejectResponseDTO> rejectLoanApplication(
      LoanReject rejectionData, String loanId) {
    rejectionData.setReasonCode(reasonCodeFactory.getPartnerRejectionReasonCode());
    return m2PWrapperApi.rejectLoanApplication(rejectionData, loanId);
  }

  public Mono<?> getRepaymentScheduleByLoanId(String loanId) {
    return m2PWrapperApi.getRepaymentScheduleByLoanId(loanId);
  }

  public Mono<?> completeBreStep(String loanId, String productCode) {
    return registerM2PCta(loanId, productCode, BRE_CTA_IDENTIFIER)
        .flatMap(
            data ->
                productConfigMasterService
                    .getProductConfigMasterData(productCode)
                    .flatMap(
                        productControlConfigData -> {
                          ProductControl.Flow flowData =
                              productConfigMasterService.getFlowFromProductConfig(
                                  productControlConfigData.getT2(), FI_CTA_IDENTIFIER);
                          if (Objects.isNull(flowData)) {
                            return Mono.error(
                                new BaseException(
                                    SOMETHING_WENT_WRONG_CONFIG,
                                    SOMETHING_WENT_WRONG_CONFIG,
                                    HttpStatus.INTERNAL_SERVER_ERROR));
                          }
                          if (flowData.isCtaCallFlag()) {
                            return checkForBreApproved(loanId)
                                .flatMap(
                                    breApprovedStatus -> {
                                      if (Boolean.TRUE.equals(breApprovedStatus)) {
                                        log.info(
                                            "[{}] registering fi step cta hit for loan: {}",
                                            FI_CTA_IDENTIFIER,
                                            loanId);
                                        return registerM2PCta(
                                                loanId, productCode, FI_CTA_IDENTIFIER)
                                            .flatMap(ctaResponse -> Mono.just(data));
                                      }
                                      log.info(
                                          "[{}] cancelling fi step cta hit for loan: {}",
                                          FI_CTA_IDENTIFIER,
                                          loanId);
                                      return Mono.just(data);
                                    });
                          }
                          return Mono.just(data);
                        }));
  }

  public Mono<?> completeStartKycStep(String loanId, String productCode) {
    return registerM2PCta(loanId, productCode, START_KYC_CTA_IDENTIFIER);
  }

  private Mono<Boolean> checkForBreApproved(String loanId) {
    return breStatusService
        .findByExternalIdAndSuccessStatusAndOnlyApprovedState(loanId)
        .flatMap(data -> Mono.just(Boolean.TRUE))
        .defaultIfEmpty(false);
  }

  private Mono<?> registerM2PCta(String loanId, String productCode, String identifier) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), identifier);
              if (Objects.isNull(flowData)) {
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }
              return m2PWrapperApi.registerCta(loanId, flowData.getCtaName());
            });
  }

  public Mono<Boolean> processPreDisbursementQcChecks(
      ProductControl.Flow flowData, String loanId, String leadId, String productCode) {
    if (flowData.getDisbValidationCondition() != null
        && flowData.getDisbValidationCondition().containsKey("validateCheck")
        && Boolean.TRUE.equals(flowData.getDisbValidationCondition().get("validateCheck"))) {
      return m2PWrapperApi
          .getDisbursalCheckData(loanId)
          .flatMap(
              disbursalChecksData -> {
                if (disbursalChecksData.isEmpty()) {
                  return Mono.error(new NotFoundException(SOMETHING_WENT_WRONG));
                }
                M2PDisbursementCheckDetailDTO disbCheckCondition = disbursalChecksData.get(0);
                Mono<Boolean> errorCheck = Mono.just(false);
                if (flowData.getDisbValidationCondition().containsKey("hasLoanCheck")
                    && Boolean.TRUE.equals(
                        flowData.getDisbValidationCondition().get("hasLoanCheck"))) {
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError -> {
                            boolean currentCheckFailed =
                                Boolean.TRUE.equals(disbCheckCondition.getHasLoan());
                            if (currentCheckFailed) {
                              log.error(
                                  "[{}] [QC_BREACH] loan already exist in disbursed initiated or"
                                      + " completed state, lead id: {}, client id: {}",
                                  PRE_DISBURSAL_VALIDATION,
                                  loanId,
                                  leadId);
                              qcCheckStoreService
                                  .asyncSaveBreach(
                                      loanId,
                                      leadId,
                                      "hasLoan",
                                      null,
                                      null,
                                      PRE_DISB_QC,
                                      productCode)
                                  .subscribe();
                            }
                            return Mono.just(accumulatedError || currentCheckFailed);
                          });
                }
                if (flowData.getDisbValidationCondition().containsKey("hasLimitCheck")
                    && Boolean.TRUE.equals(
                        flowData.getDisbValidationCondition().get("hasLimitCheck"))) {
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError -> {
                            boolean currentCheckFailed =
                                Boolean.TRUE.equals(disbCheckCondition.getHasLimit());
                            if (currentCheckFailed) {
                              log.error(
                                  "[{}] [QC_BREACH] limit active already exist in disbursed"
                                      + " initiated or completed state, lead id: {}, client id: {}",
                                  PRE_DISBURSAL_VALIDATION,
                                  loanId,
                                  leadId);
                              qcCheckStoreService
                                  .asyncSaveBreach(
                                      loanId,
                                      leadId,
                                      "hasLimit",
                                      null,
                                      null,
                                      PRE_DISB_QC,
                                      productCode)
                                  .subscribe();
                            }
                            return Mono.just(accumulatedError || currentCheckFailed);
                          });
                }
                if (flowData.getDisbValidationCondition().containsKey("breChecksEnabled")
                    && Boolean.TRUE.equals(
                        flowData.getDisbValidationCondition().get("breChecksEnabled"))) {
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError ->
                              getBreQcResult(
                                      flowData, loanId, leadId, disbCheckCondition, productCode)
                                  .map(
                                      breQcResult -> {
                                        boolean currentCheckFailed =
                                            Boolean.TRUE.equals(breQcResult);
                                        if (currentCheckFailed) {
                                          log.error(
                                              "[{}] [QC_BREACH] bre qc checks failed, lead id: {},"
                                                  + CLIENT_ID_LOG_LITERAL,
                                              PRE_DISBURSAL_VALIDATION,
                                              loanId,
                                              leadId);
                                        }
                                        return accumulatedError || currentCheckFailed;
                                      }));
                }
                if (flowData.getDisbValidationCondition().containsKey("checkEsignStatus")
                    && Boolean.TRUE.equals(
                        flowData.getDisbValidationCondition().get("checkEsignStatus"))) {
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError -> {
                            boolean currentCheckFailed =
                                !Boolean.TRUE.equals(disbCheckCondition.getEsignStatus());
                            if (currentCheckFailed) {
                              log.error(
                                  "[{}] [QC_BREACH] e-sign status not completed, lead id: {},"
                                      + CLIENT_ID_LOG_LITERAL,
                                  PRE_DISBURSAL_VALIDATION,
                                  loanId,
                                  leadId);
                              qcCheckStoreService
                                  .asyncSaveBreach(
                                      loanId, leadId, "esign", null, null, PRE_DISB_QC, productCode)
                                  .subscribe();
                            }
                            return Mono.just(accumulatedError || currentCheckFailed);
                          });
                }
                if (flowData.getDisbValidationCondition().containsKey("loanAmountCheck")
                    && Boolean.TRUE.equals(
                        flowData.getDisbValidationCondition().get("loanAmountCheck"))) {
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError -> {
                            boolean currentCheckFailed =
                                disbCheckCondition.getLoanAmountRequest()
                                    > disbCheckCondition.getBreAmount();
                            if (currentCheckFailed) {
                              log.error(
                                  "[{}] [QC_BREACH] requested loan amount greater than bre amount,"
                                      + " lead id: {}, client id: {}",
                                  PRE_DISBURSAL_VALIDATION,
                                  loanId,
                                  leadId);
                              qcCheckStoreService
                                  .asyncSaveBreach(
                                      loanId,
                                      leadId,
                                      "loan-amount",
                                      disbCheckCondition.getBreAmount(),
                                      disbCheckCondition.getLoanAmountRequest(),
                                      PRE_DISB_QC,
                                      productCode)
                                  .subscribe();
                            }
                            return Mono.just(accumulatedError || currentCheckFailed);
                          });
                }
                if (flowData.getDisbValidationCondition().containsKey("dobCheck")
                    && Boolean.TRUE.equals(flowData.getDisbValidationCondition().get("dobCheck"))) {
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError -> {
                            boolean currentCheckFailed =
                                !Objects.equals(
                                    disbCheckCondition.getClientDob(),
                                    disbCheckCondition.getAadhaarDob());
                            if (currentCheckFailed) {
                              log.error(
                                  "[{}] [QC_BREACH] client dob doesn't match with aadhaar dob, lead"
                                      + " id: {}, client id: {}",
                                  PRE_DISBURSAL_VALIDATION,
                                  loanId,
                                  leadId);
                              qcCheckStoreService
                                  .asyncSaveBreach(
                                      loanId,
                                      leadId,
                                      "dob",
                                      disbCheckCondition.getAadhaarDob(),
                                      disbCheckCondition.getClientDob(),
                                      PRE_DISB_QC,
                                      productCode)
                                  .subscribe();
                            }
                            return Mono.just(accumulatedError || currentCheckFailed);
                          });
                }
                if (flowData.getDisbValidationCondition().containsKey("aprThreshold")) {
                  Number aprThreshold =
                      (Number) flowData.getDisbValidationCondition().get("aprThreshold");
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError -> {
                            boolean currentCheckFailed =
                                disbCheckCondition.getApr() == null
                                    || disbCheckCondition.getApr() > aprThreshold.doubleValue();

                            if (currentCheckFailed) {
                              log.error(
                                  "[{}] [QC_BREACH] apr is greater than threshold, lead id: {},"
                                      + CLIENT_ID_LOG_LITERAL,
                                  PRE_DISBURSAL_VALIDATION,
                                  loanId,
                                  leadId);
                              qcCheckStoreService
                                  .asyncSaveBreach(
                                      loanId,
                                      leadId,
                                      "apr",
                                      aprThreshold.doubleValue(),
                                      disbCheckCondition.getApr(),
                                      PRE_DISB_QC,
                                      productCode)
                                  .subscribe();
                            }
                            return Mono.just(accumulatedError || currentCheckFailed);
                          });
                }
                if (flowData.getDisbValidationCondition().containsKey("pfThreshold")) {
                  Number pfThreshold =
                      (Number) flowData.getDisbValidationCondition().get("pfThreshold");
                  errorCheck =
                      errorCheck.flatMap(
                          accumulatedError -> {
                            boolean currentCheckFailed =
                                disbCheckCondition.getPf() > pfThreshold.doubleValue();
                            if (currentCheckFailed) {
                              log.error(
                                  "[{}] [QC_BREACH] pf is greater than threshold, lead id: {},"
                                      + CLIENT_ID_LOG_LITERAL,
                                  PRE_DISBURSAL_VALIDATION,
                                  loanId,
                                  leadId);
                              qcCheckStoreService
                                  .asyncSaveBreach(
                                      loanId,
                                      leadId,
                                      "pf",
                                      pfThreshold.doubleValue(),
                                      disbCheckCondition.getPf(),
                                      PRE_DISB_QC,
                                      productCode)
                                  .subscribe();
                            }
                            return Mono.just(accumulatedError || currentCheckFailed);
                          });
                }
                return errorCheck;
              });
    }
    return Mono.just(false);
  }

  private Mono<Boolean> getBreQcResult(
      ProductControl.Flow flowData,
      String loanApplicationId,
      String clientId,
      M2PDisbursementCheckDetailDTO disbursementData,
      String productCode) {
    return breStatusService
        .findByExternalIdAndStatusAndScienapticEligibleStatus(loanApplicationId)
        .flatMap(
            breResponse -> {
              String breType =
                  flowData.getDisbValidationCondition().getOrDefault("breType", "").toString();
              return switch (breType) {
                case "multiple" ->
                    getMultipleOfferBreQcResult(
                        flowData,
                        breResponse,
                        disbursementData,
                        loanApplicationId,
                        clientId,
                        productCode);
                case "single" ->
                    getSingleOfferBreQcResult(
                        flowData,
                        breResponse,
                        disbursementData,
                        loanApplicationId,
                        clientId,
                        productCode);
                case "limit-based" ->
                    getLimitBreQcResult(
                        flowData,
                        breResponse,
                        disbursementData,
                        loanApplicationId,
                        clientId,
                        productCode);
                default -> Mono.just(false);
              };
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[{}] no eligible bre available for loan application id: {}",
                      PRE_DISBURSAL_VALIDATION,
                      loanApplicationId);
                  qcCheckStoreService
                      .asyncSaveBreach(
                          loanApplicationId, clientId, "no-bre", null, null, BRE_QC, productCode)
                      .subscribe();
                  return Mono.just(true);
                }));
  }

  private Mono<Boolean> getSingleOfferBreQcResult(
      ProductControl.Flow flowData,
      BreStatus breResponse,
      M2PDisbursementCheckDetailDTO disbursementData,
      String loanApplicationId,
      String clientId,
      String productCode) {
    log.info(
        "[{}] single-bre offer qc checks initiated for loan application id: {}",
        PRE_DISBURSAL_VALIDATION,
        loanApplicationId);
    return Mono.defer(
        () -> {
          Json breJson = breResponse.getRequest();
          if (breJson == null) {
            log.error(
                "[{}] [BRE_QC_BREACH] [SINGLE] missing bre request json for loan application id:"
                    + " {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId);
            return Mono.just(true);
          }
          JsonNode breData;
          try {
            breData = objectMapper.readTree(breJson.asString());
          } catch (Exception e) {
            log.error(
                "[{}] error parsing bre json: {}, for loan application id: {}",
                PRE_DISBURSAL_VALIDATION,
                e.getMessage(),
                loanApplicationId,
                e);
            return Mono.just(true);
          }
          Map<String, Object> disbValidationCondition = flowData.getDisbValidationCondition();
          boolean pfCheckEnabled = getFlag(disbValidationCondition, BRE_PF_CHECK);
          boolean roiCheckEnabled = getFlag(disbValidationCondition, BRE_ROI_CHECK);
          boolean amountCheckEnabled = getFlag(disbValidationCondition, BRE_AMT_CHECK);
          boolean tenureCheckEnabled = getFlag(disbValidationCondition, BRE_TENURE_CHECK);
          List<Mono<Boolean>> checks = new ArrayList<>();

          if (pfCheckEnabled) {
            Double brePf = getDouble(breData, "pf");
            Double loanPf = disbursementData.getPf();
            checks.add(
                getBreFieldMismatchResult(
                    brePf, loanPf, loanApplicationId, SINGLE_BRE, "pf", clientId, productCode));
          }
          if (roiCheckEnabled) {
            Double breRoi = getDouble(breData, "roi");
            Double loanRoi = disbursementData.getRoi();
            checks.add(
                getBreFieldMismatchResult(
                    breRoi, loanRoi, loanApplicationId, SINGLE_BRE, "roi", clientId, productCode));
          }
          if (amountCheckEnabled) {
            Double breAmount = getDouble(breData, AMOUNT);
            checks.add(
                getBreFieldMismatchResultWithLessThanOperator(
                    breAmount,
                    disbursementData.getLoanAmountRequest(),
                    loanApplicationId,
                    SINGLE_BRE,
                    AMOUNT,
                    clientId,
                    productCode));
          }
          if (tenureCheckEnabled) {
            Double breTenure = getDouble(breData, TENURE);
            Double loanTenure = disbursementData.getTenure();
            checks.add(
                getBreFieldMismatchResult(
                    breTenure,
                    loanTenure,
                    loanApplicationId,
                    SINGLE_BRE,
                    TENURE,
                    clientId,
                    productCode));
          }
          if (checks.isEmpty()) {
            return Mono.just(false);
          }
          return Mono.zip(
              checks,
              results -> {
                for (Object result : results) {
                  if (Boolean.TRUE.equals(result)) {
                    return true;
                  }
                }
                return false;
              });
        });
  }

  private Mono<Boolean> getMultipleOfferBreQcResult(
      ProductControl.Flow flowData,
      BreStatus breResponse,
      M2PDisbursementCheckDetailDTO disbursementData,
      String loanApplicationId,
      String clientId,
      String productCode) {
    log.info(
        "[{}] [MULTIPLE] multiple-bre offer qc checks initiated for loan application id: {}",
        PRE_DISBURSAL_VALIDATION,
        loanApplicationId);
    return Mono.defer(
        () -> {
          if (breResponse == null || breResponse.getRequest() == null) {
            log.error(
                "[{}] [BRE_QC_BREACH] [MULTIPLE] bre response or request is null for loan"
                    + APPLICATION_LOG_LITERAL,
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId);
            return Mono.just(true);
          }
          String breRequestString;
          try {
            breRequestString = breResponse.getRequest().asString();
          } catch (Exception e) {
            log.error(
                "[{}] [MULTIPLE] failed to read bre request as string for loan application id: {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId,
                e);
            return Mono.just(true);
          }
          if (breRequestString.isBlank()) {
            log.error(
                "[{}] [BRE_QC_BREACH] [MULTIPLE] empty bre request json for loan application id:"
                    + " {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId);
            return Mono.just(true);
          }
          String offersJson;
          try {
            JsonNode breRequestNode = objectMapper.readTree(breRequestString);
            JsonNode dataNode = breRequestNode.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
              log.error(
                  "[{}] [BRE_QC_BREACH] [MULTIPLE] missing or null data field in bre request for"
                      + LOAN_APPLICATION_LOG_LITERAL,
                  PRE_DISBURSAL_VALIDATION,
                  loanApplicationId);
              return Mono.just(true);
            }
            offersJson = dataNode.asText();
          } catch (Exception e) {
            log.error(
                "[{}] [MULTIPLE] error parsing bre request json for loan application id: {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId,
                e);
            return Mono.just(true);
          }
          if (offersJson == null || offersJson.isBlank() || "[]".equals(offersJson.trim())) {
            log.error(
                "[{}] [BRE_QC_BREACH] [MULTIPLE] bre data array is empty or blank for loan"
                    + APPLICATION_LOG_LITERAL,
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId);
            return Mono.just(true);
          }
          List<JsonNode> offers;
          try {
            offers = objectMapper.readValue(offersJson, new TypeReference<List<JsonNode>>() {});
          } catch (Exception e) {
            log.error(
                "[{}] [MULTIPLE] error parsing offers array from bre data for loan application id:"
                    + " {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId,
                e);
            return Mono.just(true);
          }
          if (offers == null || offers.isEmpty()) {
            log.error(
                "[{}] [BRE_QC_BREACH] [MULTIPLE] no offers found in bre data for loan application"
                    + " id: {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId);
            return Mono.just(true);
          }

          Map<String, Object> disbValidationCondition = flowData.getDisbValidationCondition();
          boolean pfCheckEnabled = getFlag(disbValidationCondition, BRE_PF_CHECK);
          boolean roiCheckEnabled = getFlag(disbValidationCondition, BRE_ROI_CHECK);
          boolean amountCheckEnabled = getFlag(disbValidationCondition, BRE_AMT_CHECK);
          boolean tenureCheckEnabled = getFlag(disbValidationCondition, BRE_TENURE_CHECK);

          Double loanAmount = disbursementData.getLoanAmountRequest();
          Double loanTenure = disbursementData.getTenure();
          Double loanRoi = disbursementData.getRoi();
          Double loanPf = disbursementData.getPf();

          if (Objects.isNull(loanTenure)) {
            log.error(
                "[{}] [BRE_QC_BREACH] [MULTIPLE] no tenure found in loan data for loan application"
                    + " id: {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId);
            return Mono.just(true);
          }
          JsonNode matchingOffer =
              offers.stream()
                  .filter(
                      o ->
                          getDouble(o, TENURE) != null
                              && Objects.equals(getDouble(o, TENURE), loanTenure))
                  .findFirst()
                  .orElse(null);
          if (matchingOffer == null) {
            log.error(
                "[{}] [BRE_QC_BREACH] [MULTIPLE] no matching offer found for tenure: {} in bre"
                    + " offers for loan application id: {}",
                PRE_DISBURSAL_VALIDATION,
                loanTenure,
                loanApplicationId);
            return qcCheckStoreService
                .asyncSaveBreach(
                    loanApplicationId, clientId, TENURE, null, loanTenure, BRE_QC, productCode)
                .flatMap(response -> Mono.just(true));
          }
          List<Mono<Boolean>> checks = new ArrayList<>();

          if (pfCheckEnabled) {
            Double brePf = getDouble(matchingOffer, PROCESSING_FEE);
            checks.add(
                getBreFieldMismatchResult(
                    brePf, loanPf, loanApplicationId, MULTIPLE_BRE, "pf", clientId, productCode));
          }
          if (roiCheckEnabled) {
            Double breRoi = getDouble(matchingOffer, "roi");
            checks.add(
                getBreFieldMismatchResult(
                    breRoi,
                    loanRoi,
                    loanApplicationId,
                    MULTIPLE_BRE,
                    "roi",
                    clientId,
                    productCode));
          }
          if (amountCheckEnabled) {
            Double breAmount = getDouble(matchingOffer, "limit");
            Double minBreAmount = getDouble(matchingOffer, "minLimit");
            if (Objects.isNull(minBreAmount)) {
              checks.add(
                  getBreFieldMismatchResultWithLessThanOperator(
                      breAmount,
                      loanAmount,
                      loanApplicationId,
                      MULTIPLE_BRE,
                      AMOUNT,
                      clientId,
                      productCode));
            } else {
              checks.add(
                  getBreFieldMismatchResultWithMinMaxOperator(
                      minBreAmount,
                      breAmount,
                      loanAmount,
                      loanApplicationId,
                      MULTIPLE_BRE,
                      AMOUNT,
                      clientId,
                      productCode));
            }
          }
          if (tenureCheckEnabled) {
            Double breTenure = getDouble(matchingOffer, TENURE);
            checks.add(
                getBreFieldMismatchResult(
                    breTenure,
                    loanTenure,
                    loanApplicationId,
                    MULTIPLE_BRE,
                    TENURE,
                    clientId,
                    productCode));
          }
          if (checks.isEmpty()) {
            return Mono.just(false);
          }
          return Mono.zip(
              checks,
              results -> {
                for (Object result : results) {
                  if (Boolean.TRUE.equals(result)) {
                    return true;
                  }
                }
                return false;
              });
        });
  }

  private Mono<Boolean> getLimitBreQcResult(
      ProductControl.Flow flowData,
      BreStatus breResponse,
      M2PDisbursementCheckDetailDTO disbursementData,
      String loanApplicationId,
      String clientId,
      String productCode) {

    log.info(
        "[{}] limit-bre offer qc checks initiated for loan application id: {}",
        PRE_DISBURSAL_VALIDATION,
        loanApplicationId);

    return Mono.defer(
        () -> {
          Json breJson = breResponse.getRequest();
          if (breJson == null) {
            log.error(
                "[{}] [BRE_QC_BREACH] [LIMIT] missing bre request json for loan application id: {}",
                PRE_DISBURSAL_VALIDATION,
                loanApplicationId);
            return Mono.just(true);
          }

          JsonNode breData;
          try {
            breData = objectMapper.readTree(breJson.asString());
          } catch (Exception e) {
            log.error(
                "[{}] error parsing bre json: {}, for loan application id: {}",
                PRE_DISBURSAL_VALIDATION,
                e.getMessage(),
                loanApplicationId,
                e);
            return Mono.just(true);
          }
          Map<String, Object> disbValidationCondition = flowData.getDisbValidationCondition();
          boolean pfCheckEnabled = getFlag(disbValidationCondition, BRE_PF_CHECK);
          boolean roiCheckEnabled = getFlag(disbValidationCondition, BRE_ROI_CHECK);
          boolean amountCheckEnabled = getFlag(disbValidationCondition, BRE_AMT_CHECK);
          boolean tenureCheckEnabled = getFlag(disbValidationCondition, BRE_TENURE_CHECK);
          List<Mono<Boolean>> checks = new ArrayList<>();

          if (pfCheckEnabled) {
            Double brePf = getDouble(breData, "pf");
            Double loanPf = disbursementData.getPf();
            checks.add(
                getBreFieldMismatchResult(
                    brePf, loanPf, loanApplicationId, LIMIT_BRE, "pf", clientId, productCode));
          }
          if (roiCheckEnabled) {
            Double breRoi = getDouble(breData, "roi");
            Double loanRoi = disbursementData.getRoi();
            checks.add(
                getBreFieldMismatchResult(
                    breRoi, loanRoi, loanApplicationId, LIMIT_BRE, "roi", clientId, productCode));
          }
          if (amountCheckEnabled) {
            Double breAmount = getDouble(breData, AMOUNT);
            checks.add(
                getBreFieldMismatchResultWithLessThanOperator(
                    breAmount,
                    disbursementData.getLoanAmountRequest(),
                    loanApplicationId,
                    LIMIT_BRE,
                    AMOUNT,
                    clientId,
                    productCode));
          }
          if (tenureCheckEnabled) {
            Double breTenure = getDouble(breData, TENURE);
            Double loanTenure = disbursementData.getTenure();
            checks.add(
                getBreFieldMismatchResultWithLessThanOperator(
                    breTenure,
                    loanTenure,
                    loanApplicationId,
                    LIMIT_BRE,
                    TENURE,
                    clientId,
                    productCode));
          }
          if (checks.isEmpty()) {
            return Mono.just(false);
          }
          return Mono.zip(
              checks,
              results -> {
                for (Object result : results) {
                  if (Boolean.TRUE.equals(result)) {
                    return true;
                  }
                }
                return false;
              });
        });
  }

  private Mono<Boolean> getBreFieldMismatchResult(
      Double breValue,
      Double loanValue,
      String loanApplicationId,
      String breType,
      String fieldName,
      String clientId,
      String productCode) {
    if (breValue == null || loanValue == null) {
      log.error(
          "[{}] [BRE_QC_BREACH] [{}] {} null in bre: {}, loan: {}, for loan"
              + APPLICATION_LOG_LITERAL,
          PRE_DISBURSAL_VALIDATION,
          breType,
          fieldName,
          breValue,
          loanValue,
          loanApplicationId);
      return qcCheckStoreService
          .asyncSaveBreach(
              loanApplicationId, clientId, fieldName, breValue, loanValue, BRE_QC, productCode)
          .flatMap(response -> Mono.just(true));
    } else if (!breValue.equals(loanValue)) {
      log.error(
          "[{}] [BRE_QC_BREACH] [{}] {} mismatch in bre: {}, loan: {}, for loan"
              + APPLICATION_LOG_LITERAL,
          PRE_DISBURSAL_VALIDATION,
          breType,
          fieldName,
          breValue,
          loanValue,
          loanApplicationId);
      return qcCheckStoreService
          .asyncSaveBreach(
              loanApplicationId, clientId, fieldName, breValue, loanValue, BRE_QC, productCode)
          .flatMap(response -> Mono.just(true));
    }
    return Mono.just(false);
  }

  private Mono<Boolean> getBreFieldMismatchResultWithLessThanOperator(
      Double breValue,
      Double loanValue,
      String loanApplicationId,
      String breType,
      String fieldName,
      String clientId,
      String productCode) {
    if (breValue == null || loanValue == null) {
      log.error(
          "[{}] [BRE_QC_BREACH] [{}] {} null in bre: {}, loan: {}, for loan"
              + APPLICATION_LOG_LITERAL,
          PRE_DISBURSAL_VALIDATION,
          breType,
          fieldName,
          breValue,
          loanValue,
          loanApplicationId);
      return qcCheckStoreService
          .asyncSaveBreach(
              loanApplicationId, clientId, fieldName, breValue, loanValue, BRE_QC, productCode)
          .flatMap(response -> Mono.just(true));
    } else if (breValue < loanValue) {
      log.error(
          "[{}] [BRE_QC_BREACH] [{}] {} mismatch in bre: {}, loan: {}, for loan"
              + APPLICATION_LOG_LITERAL,
          PRE_DISBURSAL_VALIDATION,
          breType,
          fieldName,
          breValue,
          loanValue,
          loanApplicationId);
      return qcCheckStoreService
          .asyncSaveBreach(
              loanApplicationId, clientId, fieldName, breValue, loanValue, BRE_QC, productCode)
          .flatMap(response -> Mono.just(true));
    }
    return Mono.just(false);
  }

  private Mono<Boolean> getBreFieldMismatchResultWithMinMaxOperator(
      Double minBreValue,
      Double maxBreValue,
      Double loanValue,
      String loanApplicationId,
      String breType,
      String fieldName,
      String clientId,
      String productCode) {
    String breObject = minBreValue + " to " + maxBreValue;
    if (minBreValue == null || maxBreValue == null || loanValue == null) {
      log.error(
          "[{}] [BRE_QC_BREACH] [{}] {} null in bre, minBreValue: {}, maxBreValue: {}, loan: {},"
              + " for loan"
              + APPLICATION_LOG_LITERAL,
          PRE_DISBURSAL_VALIDATION,
          breType,
          fieldName,
          minBreValue,
          maxBreValue,
          loanValue,
          loanApplicationId);
      return qcCheckStoreService
          .asyncSaveBreach(
              loanApplicationId, clientId, fieldName, breObject, loanValue, BRE_QC, productCode)
          .flatMap(response -> Mono.just(true));
    } else if (loanValue > maxBreValue || loanValue < minBreValue) {
      log.error(
          "[{}] [BRE_QC_BREACH] [{}] {} mismatch in bre, minBreValue: {}, maxBreValue: {}, loan:"
              + " {}, for loan"
              + APPLICATION_LOG_LITERAL,
          PRE_DISBURSAL_VALIDATION,
          breType,
          fieldName,
          minBreValue,
          maxBreValue,
          loanValue,
          loanApplicationId);
      return qcCheckStoreService
          .asyncSaveBreach(
              loanApplicationId, clientId, fieldName, breObject, loanValue, BRE_QC, productCode)
          .flatMap(response -> Mono.just(true));
    }
    return Mono.just(false);
  }

  private boolean getFlag(Map<String, Object> map, String key) {
    if (map == null || !map.containsKey(key)) return false;
    Object value = map.get(key);
    return value instanceof Boolean booleanValue && booleanValue;
  }

  private Double getDouble(JsonNode json, String key) {
    if (json.has(key) && !json.get(key).isNull()) {
      try {
        return json.get(key).asDouble();
      } catch (Exception e) {
        log.error(
            "[{}] error parsing double for key: {}, from bre data",
            PRE_DISBURSAL_VALIDATION,
            key,
            e);
      }
    }
    return null;
  }

  public Mono<?> uploadDocumentsAgainstLoan(
      BulkDocumentsUploadRequest bulkDocumentsUploadRequest, String loanId, String productCode) {

    uploadBase64DocumentsToS3(bulkDocumentsUploadRequest, productCode);

    return processDigioEligibleDocuments(bulkDocumentsUploadRequest, loanId, productCode)
        .then(
            Mono.defer(
                () ->
                    FileValidatorUtil.validateDocumentsForUpload(bulkDocumentsUploadRequest, loanId)
                        .flatMap(
                            results ->
                                requireAllDocumentsValid(results, loanId)
                                    .then(
                                        Mono.defer(
                                            () ->
                                                uploadToM2pAndPostProcess(
                                                    bulkDocumentsUploadRequest,
                                                    loanId,
                                                    productCode))))));
  }

  private void uploadBase64DocumentsToS3(BulkDocumentsUploadRequest request, String productCode) {
    for (BulkDocumentsUploadRequest.DocumentDetailsDTO docDetails : request.getDocuments()) {
      BulkDocumentsUploadRequest.DocumentInfoDTO docInfo = docDetails.getDocument();
      if (docInfo != null && BASE64.equalsIgnoreCase(docInfo.getStorageType())) {
        if (docInfo.getEncodedFile() == null || docInfo.getEncodedFile().isEmpty()) {
          log.warn("file content is null or empty for file: {}", docInfo.getFileName());
          throw new BaseException(EMPTY_FILE_CONTENT, EMPTY_FILE_CONTENT, HttpStatus.BAD_REQUEST);
        }
        String preSignedUrl = uploadFileToS3(docInfo, productCode);
        docInfo.setFilePath(preSignedUrl);
      }
    }
  }

  private Mono<Void> processDigioEligibleDocuments(
      BulkDocumentsUploadRequest request, String loanId, String productCode) {
    return Flux.fromIterable(request.getDocuments())
        .filter(
            doc ->
                doc.getDocument() != null
                    && doc.getDocument().getFilePath() != null
                    && doc.getDocument().getFileName() != null
                    && tagEligibilityValidatorRegistry.isDigioEligibleTag(doc.getTag()))
        .flatMap(doc -> processSingleDigioDocument(doc, loanId, productCode))
        .then()
        .doOnSuccess(
            v ->
                log.info(
                    "[UPLOAD_DOC] Digio flow complete for loanId={}, starting file validation",
                    loanId))
        .doOnError(
            e ->
                log.error(
                    "[UPLOAD_DOC] Digio flow failed for loanId={}: {}", loanId, e.getMessage(), e));
  }

  private Mono<DocS3UploadService.S3UploadResult> processSingleDigioDocument(
      BulkDocumentsUploadRequest.DocumentDetailsDTO doc, String loanId, String productCode) {
    TagEligibilityValidator validator =
        tagEligibilityValidatorRegistry
            .getValidator(doc.getTag())
            .orElseThrow(
                () ->
                    new BaseException(
                        "No eligibility validator for tag: " + doc.getTag(),
                        "Unsupported tag",
                        HttpStatus.BAD_REQUEST));
    return validator
        .validateEligibility(loanId, productCode)
        .then(
            digioRestructureEsignService.processRestructureLoanAgreement(
                doc.getDocument().getFilePath(),
                doc.getDocument().getFileName(),
                loanId,
                productCode))
        .doOnNext(
            result -> {
              doc.getDocument().setFilePath(result.presignedUrl());
              doc.getDocument().setS3Path(result.s3Path());
              log.info(
                  "[UPLOAD_DOC] Digio complete, filePath and s3Path set for doc: {}",
                  doc.getDocument().getFileName());
            });
  }

  private Mono<Void> requireAllDocumentsValid(
      List<AbstractMap.SimpleEntry<BulkDocumentsUploadRequest.DocumentDetailsDTO, Boolean>> results,
      String loanId) {
    List<BulkDocumentsUploadRequest.DocumentDetailsDTO> invalidDocs =
        results.stream().filter(entry -> !entry.getValue()).map(Map.Entry::getKey).toList();

    if (!invalidDocs.isEmpty()) {
      return Mono.error(
          new RuntimeException(
              "Validation failed for documents: "
                  + invalidDocs.stream()
                      .map(
                          doc ->
                              doc.getDocument().getFileName()
                                  + " ("
                                  + (doc.getDocument().getFileType() != null
                                      ? doc.getDocument().getFileType()
                                      : MediaType.APPLICATION_PDF_VALUE)
                                  + ")")
                      .toList()));
    }
    return Mono.empty();
  }

  private Mono<?> uploadToM2pAndPostProcess(
      BulkDocumentsUploadRequest request, String loanId, String productCode) {
    M2pBulkDocumentsUploadDTO m2pBulkDocumentsUploadDTO = mapToM2pBulkDocDTO(request);

    log.info(
        "[UPLOAD_DOC] Calling M2P upload for loanId={}, docCount={}",
        loanId,
        m2pBulkDocumentsUploadDTO.getDocuments().size());

    return m2PWrapperApi
        .uploadDocumentsAgainstLoan(loanId, m2pBulkDocumentsUploadDTO)
        .cast(M2pDocumentsUploadResponseDTO.class)
        .doOnSuccess(
            data -> {
              if (data != null) {
                productConfigMasterService
                    .getProductConfigMasterData(productCode)
                    .subscribe(
                        productControlTuple -> {
                          ProductControl.Flow businessLoanConfig =
                              productConfigMasterService.getFlowFromProductConfig(
                                  productControlTuple.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
                          if (businessLoanConfig != null
                              && Boolean.TRUE.equals(businessLoanConfig.getIsBusinessLoan())) {
                            businessLoanEvaluationService.publishBusinessLoanOcrMessagesAsync(
                                data, loanId, productCode, businessLoanConfig);
                            applyLoanAgreementLspStatusFromUpload(request, loanId, productCode);
                          }
                        });
              }
            })
        .doOnError(
            e ->
                log.error(
                    "[UPLOAD_DOC] M2P upload failed for loanId={}: {}", loanId, e.getMessage(), e))
        .flatMap(
            data -> {
              clearBase64FilePathFromResponse(data);
              return updateRestructureDetailsAfterM2pUpload(loanId, request, data)
                  .then(triggerPostUploadCtaIfRequired(loanId, productCode, request, data));
            });
  }

  /**
   * For LOAN_AGREEMENT documents with optional {@code loanStatus}: updates {@code lsp_status} on
   * loan_type_classification (business loan products only; caller gates via config).
   */
  private void applyLoanAgreementLspStatusFromUpload(
      BulkDocumentsUploadRequest request, String loanId, String productCode) {
    if (request.getDocuments() == null) {
      return;
    }
    for (BulkDocumentsUploadRequest.DocumentDetailsDTO doc : request.getDocuments()) {
      if (doc == null || doc.getTag() != DocumentTag.LOAN_AGREEMENT) {
        continue;
      }
      String loanStatus = doc.getLoanStatus();
      if (loanStatus == null || loanStatus.isBlank()) {
        continue;
      }
      businessLoanEvaluationService.applyLspStatusFromLoanAgreementUploadAsync(
          loanId, productCode, loanStatus.trim());
    }
  }

  private void clearBase64FilePathFromResponse(M2pDocumentsUploadResponseDTO data) {
    if (data.getDocuments() == null) {
      return;
    }
    data.getDocuments()
        .forEach(
            docResponse -> {
              var details = docResponse.getDocumentDetails();
              if (details == null || details.getDocument() == null) {
                return;
              }
              var document = details.getDocument();
              if (BASE64.equalsIgnoreCase(document.getStorageType())) {
                document.setFilePath(null);
              }
            });
  }

  private Mono<M2pDocumentsUploadResponseDTO> triggerPostUploadCtaIfRequired(
      String loanId,
      String productCode,
      BulkDocumentsUploadRequest request,
      M2pDocumentsUploadResponseDTO data) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), UPLOAD_DOC_CTA_IDENTIFIER);
              if (Objects.isNull(flowData)) {
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }
              List<String> documentsFromRequest = getDocumentsFromRequest(request);
              String ctaName = getCtaNameAgainstDocumentsCategory(documentsFromRequest, flowData);
              if (!Objects.isNull(ctaName)) {
                log.info("[{}] triggering post document upload cta: {}", UPLOAD_DOC_LOAN, loanId);
                return m2PWrapperApi
                    .registerCta(loanId, ctaName)
                    .flatMap(response -> Mono.just(data));
              }
              log.info("[{}] post document upload cta cancelled: {}", UPLOAD_DOC_LOAN, loanId);
              return Mono.just(data);
            });
  }

  private Mono<Void> updateRestructureDetailsAfterM2pUpload(
      String loanId,
      BulkDocumentsUploadRequest request,
      M2pDocumentsUploadResponseDTO m2pResponse) {
    var restructureRequestDoc =
        request.getDocuments().stream()
            .filter(doc -> doc.getTag() == DocumentTag.RESCHEDULE_AGREEMENT)
            .findFirst();
    if (restructureRequestDoc.isEmpty()) {
      return Mono.empty();
    }

    var restructureResponseDoc =
        m2pResponse.getDocuments() == null
            ? java.util.Optional.<M2pDocumentsUploadResponseDTO.Doc>empty()
            : m2pResponse.getDocuments().stream()
                .filter(
                    doc ->
                        doc.getDocumentDetails() != null
                            && DocumentTag.RESCHEDULE_AGREEMENT
                                .getDisplayName()
                                .equals(doc.getDocumentDetails().getTag()))
                .findFirst();

    if (restructureResponseDoc.isEmpty()) {
      return Mono.empty();
    }

    var doc = restructureRequestDoc.get().getDocument();
    String s3PathToSave = doc.getS3Path() != null ? doc.getS3Path() : doc.getFilePath();
    String signedDocId = String.valueOf(restructureResponseDoc.get().getDocumentId());
    Long leadId;
    try {
      leadId = Long.parseLong(loanId);
    } catch (NumberFormatException e) {
      log.warn("[RESTRUCTURE_UPDATE] Invalid loanId for restructure update: {}", loanId);
      return Mono.empty();
    }

    return restructureDetailsRepository
        .findByLeadAndEligibilityAndRestructure(leadId, true, "NOT_TRIGGERED")
        .flatMap(
            entity -> {
              entity.setSignedUrl(s3PathToSave);
              entity.setSignedDocId(signedDocId);
              entity.setUpdatedAt(LocalDateTime.now());
              return Mono.just(entity);
            })
        .flatMap(restructureDetailsRepository::save)
        .doOnSuccess(
            e ->
                log.info(
                    "[RESTRUCTURE_UPDATE] Updated restructure details for leadId={},"
                        + " signedDocId={}",
                    loanId,
                    signedDocId))
        .doOnError(
            err ->
                log.error(
                    "[RESTRUCTURE_UPDATE] Failed to update restructure details for leadId={}: {}",
                    loanId,
                    err.getMessage()))
        .then();
  }

  private String uploadFileToS3(
      BulkDocumentsUploadRequest.DocumentInfoDTO docInfo, String productCode) {
    return docS3UploadService.uploadPdfAndGetPresignedUrl(
        docInfo.getEncodedFile(), docInfo.getFileName(), productCode);
  }

  private M2pBulkDocumentsUploadDTO mapToM2pBulkDocDTO(
      BulkDocumentsUploadRequest bulkDocumentsUploadRequest) {
    return M2pBulkDocumentsUploadDTO.builder()
        .documents(
            bulkDocumentsUploadRequest.getDocuments().stream()
                .map(
                    doc ->
                        M2pBulkDocumentsUploadDTO.DocumentDetailsDTO.builder()
                            .tag(doc.getTag())
                            .document(
                                M2pBulkDocumentsUploadDTO.DocumentInfoDTO.builder()
                                    .fileName(doc.getDocument().getFileName())
                                    .filePath(doc.getDocument().getFilePath())
                                    .fileType(doc.getDocument().getFileType())
                                    .storageType(doc.getDocument().getStorageType())
                                    .build())
                            .build())
                .toList())
        .build();
  }

  private String getCtaNameAgainstDocumentsCategory(
      List<String> documentsFromRequest, ProductControl.Flow flowData) {
    List<ProductControl.Flow.CtaConfiguration> ctaConfigurations = flowData.getCtaConfigurations();
    if (Objects.isNull(ctaConfigurations) || ctaConfigurations.isEmpty()) {
      return null;
    }
    if (documentsFromRequest.isEmpty()) {
      return null;
    }

    String ctaName = null;
    for (ProductControl.Flow.CtaConfiguration ctaConfiguration : ctaConfigurations) {
      List<String> documentsFromProductConfig = ctaConfiguration.getDocuments();
      if (Objects.isNull(documentsFromProductConfig) || documentsFromProductConfig.isEmpty()) {
        continue;
      }
      if (checkDocumentsIntersectionForNoCtaCall(
          documentsFromRequest, documentsFromProductConfig)) {
        ctaName = ctaConfiguration.getCtaName();
      }
    }
    return ctaName;
  }

  private boolean checkDocumentsIntersectionForNoCtaCall(
      List<String> documentsFromRequest, List<String> documentsFromProductConfig) {
    return documentsFromRequest.stream().anyMatch(documentsFromProductConfig::contains);
  }

  private List<String> getDocumentsFromRequest(
      BulkDocumentsUploadRequest bulkDocumentsUploadRequest) {
    List<String> documentsFromRequest = new ArrayList<>();
    for (BulkDocumentsUploadRequest.DocumentDetailsDTO document :
        bulkDocumentsUploadRequest.getDocuments()) {
      if (document != null && document.getTag() != null) {
        documentsFromRequest.add(document.getTag().getDisplayName());
      }
    }
    return documentsFromRequest;
  }

  public Mono<ByteArrayResource> getDocumentByLoanIdAndDocumentId(
      String loanId, String documentId) {
    return m2PWrapperApi
        .getDocumentByLoanIdAndDocumentId(loanId, documentId)
        .map(ByteArrayResource::new);
  }

  public Mono<?> undoApproveLoan(String loanId) {
    return getLoanApplicationByLoanIdV2(loanId)
        .flatMap(
            loanResponse -> {
              String status = loanResponse.getLoanApplicationStatus();
              if ("APPLICATION_CREATED".equals(status) || "APPLICATION_APPROVED".equals(status)) {
                return m2PWrapperApi.undoApproveLoan(loanId);
              }
              log.error("[UNDO_APPROVE] Cannot update loan. Current status: {} ", status);
              return Mono.error(
                  new ForbiddenException(
                      "Cannot update loan. Current status: " + status, null, HttpStatus.FORBIDDEN));
            });
  }

  public Mono<?> addCharges(SaveChargeRequest saveChargeRequest, String loanId) {
    LoanChargesDTO chargeData = new LoanChargesDTO();
    chargeData.setChargeId(saveChargeRequest.getChargeId());
    chargeData.setAmount(saveChargeRequest.getAmount());
    // Setting default values for optional fields as false
    chargeData.setIsAmountNonEditable(
        saveChargeRequest.getIsAmountNonEditable() != null
            && saveChargeRequest.getIsAmountNonEditable());
    chargeData.setIsMandatory(
        saveChargeRequest.getIsMandatory() != null && saveChargeRequest.getIsMandatory());
    Mono<Object> chargesMono =
        m2PWrapperApi.addCharges(loanId, chargeData).map(response -> (Object) response);

    return chargesMono.onErrorResume(
        ClientSideException.class,
        ex -> {
          Object errorBody = ex.getResponseBody();

          try {
            ObjectMapper mapper = new ObjectMapper();
            M2pErrorResponseDTO error = mapper.convertValue(errorBody, M2pErrorResponseDTO.class);

            if ("G001".equals(error.getErrorCode())
                && error.getDefaultUserMessage().contains("This charge already exists")) {

              return m2PWrapperApi
                  .getChargeDetail(loanId, saveChargeRequest.getChargeId())
                  .map(
                      dto ->
                          (Object)
                              new M2pResourceResponseDTO(dto.getId(), String.valueOf(dto.getId())));
            }

          } catch (Exception e) {
            return Mono.error(ex);
          }

          return Mono.error(ex);
        });
  }

  public Mono<?> addTopUpDataTable(TopupDataRequest topupDataRequest, String loanId) {
    TopUpDataTableDTO topUpDataTable = new TopUpDataTableDTO();
    topUpDataTable.setTopUpId(topupDataRequest.getTopupId());
    topUpDataTable.setOutstandingAmount(
        String.valueOf(topupDataRequest.getOutstandingAmount().doubleValue()));
    topUpDataTable.setLocale("en");
    topUpDataTable.setDateFormat("MMM dd, yyyy HH:mm");
    topUpDataTable.setSourcingChannel(topupDataRequest.getSourcingChannel());
    return m2PWrapperApi.addTopUpDataTable(topUpDataTable, loanId);
  }

  public Mono<Object> approveLoan(String loanId, String approvedDate) {
    ApproveLoanDTO approveLoanDTO = new ApproveLoanDTO();
    approveLoanDTO.setApprovedDate(approvedDate);
    approveLoanDTO.setDateFormat(DATE_FORMAT);
    return m2PWrapperApi.approveLoan(loanId, approveLoanDTO);
  }

  public Mono<Object> approveLoanWithValidation(String loanId, String approvedDate) {
    ApproveLoanDTO approveLoanDTO = new ApproveLoanDTO();
    approveLoanDTO.setApprovedDate(approvedDate);
    approveLoanDTO.setDateFormat(DATE_FORMAT);

    return breStatusService
        .findByExternalIdOfCompletedAndEligibleBre(loanId)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "[{}] No completed and eligible BRE response found for loanId: {}",
                      NO_BRE_RESPONSE,
                      loanId);
                  return Mono.error(
                      new ForbiddenException(
                          NO_BRE_RESPONSE, NO_BRE_RESPONSE, HttpStatus.FORBIDDEN));
                }))
        .flatMap(breCompletedAndSuccessStatus -> m2PWrapperApi.approveLoan(loanId, approveLoanDTO))
        .defaultIfEmpty(
            Mono.error(
                new ForbiddenException(NO_BRE_RESPONSE, NO_BRE_RESPONSE, HttpStatus.FORBIDDEN)));
  }

  /**
   * Creates loan request based on the loan request type determined by LoanDataUtil.
   *
   * <p>This method delegates the request type determination to LoanDataUtil and creates the
   * appropriate loan via M2PWrapperApi.
   *
   * @param loanData the loan application data
   * @param leadId the lead/client ID
   * @param productCode the product code
   * @return Mono containing the loan creation response
   */
  private Mono<M2pLoanCreationResponseDTO> createLoanByRequestType(
      LoanApplication loanData, String leadId, String productCode) {

    LoanDataUtil.LoanRequestType requestType =
        LoanDataUtil.determineLoanRequestType(loanData, productCode);

    return switch (requestType) {
      case CREDIT_LINE ->
          m2PWrapperApi
              .createLoan(LoanDataUtil.buildCreditLineM2pLoanRequest(loanData), leadId)
              .onErrorResume(Mono::error);
      case TOP_UP ->
          m2PWrapperApi
              .createLoan(LoanDataUtil.getM2pTopUpLoanApplicationRequestDTO(loanData, leadId))
              .onErrorResume(Mono::error);
      case NORMAL ->
          m2PWrapperApi
              .createLoan(LoanDataUtil.getM2pLoanApplicationRequestDTO(loanData), leadId)
              .onErrorResume(Mono::error);
    };
  }

  private Mono<M2pLoanCreationResponseDTO> createLoanApplication(
      LoanApplication loanData, String leadId, String productCode) {

    Mono<M2pLoanCreationResponseDTO> loanMono =
        createLoanByRequestType(loanData, leadId, productCode);

    return loanMono.flatMap(
        response ->
            partnerMasterService
                .getProductIdByCode(productCode)
                .flatMap(
                    partnerId -> {
                      // Persist in DB (fire-and-forget)
                      LoanClientPartnerMapEntity entity =
                          LoanClientPartnerMapEntity.builder()
                              .loanApplicationId(response.getResourceId())
                              .clientId(leadId != null ? Integer.valueOf(leadId) : null)
                              .partnerId(partnerId != null ? Integer.valueOf(partnerId) : null)
                              .createdAt(LocalDateTime.now())
                              .updatedAt(LocalDateTime.now())
                              .build();

                      loanClientPartnerMapRepository
                          .save(entity)
                          .doOnError(
                              err -> log.error("Failed to persist loan-client-partner map", err))
                          .subscribe();

                      // Prepare cache entity
                      LoanApplicationClientPartnerEntity cacheEntity =
                          LoanApplicationClientPartnerEntity.builder()
                              .loanApplicationId(String.valueOf(response.getResourceId()))
                              .clientId(leadId)
                              .partnerId(partnerId)
                              .build();

                      return loanApplicationCacheService
                          .cacheLoanApplicationClientPartner(cacheEntity)
                          .thenReturn(response); // return the original loan response
                    }));
  }

  public Mono<?> getBreStatus(String leadId) {
    return breStatusService
        .findByExternalIdAndBreType(leadId, "SANCTION")
        .map(
            breData -> {
              boolean isActive = Boolean.TRUE.equals(breData.isActive());
              String status = breData.getStatus();
              BreStatusResponse response;

              if (StringUtils.isNotBlank(breData.getDescription())
                  && PAN_NOT_MATCHED.equals(breData.getDescription())) {
                response =
                    BreStatusResponse.builder()
                        .stage("REJECTED")
                        .active(isActive)
                        .status(FAIL)
                        .breResult(breData.getScienapticStatus())
                        .reasons("Customer and Bank account not matching")
                        .data(null)
                        .build();

                log.info(LOGGING_RESPONSE, BRE_STATUS, "trillion", gson.toJson(response));
                return response;
              }

              if ((isActive && FAIL.equals(status)) || (!isActive && SUCCESS.equals(status))) {
                if (COMPLETED.equals(breData.getStage())) {
                  Object request = gson.fromJson(breData.getRequest().asString(), Object.class);
                  String scienapticStatus = extractFieldValue(request, "scienapticstatus");
                  String reasons = extractFieldValue(request, "reasons");
                  String data = extractFieldValue(request, "data");
                  List<Map<String, Object>> result = new ArrayList<>();
                  if (!Objects.isNull(data) && !data.isEmpty()) {
                    result =
                        gson.fromJson(
                            extractFieldValue(request, "data"),
                            new TypeToken<List<Map<String, Object>>>() {}.getType());
                  }

                  response =
                      BreStatusResponse.builder()
                          .stage(
                              INELIGIBLE.equals(scienapticStatus) ? "REJECTED" : breData.getStage())
                          .active(isActive)
                          .status(INELIGIBLE.equals(scienapticStatus) ? FAIL : status)
                          .breResult(scienapticStatus)
                          .reasons(reasons)
                          .data(result)
                          .build();
                } else {
                  response =
                      BreStatusResponse.builder()
                          .stage(breData.getStage())
                          .active(isActive)
                          .status(status)
                          .breResult(null)
                          .reasons(null)
                          .data(null)
                          .build();
                }
              } else {
                response =
                    BreStatusResponse.builder()
                        .stage(IN_PROGRESS)
                        .active(true)
                        .status(SUCCESS)
                        .breResult(null)
                        .reasons(null)
                        .data(null)
                        .build();
              }

              log.info(LOGGING_RESPONSE, BRE_STATUS, "trillion", gson.toJson(response));
              return response;
            })
        .switchIfEmpty(
            Mono.error(
                new BaseException(
                    "BRE status does not exist for given id", null, HttpStatus.NOT_FOUND)));
  }

  public Mono<Map<String, String>> getBankDetailsFromLoanLevelDataTable(String loanId) {
    return m2PWrapperApi
        .getBankDetailsDataTable(loanId)
        .flatMap(
            response -> {
              if (Objects.nonNull(response)
                  && Objects.nonNull(response.getColumnData())
                  && !response.getColumnData().isEmpty()) {
                Map<String, String> resultMap = new HashMap<>();
                response.getColumnData().stream()
                    .max(
                        Comparator.comparing(
                            rowDTO ->
                                rowDTO.getRow().stream()
                                    .filter(col -> "id".equalsIgnoreCase(col.getColumnName()))
                                    .map(col -> Long.parseLong(col.getValue()))
                                    .findFirst()
                                    .orElse(0L)))
                    .ifPresent(
                        maxRowDTO ->
                            maxRowDTO
                                .getRow()
                                .forEach(
                                    columnValueDTO ->
                                        resultMap.put(
                                            columnValueDTO.getColumnName(),
                                            columnValueDTO.getValue())));
                return Mono.just(resultMap);
              }
              return Mono.error(
                  new BaseException(
                      NO_DISBURSAL_BANK_ACCOUNT, NO_DISBURSAL_BANK_ACCOUNT, HttpStatus.NOT_FOUND));
            });
  }

  public Mono<?> addCoApplicant(String loanId, String leadId, String productCode) {
    return m2PWrapperApi
        .addCoApplicant(loanId, leadId)
        .onErrorResume(Mono::error)
        .flatMap(
            data -> {
              if (Objects.isNull(data)) {
                return Mono.error(
                    new BaseException(
                        "CO-APPLICANT ADDITION FAILED", null, HttpStatus.INTERNAL_SERVER_ERROR));
              }
              return Mono.just(data);
            });
  }

  public Mono<?> addPanAadhaarLinkDetailsDataTable(
      PanAadhaarLinkStatusDataTableDTO panAadhaarLinkStatusDataTableDTO, String loanId) {
    return m2PWrapperApi.addPanAadhaarLinkDetailsDataTable(
        panAadhaarLinkStatusDataTableDTO, loanId);
  }

  public Mono<?> getLoanDisbursementStatus(String loanId) {
    return m2PWrapperApi.getLoanDisbursementStatus(loanId);
  }

  public Mono<M2pAadhaarXmlResponseDTO> uploadOkycAadhaarXml(
      AadhaarXmlRequest okycAadhaarXmlRequest, String leadId, String productCode) {

    return aadhaarXmlService
        .uploadAadhaarXml(okycAadhaarXmlRequest, leadId, AadhaarXMLType.OKYC, null, productCode)
        .doOnSuccess(
            response -> {
              validationFunnelService.runDOBWaterfallValidationFunnelAtAadharXMLUpload(
                  okycAadhaarXmlRequest.getRequestString(), leadId, productCode, null);
            });
  }

  public Mono<?> getDocumentList(String loanApplicationId) {
    return m2PWrapperApi.getDocumentList(loanApplicationId);
  }

  /**
   * Retry Risk Categorization for failed Cases
   *
   * <p>This Method involves following steps
   *
   * <ul>
   *   <li>Fetches Failed loan application id from Database
   *   <li>Runs-Risk Categorization for each loan application id
   *   <li><Updates status and lastUpdatedAt data in datatable in Database
   * </ul>
   *
   * @return: ResponseDTO with "status": "SUCCESS/FAIL"
   */
  public Flux<ResponseDTO<RiskCategorizationFailureEntity>> retryRiskCategorizationFailureCase() {
    return riskCategorizationFailureRepository
        .findByStatus(FAIL)
        .flatMap(
            caseData ->
                m2pFacadeService
                    .triggerRiskProcess(caseData.getLoanApplicationId())
                    .flatMap(
                        triggerResult -> {
                          caseData.setLastUpdatedAt(
                              convertEpochMilliToIst(System.currentTimeMillis()));
                          return updateRiskCategorizationFailedCases(
                                  caseData.getLoanApplicationId(), SUCCESS)
                              .thenReturn(
                                  riskResponseBuilder(
                                      caseData,
                                      ResponseStatus.SUCCESS,
                                      RISK_RETRY_PROCESSED_SUCCESSFULLY));
                        })
                    .switchIfEmpty(
                        Mono.defer(
                            () -> {
                              caseData.setLastUpdatedAt(
                                  convertEpochMilliToIst(System.currentTimeMillis()));
                              return updateRiskCategorizationFailedCases(
                                      caseData.getLoanApplicationId(), SUCCESS)
                                  .thenReturn(
                                      riskResponseBuilder(
                                          caseData,
                                          ResponseStatus.SUCCESS,
                                          RISK_RETRY_PROCESSED_SUCCESSFULLY));
                            }))
                    .onErrorResume(
                        ex -> {
                          caseData.setLastUpdatedAt(
                              convertEpochMilliToIst(System.currentTimeMillis()));
                          log.error(
                              RISK_CATEGORIZATION_RETRY.concat(
                                  "[ERROR] Error during Trigger Risk" + " Process: {}"),
                              ex.getMessage());
                          return updateRiskCategorizationFailedCases(
                                  caseData.getLoanApplicationId(), FAIL)
                              .thenReturn(
                                  riskResponseBuilder(
                                      caseData,
                                      ResponseStatus.FAIL,
                                      RISK_CATEGORIZATION_RETRY_FAILED));
                        }))
        .switchIfEmpty(
            Flux.just(
                riskResponseBuilder(
                    null, ResponseStatus.SUCCESS, NO_FAILED_CASE_FOR_RISK_CATEGORIZATION_FOUND)))
        .onErrorResume(
            error -> {
              log.error(
                  RISK_CATEGORIZATION_RETRY.concat(
                      "[ERROR] Error during retry risk categorization" + " failure case: {}"),
                  error.getMessage());

              return Mono.error(
                  new BaseException(
                      RISK_CATEGORIZATION_RETRY_FAILED, null, HttpStatus.INTERNAL_SERVER_ERROR));
            });
  }

  /**
   * Generates Entity Data FAILED/SUCCESS as per Data Table for Risk Categorization Failure to be
   * saved in DB.
   *
   * @param loanApplicationId: Loan Application ID.
   * @param status: FAIL/SUCCESS, Status to be saved in DB for datatable.
   * @return: A mono representing Data Entity creation for RiskCategorizationFailureEntity.
   */
  public Mono<RiskCategorizationFailureEntity> updateRiskCategorizationFailedCases(
      String loanApplicationId, String status) {
    return riskCategorizationFailureRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            entity -> {
              entity.setStatus(status);
              entity.setLastUpdatedAt(convertEpochMilliToIst(System.currentTimeMillis()));

              log.info(
                  "[RISK CATEGORIZATION][UPDATING] Updating risk categorization case {}",
                  loanApplicationId);
              return riskCategorizationFailureRepository.save(entity);
            })
        .switchIfEmpty(Mono.error(new RuntimeException(RISK_LOAN_APPLICATION_NOT_FOUND)));
  }

  /**
   * Creates ResponseDTO for Risk Categorization.
   *
   * @param caseData:Data of Case processed.
   * @param responseStatus: Case processing status, SUCCESS/FAIL.
   * @return: ResponseDTO containing RiskCategorizationFailureEntity.
   */
  private ResponseDTO<RiskCategorizationFailureEntity> riskResponseBuilder(
      RiskCategorizationFailureEntity caseData, ResponseStatus responseStatus, String message) {
    ResponseDTO<RiskCategorizationFailureEntity> response =
        new ResponseDTO<>(responseStatus, message, "", caseData);
    log.info(RISK_CATEGORIZATION_RETRY.concat("[RESPONSE]:{}"), response);
    return response;
  }

  public Mono<M2pGetKycStatusResponseDTO> getKycStatus(String loanId) {

    if (!loanId.matches("\\d+")) {
      return Mono.error(
          new BaseException("Invalid loan ID: must be numeric.", loanId, HttpStatus.BAD_REQUEST));
    }
    return m2PWrapperApi
        .getKycStatus(loanId)
        .flatMap(
            responseDTO -> {
              try {
                boolean hasAadhaarChecks =
                    responseDTO.getAadhaarXmlFaceMatchStatus() != null
                        || responseDTO.getAadhaarXmlNameMatchStatus() != null
                        || responseDTO.getAadhaarXmlValidityStatus() != null;

                boolean hasOkycChecks =
                    responseDTO.getOkycFaceMatchStatus() != null
                        || responseDTO.getOkycNameMatchStatus() != null;

                if (!hasAadhaarChecks && !hasOkycChecks) {
                  return Mono.empty();
                }
                List<String> failureReasons = new ArrayList<>();
                if (hasAadhaarChecks
                    && (responseDTO.getAadhaarXmlFaceMatchStatus() == null
                        || responseDTO.getAadhaarXmlNameMatchStatus() == null
                        || responseDTO.getAadhaarXmlValidityStatus() == null)) {
                  log.info("[KYC_STATUS] All the response fields are not present");
                  return Mono.error(
                      new BaseException("No Record Found", null, HttpStatus.NOT_FOUND));
                }
                if (hasOkycChecks
                    && (responseDTO.getOkycFaceMatchStatus() == null
                        || responseDTO.getOkycNameMatchStatus() == null)) {
                  log.info("[OKYC_STATUS] All the response fields are not present");
                  return Mono.error(
                      new BaseException("No Record Found", null, HttpStatus.NOT_FOUND));
                }
                boolean verified = true;
                if (responseDTO.getAadhaarXmlFaceMatchStatus() != null
                    || responseDTO.getAadhaarXmlNameMatchStatus() != null
                    || responseDTO.getAadhaarXmlValidityStatus() != null) {
                  if (!VERIFIED.equalsIgnoreCase(responseDTO.getAadhaarXmlFaceMatchStatus())) {
                    verified = false;
                    failureReasons.add(
                        responseDTO.getAadhaarXmlFaceMatchError() != null
                            ? responseDTO.getAadhaarXmlFaceMatchError()
                            : "");
                  }
                  if (!VERIFIED.equalsIgnoreCase(responseDTO.getAadhaarXmlNameMatchStatus())) {
                    verified = false;
                    failureReasons.add(
                        responseDTO.getAadhaarXmlNameMatchError() != null
                            ? responseDTO.getAadhaarXmlNameMatchError()
                            : "");
                  }
                  if (!VERIFIED.equalsIgnoreCase(responseDTO.getAadhaarXmlValidityStatus())) {
                    verified = false;
                    failureReasons.add(
                        responseDTO.getAadhaarXmlValidityError() != null
                            ? responseDTO.getAadhaarXmlValidityError()
                            : "");
                  }
                  responseDTO.setKycStatus(verified ? VERIFIED : REJECTED);
                }

                if (responseDTO.getOkycFaceMatchStatus() != null
                    || responseDTO.getOkycNameMatchStatus() != null) {
                  if (!VERIFIED.equalsIgnoreCase(responseDTO.getOkycFaceMatchStatus())) {
                    verified = false;
                    failureReasons.add(
                        responseDTO.getOkycFaceMatchStatusError() != null
                            ? responseDTO.getOkycFaceMatchStatusError()
                            : "");
                  }
                  if (!VERIFIED.equalsIgnoreCase(responseDTO.getOkycNameMatchStatus())) {
                    verified = false;
                    failureReasons.add(
                        responseDTO.getOkycNameMatchError() != null
                            ? responseDTO.getOkycNameMatchError()
                            : "");
                  }
                }

                M2pGetKycStatusResponseDTO kycResponse =
                    M2pGetKycStatusResponseDTO.builder()
                        .kycStatus(verified ? VERIFIED : REJECTED)
                        .okycFaceMatch(responseDTO.getOkycFaceMatch())
                        .okycFaceMatchStatus(responseDTO.getOkycFaceMatchStatus())
                        .okycNameMatch(responseDTO.getOkycNameMatch())
                        .okycNameMatchScore(responseDTO.getOkycNameMatchScore())
                        .okycNameMatchStatus(responseDTO.getOkycNameMatchStatus())
                        .aadhaarXmlValidityCheck(responseDTO.getAadhaarXmlValidityCheck())
                        .aadhaarXmlValidityStatus(responseDTO.getAadhaarXmlValidityStatus())
                        .aadhaarXmlFaceMatch(responseDTO.getAadhaarXmlFaceMatch())
                        .aadhaarXmlFaceMatchStatus(responseDTO.getAadhaarXmlFaceMatchStatus())
                        .aadhaarXmlNameMatch(responseDTO.getAadhaarXmlNameMatch())
                        .aadhaarXmlNameMatchScore(responseDTO.getAadhaarXmlNameMatchScore())
                        .aadhaarXmlNameMatchStatus(responseDTO.getAadhaarXmlNameMatchStatus())
                        .amlStatus(normalizeAmlPepStatus(responseDTO.getAmlStatus()))
                        .loanApplicationId(responseDTO.getLoanApplicationId())
                        .clientId(responseDTO.getClientId())
                        .pepMatch(normalizeAmlPepStatus(responseDTO.getPepMatch()))
                        .kycFailureReason(verified ? null : failureReasons)
                        .build();
                return amlPepResultsRepository
                    .findByLeadId(loanId)
                    .map(
                        amlPepResult -> {
                          kycResponse.setAmlStatus(
                              normalizeAmlPepStatus(amlPepResult.getAmlDecision()));
                          kycResponse.setPepMatch(
                              normalizeAmlPepStatus(amlPepResult.getPepDecision()));
                          List<String> amlPepFailureReasons =
                              getAmlPepPolicyFailureReasons(
                                  kycResponse.getAmlStatus(), kycResponse.getPepMatch());
                          if (!amlPepFailureReasons.isEmpty()) {
                            List<String> mergedFailureReasons = new ArrayList<>();
                            if (kycResponse.getKycFailureReason() != null) {
                              mergedFailureReasons.addAll(kycResponse.getKycFailureReason());
                            }
                            mergedFailureReasons.addAll(amlPepFailureReasons);
                            kycResponse.setKycFailureReason(mergedFailureReasons);
                          }
                          if (isAmlPepRejected(
                              kycResponse.getAmlStatus(), kycResponse.getPepMatch())) {
                            kycResponse.setKycStatus(REJECTED);
                          }
                          return kycResponse;
                        })
                    .switchIfEmpty(
                        Mono.defer(
                            () -> {
                              if (isAmlPepRejected(
                                  kycResponse.getAmlStatus(), kycResponse.getPepMatch())) {
                                kycResponse.setKycStatus(REJECTED);
                              }
                              return Mono.just(kycResponse);
                            }));
              } catch (IllegalArgumentException e) {
                return Mono.error(new IllegalArgumentException("Invalid KYC data", e));
              }
            })
        .switchIfEmpty(
            Mono.error(new BaseException("No Record Found", null, HttpStatus.NOT_FOUND)));
  }

  private String normalizeAmlPepStatus(String status) {
    if (status == null || status.isBlank()) {
      return CANNOT_BE_DONE;
    }

    return switch (status.trim().toUpperCase()) {
      case VERIFIED, PASS, "MANUAL REVIEW" -> VERIFIED;
      case "REJECTED", FAIL, "REJECT" -> "REJECTED";
      case CANNOT_BE_DONE -> CANNOT_BE_DONE;
      default -> CANNOT_BE_DONE;
    };
  }

  private boolean isAmlPepRejected(String amlStatus, String pepMatch) {
    return "REJECTED".equalsIgnoreCase(normalizeAmlPepStatus(amlStatus))
        || "REJECTED".equalsIgnoreCase(normalizeAmlPepStatus(pepMatch));
  }

  private List<String> getAmlPepPolicyFailureReasons(String amlStatus, String pepMatch) {
    List<String> failureReasons = new ArrayList<>();
    String normalizedPepStatus = normalizeAmlPepStatus(pepMatch);
    String normalizedAmlStatus = normalizeAmlPepStatus(amlStatus);

    // Precedence: if PEP is rejected, return only PEP failure reason.
    if ("REJECTED".equalsIgnoreCase(normalizedPepStatus)) {
      failureReasons.add("Application not acceptable as per PEP Policy");
      return failureReasons;
    }

    // AML reason is applicable only when PEP is not rejected.
    if ("REJECTED".equalsIgnoreCase(normalizedAmlStatus)) {
      failureReasons.add("Application not acceptable as per AML Policy");
    }
    return failureReasons;
  }

  public Mono<Object> approveLoanAndTriggerCta(String loanId, String approvedDate) {
    LocalDate today = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(approvedDate, formatter);
    } catch (Exception e) {
      return Mono.error(
          new IllegalArgumentException("[LOAN_APPROVAL_REQUEST] invalid date format"));
    }
    if (parsedDate.isBefore(today)) {
      return Mono.error(new IllegalArgumentException("approved date cannot be a past date"));
    }
    return approveLoanWithValidation(loanId, approvedDate)
        .onErrorResume(
            error -> {
              if (isAlreadyApprovedError(error)) {
                return m2PWrapperApi
                    .getLoanApprovalDetails(loanId)
                    .map(
                        approvalDetails -> {
                          Map<String, Object> response = new HashMap<>();
                          response.put("officeId", approvalDetails.getOfficeId());
                          response.put("clientId", approvalDetails.getClientId());
                          response.put("resourceId", Integer.valueOf(loanId));
                          Map<String, Object> changes = new HashMap<>();
                          changes.put("statusEnum", approvalDetails.getStatusEnum());
                          changes.put("remarks", "");
                          response.put("changes", changes);
                          response.put("rollbackTransaction", false);
                          return response;
                        });
              }
              return Mono.error(error);
            })
        .flatMap(
            approveApplicationResponse ->
                m2PWrapperApi
                    .registerCta(loanId, "freeze-loan-application")
                    .onErrorResume(
                        ctaErr -> {
                          log.error(
                              "[APPROVE_LOAN_CTA] [ERROR] CTA registration failed for loanId {}:"
                                  + " {}",
                              loanId,
                              ctaErr.getMessage());
                          return Mono.just(approveApplicationResponse);
                        })
                    .thenReturn(approveApplicationResponse));
  }

  private boolean isAlreadyApprovedError(Throwable error) {
    if (!(error instanceof ClientSideException clientResponse)) {
      return false;
    }

    if (clientResponse.getHttpStatusCode() != HttpStatus.FORBIDDEN) {
      return false;
    }

    Object responseBody = clientResponse.getResponseBody();
    if (!(responseBody instanceof Map<?, ?> responseMap)) {
      return false;
    }

    Object errorsObj = responseMap.get("errors");
    if (!(errorsObj instanceof List<?> errorsList)) {
      return false;
    }

    return errorsList.stream()
        .filter(Map.class::isInstance)
        .map(err -> ((Map<String, Object>) err).get("developerMessage"))
        .filter(Objects::nonNull)
        .map(Object::toString)
        .anyMatch(
            msg ->
                msg.contains("APPLICATION_APPROVED status")
                    || msg.contains("Action :APPROVE cannot be performed"));
  }

  public Mono<GetDocketDetailsResponseDto> getLoanAndClientDetailsForDocketPopulationByLoanId(
      String loanId) {
    return m2PWrapperApi.getLoanAndClientDetailsForDocketPopulationByLoanId(loanId);
  }

  public Mono<Boolean> getPanAadhaarLinkage(String productCode, String loanId) {
    return panAadhaarLinkageRepository
        .findByloanId(loanId)
        .flatMap(
            panAadhaarData -> {
              boolean panAadhaarActive =
                  Boolean.parseBoolean(environment.getProperty("pan-aadhaar-linkage"));
              log.info(PAN_AADHAAR_LINKAGE_HEADER.concat(" feature flag is: {}"), panAadhaarActive);
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlConfigData -> {
                        String callbackIdentifier =
                            AadhaarXMLType.OKYC.getDisplayName().equals(panAadhaarData.getKycType())
                                ? OKYC_CALLBACK_IDENTIFIER
                                : KYC_CALLBACK_IDENTIFIER;
                        log.info(
                            PAN_AADHAAR_LINKAGE_HEADER.concat(" KYC identifier used is: {}"),
                            callbackIdentifier);
                        ProductControl.Flow flowData =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControlConfigData.getT2(), callbackIdentifier);
                        if (Objects.isNull(flowData)) {
                          return Mono.error(
                              new BaseException(
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                        log.info(
                            PAN_AADHAAR_LINKAGE_HEADER.concat(" product-feature flag is: {}"),
                            flowData.isAadhaarPanLinkEnforce());
                        if (panAadhaarActive
                            && flowData.isAadhaarPanLinkEnforce()
                            && panAadhaarData.getProductCode().equals(productCode)) {
                          log.info(
                              PAN_AADHAAR_LINKAGE_HEADER.concat(" linked status: {}"),
                              panAadhaarData.getLinked());
                          // String check has been implemented as getting "null" value
                          if ((panAadhaarData.getLinked() != null
                                  && !panAadhaarData.getLinked().equals("null"))
                              && !Boolean.parseBoolean(panAadhaarData.getLinked())) {
                            log.warn(
                                PAN_AADHAAR_LINKAGE_HEADER.concat(
                                    "pan-aadhaar is not linked for loan id: {}"),
                                loanId);
                            return Mono.error(
                                new BaseException(
                                    PAN_AADHAAR_NOT_LINKED,
                                    PAN_AADHAAR_NOT_LINKED,
                                    HttpStatus.PRECONDITION_FAILED));
                          }
                          log.info(
                              PAN_AADHAAR_LINKAGE_HEADER.concat(
                                  "pan-aadhaar is linked for loan id: {}"),
                              loanId);
                          return Mono.just(Boolean.TRUE);
                        }
                        return Mono.just(Boolean.TRUE);
                      });
            })
        .switchIfEmpty(Mono.just(Boolean.TRUE))
        .onErrorResume(
            err -> {
              log.error(PAN_AADHAAR_LINKAGE_HEADER.concat(" error while executing check: {}"), err);
              return Mono.error(err);
            });
  }

  /**
   * Same as {@link #getPanAadhaarLinkage(String, String)} but returns client ID along with the
   * linkage boolean. When no pan-aadhaar record exists for the loan, returns clientId=null and
   * linked=true (check skipped).
   */
  public Mono<PanAadhaarLinkageResult> getPanAadhaarLinkageWithClientId(
      String productCode, String loanId) {
    return resolvePanAadhaarLinkageWithClientId(productCode, loanId, true);
  }

  /**
   * Pre-activation / limit QC: if there is no row in the pan-aadhaar linkage table for this loan,
   * returns {@code linked=false} so QC fails. Differs from {@link
   * #getPanAadhaarLinkageWithClientId(String, String)} which treats a missing row as linked (skip).
   */
  public Mono<PanAadhaarLinkageResult> getPanAadhaarLinkageWithClientIdForPreActivationQc(
      String productCode, String loanId) {
    return resolvePanAadhaarLinkageWithClientId(productCode, loanId, true);
  }

  private Mono<PanAadhaarLinkageResult> resolvePanAadhaarLinkageWithClientId(
      String productCode, String loanId, boolean linkedWhenNoLinkageRecord) {
    return panAadhaarLinkageRepository
        .findByloanId(loanId)
        .flatMap(
            panAadhaarData -> {
              boolean panAadhaarActive =
                  Boolean.parseBoolean(environment.getProperty("pan-aadhaar-linkage"));
              log.info(PAN_AADHAAR_LINKAGE_HEADER.concat(" feature flag is: {}"), panAadhaarActive);
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlConfigData -> {
                        String callbackIdentifier =
                            AadhaarXMLType.OKYC.getDisplayName().equals(panAadhaarData.getKycType())
                                ? OKYC_CALLBACK_IDENTIFIER
                                : KYC_CALLBACK_IDENTIFIER;
                        log.info(
                            PAN_AADHAAR_LINKAGE_HEADER.concat(" KYC identifier used is: {}"),
                            callbackIdentifier);
                        ProductControl.Flow flowData =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControlConfigData.getT2(), callbackIdentifier);
                        if (Objects.isNull(flowData)) {
                          return Mono.error(
                              new BaseException(
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                        log.info(
                            PAN_AADHAAR_LINKAGE_HEADER.concat(" product-feature flag is: {}"),
                            flowData.isAadhaarPanLinkEnforce());
                        if (panAadhaarActive
                            && flowData.isAadhaarPanLinkEnforce()
                            && panAadhaarData.getProductCode().equals(productCode)) {
                          log.info(
                              PAN_AADHAAR_LINKAGE_HEADER.concat(" linked status: {}"),
                              panAadhaarData.getLinked());
                          // String check has been implemented as getting "null" value
                          if ((panAadhaarData.getLinked() != null
                                  && !panAadhaarData.getLinked().equals("null"))
                              && !Boolean.parseBoolean(panAadhaarData.getLinked())) {
                            log.warn(
                                PAN_AADHAAR_LINKAGE_HEADER.concat(
                                    "pan-aadhaar is not linked for loan id: {}"),
                                loanId);
                            return Mono.error(
                                new BaseException(
                                    PAN_AADHAAR_NOT_LINKED,
                                    PAN_AADHAAR_NOT_LINKED,
                                    HttpStatus.PRECONDITION_FAILED));
                          }
                          log.info(
                              PAN_AADHAAR_LINKAGE_HEADER.concat(
                                  "pan-aadhaar is linked for loan id: {}"),
                              loanId);
                          return Mono.just(
                              new PanAadhaarLinkageResult(
                                  panAadhaarData.getClientId(), Boolean.TRUE));
                        }
                        return Mono.just(
                            new PanAadhaarLinkageResult(
                                panAadhaarData.getClientId(), Boolean.TRUE));
                      });
            })
        .switchIfEmpty(Mono.just(new PanAadhaarLinkageResult(null, linkedWhenNoLinkageRecord)));
  }

  public Mono<M2pAddBankDetailsResponseDTO> attachBankAccountProductWise(
      String loanId,
      String leadId,
      @Valid AttachBankDetailsDTO attachBankDetailsDTO,
      String productCode) {
    return bankVerificationService.attachBankAccountProductWise(
        loanId, leadId, attachBankDetailsDTO, productCode);
  }

  public Mono<Boolean> getPanAadhaarLinkageStatus(String productCode, String loanId) {
    return panAadhaarLinkageRepository
        .findByloanId(loanId)
        .flatMap(
            panAadhaarData -> {
              if (!productCode.equals(panAadhaarData.getProductCode())) {
                log.info(
                    PAN_AADHAAR_LINKAGE_AUTO_DISB_STATUS.concat(
                        " product code mismatch for loan application id: {}"),
                    loanId);
                return Mono.just(Boolean.FALSE);
              }
              Boolean linked =
                  panAadhaarData.getLinked() != null
                      ? Boolean.parseBoolean(panAadhaarData.getLinked())
                      : Boolean.FALSE;
              log.info(
                  PAN_AADHAAR_LINKAGE_AUTO_DISB_STATUS.concat(
                      " linked status for loan application id {} : {}"),
                  loanId,
                  linked);
              return Mono.just(linked);
            })
        .switchIfEmpty(
            Mono.fromSupplier(
                () -> {
                  log.info(
                      PAN_AADHAAR_LINKAGE_AUTO_DISB_STATUS.concat(
                          " no pan-aadhaar entry found for loan application id: {}"),
                      loanId);
                  return Boolean.FALSE;
                }))
        .onErrorResume(
            err -> {
              log.error(
                  PAN_AADHAAR_LINKAGE_AUTO_DISB_STATUS.concat(
                      " error while checking pan-aadhaar linkage for loan application id: {}"),
                  loanId,
                  err);
              return Mono.just(Boolean.FALSE);
            });
  }

  public Mono<Boolean> getNsdlPanValidationStatus(String productCode, String clientId) {
    return validationFunnelService
        .findLatestClientValidationFunnelStatusByClientIdAndProductCode(clientId, productCode)
        .map(
            entity -> {
              log.info(
                  "[AUTO_DISBURSAL_VALIDATIONS] [NSDL_PAN] nsdl pan entity received for clientId:"
                      + " {}, productCode: {}, entity: {}",
                  clientId,
                  productCode,
                  gson.toJson(entity));
              boolean isPass = "PASS".equalsIgnoreCase(entity.getFinalStatus());
              log.info(
                  "[AUTO_DISBURSAL_VALIDATIONS] [NSDL_PAN] clientId: {}, productCode: {},"
                      + " result: {}, finalStatus: {}",
                  clientId,
                  productCode,
                  isPass,
                  entity.getFinalStatus());
              return isPass;
            })
        .switchIfEmpty(
            Mono.fromSupplier(
                () -> {
                  log.info(
                      "[AUTO_DISBURSAL_VALIDATIONS] [NSDL_PAN] clientId: {}, productCode: {},"
                          + " result: false, reason: entry not found",
                      clientId,
                      productCode);
                  return Boolean.FALSE;
                }))
        .onErrorResume(
            error -> {
              log.error(
                  "[AUTO_DISBURSAL_VALIDATIONS] [NSDL_PAN] clientId: {}, productCode: {},"
                      + " result: false, reason: error",
                  clientId,
                  productCode,
                  error);
              return Mono.just(Boolean.FALSE);
            });
  }

  public Mono<Boolean> getPennyDropStatus(String clientId, String bankId) {
    return bankVerificationService
        .getBankVerificationStatus(clientId, bankId)
        .map(
            response ->
                response.getBankVerificationStatus() == BankVerificationStatusEnum.PASS
                    && response.getNameMatchPercentage() != null
                    && response.getNameMatchPercentage() > 0.42)
        .defaultIfEmpty(Boolean.FALSE)
        .onErrorResume(
            error -> {
              log.error(
                  "[AUTO_DISB_VALIDATION] [PENNY_DROP] error while fetching penny drop status for"
                      + " client id: {}, bank id: {}",
                  clientId,
                  bankId,
                  error);
              return Mono.just(Boolean.FALSE);
            });
  }

  public Mono<Boolean> getAmlPepStatusFromM2p(String loanApplicationId) {
    return m2PWrapperApi
        .getAmlPepResult(loanApplicationId)
        .map(
            response ->
                "PASS".equalsIgnoreCase(response.getAmlDecision())
                    && "No".equalsIgnoreCase(response.getPepResult()))
        .defaultIfEmpty(Boolean.FALSE)
        .onErrorResume(
            error -> {
              log.error(
                  "[AUTO_DISB_VALIDATION] [AML_PEP] error while fetching aml/pep status from m2p"
                      + " for loan application id: {}",
                  loanApplicationId,
                  error);
              return Mono.just(Boolean.FALSE);
            });
  }

  /**
   * Saves miscellaneous details for client and/or loan application with transactional support. Both
   * client and loan application miscellaneous details are optional. This method fetches clientId
   * from loanApplicationId and saves both in a single transaction to maintain atomicity.
   *
   * @param loanApplicationId the loan application ID (required)
   * @param clientMiscellaneousDetails map of key-value pairs for client (optional)
   * @param loanApplicationMiscellaneousDetails map of key-value pairs for loan application
   *     (optional)
   * @return Mono<ResponseEntity<Map<String, String>>> success response when save is successful
   */
  @Transactional
  public Mono<ResponseEntity<Map<String, String>>> saveMiscellaneousDetails(
      String loanApplicationId,
      Map<String, String> clientMiscellaneousDetails,
      Map<String, String> loanApplicationMiscellaneousDetails) {

    log.info(
        "[SAVE_MISC_DETAILS] Saving miscellaneous details for loanApplicationId: {}",
        loanApplicationId);

    return getClientIdAndProductCode(loanApplicationId)
        .flatMap(
            tuple -> {
              Integer clientId = tuple.getT1();
              String productCode = tuple.getT2();

              log.info(
                  "[SAVE_MISC_DETAILS] Found clientId: {}, productCode: {} for loanApplicationId:"
                      + " {}",
                  clientId,
                  productCode,
                  loanApplicationId);

              Mono<Integer> partnerIdMono;
              if ("ELTO".equals(productCode)) {
                partnerIdMono = Mono.just(1001);
              } else {
                partnerIdMono =
                    partnerMasterService
                        .findByProductCode(productCode)
                        .map(partnerMaster -> Integer.valueOf(partnerMaster.getPartnerId()));
              }

              return partnerIdMono.flatMap(
                  partnerId -> {
                    log.info(
                        "[SAVE_MISC_DETAILS] Found partnerId: {} for productCode: {}",
                        partnerId,
                        productCode);

                    return saveClientMiscDetails(clientId, partnerId, clientMiscellaneousDetails)
                        .then(
                            saveLoanAppMiscDetails(
                                Integer.parseInt(loanApplicationId),
                                clientId,
                                productCode,
                                loanApplicationMiscellaneousDetails))
                        .then(
                            Mono.fromSupplier(
                                () -> {
                                  log.info(
                                      "[SAVE_MISC_DETAILS] Successfully saved miscellaneous"
                                          + " details for loanApplicationId: {}, clientId: {}",
                                      loanApplicationId,
                                      clientId);
                                  return ResponseEntity.ok(
                                      Map.of("status", ResponseStatus.SUCCESS.name()));
                                }));
                  });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[SAVE_MISC_DETAILS] Error saving miscellaneous details for loanApplicationId:"
                      + " {}, error: {}",
                  loanApplicationId,
                  error.getMessage());
              return Mono.error(error);
            });
  }

  private Mono<Tuple2<Integer, String>> getClientIdAndProductCode(String loanApplicationId) {
    return m2PWrapperApi
        .getLoanApplicationByLoanId(loanApplicationId)
        .flatMap(
            response -> {
              Map<String, Object> loanDataMap = (Map<String, Object>) response;

              Object clientIdObj = loanDataMap.get("clientId");
              if (clientIdObj == null) {
                log.error(
                    "[GET_CLIENT_AND_PRODUCT_CODE] clientId not found in M2P response for"
                        + " loanApplicationId: {}",
                    loanApplicationId);
                return Mono.error(
                    new NotFoundException(
                        "clientId not found for loanApplicationId: " + loanApplicationId));
              }

              Object productKeyObj = loanDataMap.get("losProductKey");
              if (productKeyObj == null) {
                log.error(
                    "[GET_CLIENT_AND_PRODUCT_CODE] productKey not found in M2P response for"
                        + " loanApplicationId: {}",
                    loanApplicationId);
                return Mono.error(
                    new NotFoundException(
                        "productKey not found for loanApplicationId: " + loanApplicationId));
              }

              Integer clientId = Integer.valueOf(String.valueOf(clientIdObj));
              String productCode = String.valueOf(productKeyObj);
              return Mono.just(Tuples.of(clientId, productCode));
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[GET_CLIENT_AND_PRODUCT_CODE] Could not fetch loan details for"
                          + " loanApplicationId: {}",
                      loanApplicationId);
                  return Mono.error(
                      new NotFoundException(
                          "Loan details not found for loanApplicationId: " + loanApplicationId));
                }));
  }

  private Mono<Void> saveClientMiscDetails(
      Integer clientId, Integer partnerId, Map<String, String> clientMiscellaneousDetails) {
    if (clientMiscellaneousDetails == null || clientMiscellaneousDetails.isEmpty()) {
      return Mono.empty();
    }
    return leadMiscellaneousDetailsService
        .saveMiscellaneousDetails(clientId, partnerId, clientMiscellaneousDetails)
        .then();
  }

  private Mono<Void> saveLoanAppMiscDetails(
      Integer loanApplicationId,
      Integer clientId,
      String productCode,
      Map<String, String> loanApplicationMiscellaneousDetails) {
    if (loanApplicationMiscellaneousDetails == null
        || loanApplicationMiscellaneousDetails.isEmpty()) {
      return Mono.empty();
    }
    return loanApplicationMiscellaneousDetailsService
        .saveMiscellaneousDetails(
            loanApplicationId, clientId, productCode, loanApplicationMiscellaneousDetails)
        .then();
  }

  private void publishEventKafkaAsync(Runnable eventTask) {
    Mono.fromRunnable(eventTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error while publishing the event", error));
  }

  public Mono<GetLoanLanDetailsResponse> getDetailsByLoanId(String loanId) {
    return m2PWrapperApi.getDetailsByLoanId(loanId);
  }

  public Mono<GetLoanLanDetailsResponse> getLoanApplicationByLanId(String lanId) {
    return m2PWrapperApi.getDetailsByLanId(lanId);
  }

  /** Adds {@code kycDetails.panAadhaarLinkedStatus} from {@code pan_aadhaar_linkage_details}. */
  private Mono<Object> attachKycPanAadhaarLinkage(Object response, String loanId) {
    if (!(response instanceof Map)) {
      log.warn(
          "[KYC_LINKAGE] M2P response is not a Map, skipping kycDetails for loanId={}", loanId);
      return Mono.just(response);
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) response;
    return panAadhaarLinkageRepository
        .findByloanId(loanId)
        .map(
            entity -> {
              PanAadhaarLinkedStatus status = mapPanAadhaarLinkedStatus(entity);
              putKycPanAadhaarLinkage(map, status);
              log.info(
                  "[KYC_LINKAGE] Attached panAadhaarLinkedStatus={} for loanId={}", status, loanId);
              return (Object) map;
            })
        .switchIfEmpty(
            Mono.fromSupplier(
                () -> {
                  putKycPanAadhaarLinkage(map, PanAadhaarLinkedStatus.NOT_FOUND);
                  log.info(
                      "[KYC_LINKAGE] No pan_aadhaar_linkage row for loanId={}, using NOT_FOUND",
                      loanId);
                  return (Object) map;
                }))
        .onErrorResume(
            err -> {
              log.warn(
                  "[KYC_LINKAGE] Failed to load pan_aadhaar_linkage_details for loanId={}: {}",
                  loanId,
                  err.getMessage());
              putKycPanAadhaarLinkage(map, PanAadhaarLinkedStatus.NOT_FOUND);
              return Mono.just((Object) map);
            });
  }

  private static void putKycPanAadhaarLinkage(
      Map<String, Object> loanMap, PanAadhaarLinkedStatus status) {
    Map<String, Object> kycDetails = new HashMap<>();
    kycDetails.put("panAadhaarLinkedStatus", status.name());
    loanMap.put("kycDetails", kycDetails);
  }

  private static PanAadhaarLinkedStatus mapPanAadhaarLinkedStatus(PanAadhaarLinkageEntity entity) {
    String linked = entity.getLinked();
    if (linked == null || linked.isBlank() || "null".equalsIgnoreCase(linked)) {
      return PanAadhaarLinkedStatus.NOT_FOUND;
    }
    if (Boolean.parseBoolean(linked)) {
      return PanAadhaarLinkedStatus.LINKED;
    }
    return PanAadhaarLinkedStatus.NOT_LINKED;
  }
}
