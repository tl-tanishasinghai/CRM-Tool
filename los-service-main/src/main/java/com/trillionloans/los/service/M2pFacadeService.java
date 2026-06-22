package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.AML_CHECK_CANNOT_BE_DONE;
import static com.trillionloans.los.constant.StringConstants.AML_PEP_DATE_TIME_FORMAT;
import static com.trillionloans.los.constant.StringConstants.AML_REJECTION_DESCRIPTION;
import static com.trillionloans.los.constant.StringConstants.AML_VERIFIED_DESCRIPTION;
import static com.trillionloans.los.constant.StringConstants.APPROVED;
import static com.trillionloans.los.constant.StringConstants.BRE_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.CANNOT_BE_DONE;
import static com.trillionloans.los.constant.StringConstants.CKYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.CKYC_FAIL;
import static com.trillionloans.los.constant.StringConstants.CLOSURE_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;
import static com.trillionloans.los.constant.StringConstants.DATE_TIME_FORMAT;
import static com.trillionloans.los.constant.StringConstants.DEFAULT_AML_PEP_DISABLE_DESCRIPTION;
import static com.trillionloans.los.constant.StringConstants.DISB_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.ELIGIBLE;
import static com.trillionloans.los.constant.StringConstants.EN;
import static com.trillionloans.los.constant.StringConstants.ENGLISH_PREFIX;
import static com.trillionloans.los.constant.StringConstants.E_SIGN_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.FAIL;
import static com.trillionloans.los.constant.StringConstants.FAILED;
import static com.trillionloans.los.constant.StringConstants.FEATURE_NOT_ENABLED;
import static com.trillionloans.los.constant.StringConstants.FI_STATUS_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.HIGH_RISK;
import static com.trillionloans.los.constant.StringConstants.KYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.KYC_FAIL;
import static com.trillionloans.los.constant.StringConstants.KYC_STATUS_VERIFIED;
import static com.trillionloans.los.constant.StringConstants.LOW_RISK;
import static com.trillionloans.los.constant.StringConstants.MANUAL_KYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.MANUAL_REVIEW;
import static com.trillionloans.los.constant.StringConstants.MANUAL_REVIEW_DESCRIPTION;
import static com.trillionloans.los.constant.StringConstants.MEDIUM_RISK;
import static com.trillionloans.los.constant.StringConstants.NO;
import static com.trillionloans.los.constant.StringConstants.OFFER_DOWN_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.OKYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.OKYC_FAIL;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.PASS;
import static com.trillionloans.los.constant.StringConstants.PEP_REJECTION_DESCRIPTION;
import static com.trillionloans.los.constant.StringConstants.RED;
import static com.trillionloans.los.constant.StringConstants.REJECT;
import static com.trillionloans.los.constant.StringConstants.REJECTION_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.RISK_CATEGORIZATION_RETRY;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.VCIP_LINK_NOTIFICATION;
import static com.trillionloans.los.constant.StringConstants.VERIFIED;
import static com.trillionloans.los.constant.StringConstants.YES;
import static com.trillionloans.los.util.DateTimeConverterUtil.convertEpochMilliToIst;
import static com.trillionloans.los.util.JsonUtils.extractFieldValue;
import static com.trillionloans.los.util.LoanDataUtil.CREDIT_LINE_KCL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trillionloans.los.api.partner.DocSignServiceApi;
import com.trillionloans.los.api.partner.KarzaApi;
import com.trillionloans.los.api.partner.KycAdaptorApi;
import com.trillionloans.los.api.partner.LspApi;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.NexusApi;
import com.trillionloans.los.api.partner.PartnerApi;
import com.trillionloans.los.config.AmlPepConfig;
import com.trillionloans.los.config.RejectionReasonCodeFactory;
import com.trillionloans.los.config.RiskCodeConfig;
import com.trillionloans.los.constant.AadhaarXMLType;
import com.trillionloans.los.constant.BreType;
import com.trillionloans.los.constant.DocumentType;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.mapper.AmlPepFinalStatus;
import com.trillionloans.los.model.ClientCacheDTO;
import com.trillionloans.los.model.PanAadhaarLinkStatusDataTableDTO;
import com.trillionloans.los.model.dto.AmlPepDatatableDTO;
import com.trillionloans.los.model.dto.GetDocketDetailsResponseDto;
import com.trillionloans.los.model.dto.RiskDetailsDataTableDTO;
import com.trillionloans.los.model.dto.internal.AmlPepDecisionResult;
import com.trillionloans.los.model.dto.internal.DocDetailRequest;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.entity.LoanAccountPartnerEntity;
import com.trillionloans.los.model.entity.PanAadhaarLinkageEntity;
import com.trillionloans.los.model.entity.PartnerMasterEntity;
import com.trillionloans.los.model.entity.RiskCategorizationFailureEntity;
import com.trillionloans.los.model.entity.ValidationFunnelVerificationResultCallbackLog;
import com.trillionloans.los.model.request.ClientValidationFunnelStatus;
import com.trillionloans.los.model.request.ClientValidationServiceKYCCallbackResponse;
import com.trillionloans.los.model.request.KarazaPanAadhaarLinkCheckDTO;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.m2p.CreditLineStatusCallbackRequest;
import com.trillionloans.los.model.request.m2p.M2pCkycrCallbackRequest;
import com.trillionloans.los.model.request.m2p.M2pDisbursementCallBackRequest;
import com.trillionloans.los.model.request.m2p.M2pKycCallBackWithAmlRequest;
import com.trillionloans.los.model.request.m2p.M2pLoanApprovalCallBackRequest;
import com.trillionloans.los.model.request.m2p.M2pLoanClosureCallBackRequest;
import com.trillionloans.los.model.request.m2p.M2pRiskCategorisationCallRequest;
import com.trillionloans.los.model.response.LoanInfoDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanRejectResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pPlatformHealthResponse;
import com.trillionloans.los.model.response.m2p.NexusColorCodeResponse;
import com.trillionloans.los.repository.AadhaarReferenceIdRepository;
import com.trillionloans.los.repository.AmlPepResultsRepository;
import com.trillionloans.los.repository.LoanClientPartnerMapRepository;
import com.trillionloans.los.repository.PanAadhaarLinkageRepository;
import com.trillionloans.los.repository.RiskCategorizationFailureRepository;
import com.trillionloans.los.service.db.BreStatusService;
import com.trillionloans.los.service.db.CallbackStoreService;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.validationservice.ValidationFunnelConfigService;
import com.trillionloans.los.service.validationservice.ValidationFunnelServiceUtil;
import com.trillionloans.los.util.DateTimeConverterUtil;
import io.r2dbc.postgresql.codec.Json;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
@Slf4j
public class M2pFacadeService {

  private static final AmlPepConfig DEFAULT_AML_PEP_CONFIG;

  static {
    DEFAULT_AML_PEP_CONFIG = new AmlPepConfig();
    DEFAULT_AML_PEP_CONFIG.setAmlPepFeatureFlag(false);
    DEFAULT_AML_PEP_CONFIG.setPepCheckEnabled(true);
    DEFAULT_AML_PEP_CONFIG.setAmlCheckEnabled(true);
    DEFAULT_AML_PEP_CONFIG.setAmlRejectionThreshold(90.0);
    DEFAULT_AML_PEP_CONFIG.setAmlManualVerificationThreshold(60.0);
  }

  private final M2PWrapperApi m2PWrapperApi;
  private final PartnerApi partnerApi;
  private final KarzaApi karzaApi;
  private final CallbackStoreService callbackStoreService;
  private final ProductConfigMasterService productConfigMasterService;
  private final BreStatusService breStatusService;
  private final NexusApi nexusApi;
  private final Gson gson;
  private final LeadService leadService;
  private static final String REJECTED_CAPS = "REJECTED";
  private final RiskCategorizationFailureRepository riskCategorizationFailureRepository;
  private final RejectionReasonCodeFactory reasonCodeFactory;
  private final PartnerMasterService partnerMasterService;
  private final KafkaEventProducerService eventProducerService;
  private final RiskParameterService riskParameterService;
  private final DocSignServiceApi docSignServiceApi;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final PanAadhaarLinkageRepository panAadhaarLinkageRepository;
  private final PanAadhaarLinkageService panAadhaarLinkageService;
  private final AmlPepValidationService amlPepValidationService;
  private final AmlPepResultsRepository amlPepResultsRepository;
  private final AadhaarReferenceIdRepository aadhaarReferenceIdRepository;
  private final InsuranceService insuranceService;
  private final ValidationFunnelServiceUtil validationFunnelServiceUtil;
  private final ValidationFunnelConfigService validationFunnelConfigService;
  private final KycAdaptorApi kycAdaptorApi;
  private final LspApi lspApi;
  private final RiskCodeConfig riskCodeConfig;
  private final LoanTaggingService loanTaggingService;
  private final BusinessLoanEvaluationService businessLoanEvaluationService;
  private final LoanClientLookupService loanClientLookupService;
  private final LoanClientPartnerMapRepository loanClientPartnerMapRepository;
  private final LoanApplicationCacheService loanApplicationCacheService;
  private final ObjectProvider<CreditLineService> creditLineServiceProvider;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  @Value("${doc-sign-service.loanDataSave.check:false}")
  private boolean loanDataSaveCheck;

  @Value("${risk.categorisation.check:false}")
  private Boolean riskCategorisationCheck;

  @Value("${aadhar.xml.validity}")
  private int aadharXmlValidity;

  private Mono<M2pKycCallBackWithAmlRequest> enrichKycCallbackWithAmlPep(
      M2pKycCallBackWithAmlRequest requestBody,
      String clientId,
      AmlPepConfig amlPepConfig,
      String productCode) {

    return amlPepValidationService
        .fetchAmlPepResult(
            ClientCacheDTO.builder()
                .clientId(Integer.parseInt(clientId))
                .productCode(requestBody.getProductCode())
                .build(),
            requestBody.getLoanApplicationId(),
            amlPepConfig)
        .map(
            details -> {
              String pepMatch = details.getPepMatch();
              Double amlScore = details.getAmlFuzzyMatchScore();
              String serviceStatus = details.getServiceStatus();
              String amlStatus = details.getAmlStatus();

              String finalServiceStatus = serviceStatus != null ? serviceStatus : FAILED;

              // STEP 1 — NULL FIELD CHECK
              if (Objects.isNull(pepMatch) || Objects.isNull(amlScore)) {

                requestBody.setPepMatch(CANNOT_BE_DONE);
                requestBody.setAmlStatus(CANNOT_BE_DONE);
                requestBody.setAmlBestMatchScore("0");
                return requestBody;
              }

              // STEP 2 — SERVICE FAILED CHECK
              if (FAILED.equalsIgnoreCase(finalServiceStatus)) {

                requestBody.setPepMatch(CANNOT_BE_DONE);
                requestBody.setAmlStatus(CANNOT_BE_DONE);
                requestBody.setAmlBestMatchScore("0");
                return requestBody;
              }

              // STEP 3 — PEP MAPPING
              if ("PASS".equalsIgnoreCase(pepMatch)) {
                requestBody.setPepMatch("No");
              } else if ("FAIL".equalsIgnoreCase(pepMatch)) {
                requestBody.setPepMatch("Yes");
              } else {
                requestBody.setPepMatch(CANNOT_BE_DONE);
              }

              // AML SCORE
              requestBody.setAmlBestMatchScore(amlScore != null ? String.valueOf(amlScore) : "0");

              // AML STATUS
              // Digio path may not always populate amlStatus; derive it from score + thresholds.
              String finalAmlStatus =
                  (amlStatus != null && !amlStatus.isBlank())
                      ? amlStatus
                      : deriveAmlStatusFromScore(amlScore, amlPepConfig);
              requestBody.setAmlStatus(finalAmlStatus != null ? finalAmlStatus : CANNOT_BE_DONE);

              return requestBody;
            });
  }

  private String deriveAmlStatusFromScore(Double amlScore, AmlPepConfig amlPepConfig) {
    if (amlScore == null
        || amlScore.isNaN()
        || amlScore.isInfinite()
        || amlScore < 0
        || amlScore > 100) {
      return CANNOT_BE_DONE;
    }

    if (amlPepConfig == null) {
      return CANNOT_BE_DONE;
    }

    Double rejectionThreshold = amlPepConfig.getAmlRejectionThreshold();
    if (rejectionThreshold != null && amlScore >= rejectionThreshold) {
      return REJECT;
    }

    Double manualThreshold = amlPepConfig.getAmlManualVerificationThreshold();
    if (manualThreshold != null && amlScore >= manualThreshold) {
      return MANUAL_REVIEW;
    }

    return VERIFIED;
  }

  Mono<String> findClientIdFromLoanId(String loanId, String reason) {
    return m2PWrapperApi
        .getLoanApplicationByLoanIdV2(loanId, reason)
        .flatMap(
            response -> {
              if (Objects.nonNull(response) && Objects.nonNull(response.getClientId())) {
                return Mono.just(response.getClientId().toString());
              }
              return Mono.error(new IllegalStateException("clientId not found in response"));
            });
  }

  public Mono<LoanInfoDTO> findClientIdAndOrigReqAmountFromLoanId(String loanId) {
    return m2PWrapperApi
        .getLoanApplicationByLoanId(loanId)
        .flatMap(
            response -> {
              // Convert the generic response Object to LoanInfoDTO via Gson
              LoanInfoDTO dto = gson.fromJson(gson.toJson(response), LoanInfoDTO.class);

              if (dto != null
                  && dto.getClientId() != null
                  && dto.getLoanAmountRequested() != null) {
                return Mono.just(dto);
              }

              return Mono.error(new IllegalStateException("Required fields not found in response"));
            });
  }

  public Mono<?> registerDisbursementStatus(M2pDisbursementCallBackRequest requestBody) {
    return triggerProductControlFlow(
            requestBody, requestBody.getProductCode(), DISB_CALLBACK_IDENTIFIER, false)
        .doOnEach(
            signal -> {
              if ((signal.isOnNext() || signal.isOnError()) && riskCategorisationCheck) {
                signal
                    .getContextView()
                    .<String>getOrEmpty(TRACE_ID)
                    .ifPresent(
                        traceId -> {
                          String loanAppId = String.valueOf(requestBody.getLoanApplicationId());
                          String partnerId = signal.getContextView().get(PARTNER_ID);

                          triggerRiskProcess(loanAppId)
                              .subscribeOn(Schedulers.parallel())
                              .contextWrite(
                                  ctx -> ctx.put(TRACE_ID, traceId).put(PARTNER_ID, partnerId))
                              .subscribe();
                        });
              }
            });
  }

  public Mono<?> triggerRiskProcess(String loanApplicationId) {
    return m2PWrapperApi
        .getRiskDetailsAgainstLoanId(loanApplicationId)
        .switchIfEmpty(
            Mono.defer(
                () ->
                    loanClientLookupService
                        .getClientIdForLoan(
                            loanApplicationId, Event.RISK_DETAILS_UPLOAD.getDefaultEventType())
                        .flatMap(
                            clientId -> {
                              RiskDetailsDataTableDTO riskDetailsDataTableDTO =
                                  getRiskDetailDTO(
                                      String.valueOf(clientId), HIGH_RISK, loanApplicationId);
                              return m2PWrapperApi
                                  .uploadRiskAgainstLead(
                                      riskDetailsDataTableDTO, String.valueOf(clientId))
                                  .flatMap(uploadResponse -> Mono.empty());
                            })))
        .flatMap(
            response ->
                nexusApi
                    .getColorCodeFromNexus(response.getPostalCode(), loanApplicationId)
                    .switchIfEmpty(
                        Mono.defer(
                            () -> {
                              RiskDetailsDataTableDTO riskDetailsDataTableDTO =
                                  getRiskDetailDTO(
                                      String.valueOf(response.getClientId()),
                                      HIGH_RISK,
                                      loanApplicationId);
                              return m2PWrapperApi
                                  .uploadRiskAgainstLead(
                                      riskDetailsDataTableDTO,
                                      String.valueOf(response.getClientId()))
                                  .flatMap(uploadResponse -> Mono.empty());
                            }))
                    .flatMap(
                        nexusResponse -> {
                          int score = Integer.parseInt(response.getScoreValue());
                          String risk = (score <= 600) ? getRiskValue(nexusResponse) : LOW_RISK;
                          RiskDetailsDataTableDTO riskDetailsDataTableDTO =
                              getRiskDetailDTO(
                                  String.valueOf(response.getClientId()), risk, loanApplicationId);
                          return m2PWrapperApi.uploadRiskAgainstLead(
                              riskDetailsDataTableDTO, String.valueOf(response.getClientId()));
                        }))
        .onErrorResume(
            error -> {
              log.error(
                  "[RISK_PROCESS] [ERROR] error in completing risk process {}", error.getMessage());
              return updateRiskCategorizationFailedCase(loanApplicationId).then(Mono.error(error));
            });
  }

  private String getRiskValue(NexusColorCodeResponse nexusResponse) {
    return nexusResponse.getColorCode().equalsIgnoreCase(RED) ? HIGH_RISK : MEDIUM_RISK;
  }

  private RiskDetailsDataTableDTO getRiskDetailDTO(String leadId, String risk, String loanId) {

    return RiskDetailsDataTableDTO.builder()
        .leadId(String.valueOf(leadId))
        .loanId((loanId))
        .risk(risk)
        .timestamp(convertEpochMilliToIst(System.currentTimeMillis()))
        .build();
  }

  public Mono<?> registerKycStatus(M2pKycCallBackWithAmlRequest requestBody) {
    return triggerProductControlFlow(
            requestBody, requestBody.getProductCode(), KYC_CALLBACK_IDENTIFIER, false)
        .flatMap(
            response ->
                triggerRiskCategorisationM2pApi(requestBody.getClientId())
                    .flatMap(apiResponse -> Mono.just(response)))
        .onErrorResume(
            error ->
                triggerRiskCategorisationM2pApi(requestBody.getClientId())
                    .flatMap(apiResponse -> Mono.error(error)));
  }

  private Mono<Object> triggerRiskCategorisationM2pApi(String clientId) {
    return Mono.deferContextual(
            contextView -> {
              M2pRiskCategorisationCallRequest riskRequest =
                  M2pRiskCategorisationCallRequest.builder()
                      .riskCdRisk(String.valueOf(riskCodeConfig.getHighId()))
                      .date(DateTimeConverterUtil.getTodayDate())
                      .reason("Initial risk categorisation")
                      .locale(EN)
                      .dateFormat(DATE_FORMAT)
                      .build();

              return m2PWrapperApi
                  .getRiskCategorisationTable(clientId)
                  .flatMap(
                      response -> {
                        // response is a list
                        if (response instanceof List && !((List<?>) response).isEmpty()) {
                          log.info("Risk categorisation already exists for clientId: {}", clientId);
                          return Mono.just(response);
                        }
                        // Empty list — trigger the update API
                        return m2PWrapperApi
                            .updateRiskCategorisationTable(clientId, riskRequest)
                            .contextWrite(contextView);
                      });
            })
        .onErrorResume(
            error -> Mono.just("error while triggering risk categorisation api, error suppressed"));
  }

  public Mono<?> registerOKycStatus(M2pKycCallBackWithAmlRequest requestBody) {
    return triggerProductControlFlow(
            requestBody, requestBody.getProductCode(), OKYC_CALLBACK_IDENTIFIER, false)
        .flatMap(
            response ->
                triggerRiskCategorisationM2pApi(requestBody.getClientId())
                    .flatMap(apiResponse -> Mono.just(response)))
        .onErrorResume(
            error ->
                triggerRiskCategorisationM2pApi(requestBody.getClientId())
                    .flatMap(apiResponse -> Mono.error(error)));
  }

  public Mono<?> registerESignStatus(M2pKycCallBackWithAmlRequest requestBody) {
    return triggerProductControlFlow(
        requestBody, requestBody.getProductCode(), E_SIGN_CALLBACK_IDENTIFIER, false);
  }

  public Mono<?> registerBreStatus(Map<String, Object> requestBody, String productCode) {
    return triggerProductControlFlow(requestBody, productCode, BRE_CALLBACK_IDENTIFIER, false);
  }

  public Mono<?> registerCKycStatus(M2pKycCallBackWithAmlRequest requestBody) {
    return triggerProductControlFlow(
        requestBody, requestBody.getProductCode(), CKYC_CALLBACK_IDENTIFIER, false);
  }

  public Mono<?> registerManualKycStatus(M2pKycCallBackWithAmlRequest requestBody) {
    return triggerProductControlFlow(
        requestBody, requestBody.getProductCode(), MANUAL_KYC_CALLBACK_IDENTIFIER, false);
  }

  /**
   * Registers rejection status when M2P sends a rejection callback. If the partner has REJECTION_CB
   * flow configured, sends the callback to the partner; otherwise sends it to LSP service.
   */
  public Mono<?> registerRejectionStatus(M2pKycCallBackWithAmlRequest requestBody) {
    String productCode = requestBody.getProductCode();
    String traceId = MDC.get(TRACE_ID);

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow rejectionFlow =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), REJECTION_CALLBACK_IDENTIFIER);

              if (rejectionFlow != null) {
                log.info(
                    "[{}] Partner has REJECTION_CB configured for productCode={}, sending to"
                        + " partner",
                    REJECTION_CALLBACK_IDENTIFIER,
                    productCode);
                return triggerProductControlFlow(
                    requestBody, productCode, REJECTION_CALLBACK_IDENTIFIER, false);
              }

              log.info(
                  "[{}] Partner does not have REJECTION_CB for productCode={}, sending rejection"
                      + " callback to LSP",
                  REJECTION_CALLBACK_IDENTIFIER,
                  productCode);
              // Build callback log entity so LSP rejection callbacks are persisted like partner
              // callbacks
              CallbackLogEntity callback = getCallbackEntity(REJECTION_CALLBACK_IDENTIFIER, false);
              callback.setRequest(Json.of(gson.toJson(requestBody)));
              callback.setProductCode(productCode);
              callback.setReferenceId(requestBody.getLoanApplicationId());

              // Fetch partner_id from partner_master by product_code; fallback to partner_code from
              // product config
              String partnerCodeFromConfig = productControlConfigData.getT1();
              Mono<?> lspCall =
                  partnerMasterService
                      .findByProductCode(productCode)
                      .map(partner -> partner.getPartnerId())
                      .flatMap(
                          partnerId -> {
                            log.info(
                                "[{}] Resolved partnerId={} from partner_master for productCode={}",
                                REJECTION_CALLBACK_IDENTIFIER,
                                partnerId,
                                productCode);
                            return lspApi.postRejectionCallback(requestBody, partnerId);
                          })
                      .onErrorResume(
                          e -> {
                            log.warn(
                                "[{}] Could not resolve partnerId from partner_master for"
                                    + " productCode={}, using partner_code from config as fallback:"
                                    + " {}",
                                REJECTION_CALLBACK_IDENTIFIER,
                                productCode,
                                e.getMessage());
                            return lspApi.postRejectionCallback(
                                requestBody,
                                partnerCodeFromConfig != null && !partnerCodeFromConfig.isBlank()
                                    ? partnerCodeFromConfig
                                    : null);
                          });

              return lspCall
                  .flatMap(
                      response -> {
                        callback.setResponse(Json.of(gson.toJson(response)));
                        return callbackStoreService
                            .save(callback)
                            .flatMap(data -> Mono.just(response));
                      })
                  .onErrorResume(
                      error -> {
                        setErrorDataInCallbackEntity(callback, error);
                        return callbackStoreService
                            .save(callback)
                            .flatMap(data -> Mono.error(error));
                      });
            })
        .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx)
        .doOnError(err -> log.error("Error in registerRejectionStatus: {}", err.getMessage()));
  }

  public Mono<?> registerFiStatus(M2pKycCallBackWithAmlRequest requestBody) {
    return triggerProductControlFlow(
        requestBody, requestBody.getProductCode(), FI_STATUS_CALLBACK_IDENTIFIER, false);
  }

  public Mono<?> registerCkycrStatus(M2pCkycrCallbackRequest requestBody) {
    return m2PWrapperApi.registerCta(requestBody.getLoanId().toString(), "ckycr-status");
  }

  /**
   * Registers loan closure status when M2P sends a closure callback. 1) null productKey: ack
   * success without partner or LSP routing 2) CLOSURE_CB configured: partner callback 3) otherwise:
   * LSP callback
   */
  public Mono<?> registerLoanClosureStatus(M2pLoanClosureCallBackRequest requestBody) {
    String productKey = requestBody.getProductKey();
    String traceId = MDC.get(TRACE_ID);

    if (productKey == null) {
      log.warn(
          "[{}] productKey is null, skipping partner and LSP closure callback for"
              + " clientId = {}",
          CLOSURE_CALLBACK_IDENTIFIER,
          requestBody.getClientId());
      return Mono.just(Map.of("success", true))
          .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx);
    }

    return productConfigMasterService
        .getProductConfigMasterData(productKey)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow closureFlow =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), CLOSURE_CALLBACK_IDENTIFIER);

              if (closureFlow != null) {
                log.info(
                    "[{}] Partner has CLOSURE_CB configured for productKey={}, sending callback to"
                        + " partner",
                    CLOSURE_CALLBACK_IDENTIFIER,
                    productKey);
                return triggerProductControlFlow(
                    requestBody, productKey, CLOSURE_CALLBACK_IDENTIFIER, false);
              }

              log.info(
                  "[{}] Partner does not have CLOSURE_CB for productKey={}, sending closure"
                      + " callback to LSP",
                  CLOSURE_CALLBACK_IDENTIFIER,
                  productKey);
              CallbackLogEntity callback = getCallbackEntity(CLOSURE_CALLBACK_IDENTIFIER, false);
              callback.setRequest(Json.of(gson.toJson(requestBody)));
              callback.setProductCode(productKey);
              callback.setReferenceId(requestBody.getLoanApplicationId());

              String partnerCodeFromConfig = productControlConfigData.getT1();
              Mono<?> lspCall =
                  partnerMasterService
                      .findByProductCode(productKey)
                      .map(partner -> partner.getPartnerId())
                      .flatMap(
                          partnerId -> {
                            log.info(
                                "[{}] Resolved partnerId={} from partner_master for productKey={}",
                                CLOSURE_CALLBACK_IDENTIFIER,
                                partnerId,
                                productKey);
                            return lspApi.postClosureCallback(requestBody, partnerId);
                          })
                      .onErrorResume(
                          e -> {
                            log.warn(
                                "[{}] Could not resolve partnerId from partner_master for"
                                    + " productKey={}, using partner_code from config as fallback:"
                                    + " {}",
                                CLOSURE_CALLBACK_IDENTIFIER,
                                productKey,
                                e.getMessage());
                            return lspApi.postClosureCallback(
                                requestBody,
                                partnerCodeFromConfig != null && !partnerCodeFromConfig.isBlank()
                                    ? partnerCodeFromConfig
                                    : null);
                          });

              return lspCall
                  .flatMap(
                      response -> {
                        callback.setResponse(Json.of(gson.toJson(response)));
                        return callbackStoreService
                            .save(callback)
                            .flatMap(data -> Mono.just(response));
                      })
                  .onErrorResume(
                      error -> {
                        setErrorDataInCallbackEntity(callback, error);
                        return callbackStoreService
                            .save(callback)
                            .flatMap(data -> Mono.error(error));
                      });
            })
        .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx)
        .doOnError(err -> log.error("Error in registerLoanClosureStatus: {}", err.getMessage()));
  }

  public <T> Mono<?> triggerProductControlFlow(
      T requestBody, String productCode, String flowIdentifier, Boolean isRetry) {

    String traceId = MDC.get(TRACE_ID);
    // fetching product configuration from database based on product code
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);
    return productConfigTuple
        .flatMap(
            productControlConfigData -> {
              // extracting data from product configuration
              String partnerCode = productControlConfigData.getT1();
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), flowIdentifier);

              // npe checks for product configuration data
              if (Objects.isNull(flowData)) {
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }

              // extracting parameters for driving the callback-cta flow for partners
              String functionName = flowData.getFunctionName();
              try {
                // trying reflection api for holding the method
                // based on the function name found in product configuration
                Method method = getMethod(requestBody, functionName);

                // invoking the method for callback-cta flow
                Mono<?> partnerFlowResult =
                    (Mono<?>)
                        method.invoke(
                            this, requestBody, productCode, partnerCode, flowData, isRetry);

                return Mono.deferContextual(
                        ctx -> {
                          triggerVcipNotificationAsync(
                              flowIdentifier, flowData, requestBody, productCode, isRetry, ctx);
                          return Mono.empty();
                        })
                    .then(partnerFlowResult);

              } catch (IllegalAccessException
                  | InvocationTargetException
                  | NoSuchMethodException e) {
                return Mono.error(e);
              }
            })
        .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx)
        .doOnError(err -> log.error("Error in triggerProductControlFlow: {}", err.getMessage()));
  }

  private <T> void triggerVcipNotificationAsync(
      String flowIdentifier,
      ProductControl.Flow flowData,
      T requestBody,
      String productCode,
      Boolean isRetry,
      ContextView parentContext) {

    if (!Boolean.TRUE.equals(flowData.getVcipSmsEnabled())) return;
    if (!Boolean.FALSE.equals(isRetry)) return;

    try {
      if (requestBody instanceof M2pKycCallBackWithAmlRequest kycRequest) {
        boolean success = KYC_STATUS_VERIFIED.equalsIgnoreCase(kycRequest.getKycStatus());
        if (!success) return;

        log.info(
            "[{}] Async vKYC notification trigger start, clientId={}, loanAppId={}",
            VCIP_LINK_NOTIFICATION,
            kycRequest.getClientId(),
            kycRequest.getLoanApplicationId());

        resolvePartnerInfo(productCode)
            .flatMap(
                tuple -> {
                  String partnerId = tuple.getT1();
                  String partnerName = tuple.getT2();

                  return m2PWrapperApi
                      .getRiskCategorisationTable(kycRequest.getClientId())
                      .map(riskCodeConfig::getRiskCategory)
                      .onErrorReturn("UNKNOWN")
                      .flatMap(
                          riskCategory -> {
                            log.info(
                                "[{}] Risk={} clientId={} partnerId={}",
                                VCIP_LINK_NOTIFICATION,
                                riskCategory,
                                kycRequest.getClientId(),
                                partnerId);

                            return kycAdaptorApi.vcipNotification(
                                kycRequest.getClientId(),
                                kycRequest.getLoanApplicationId(),
                                partnerName,
                                partnerId,
                                productCode,
                                riskCategory);
                          });
                })
            .doOnSuccess(
                res ->
                    log.info(
                        "[{}] Async vKYC notification success, loanAppId={}",
                        VCIP_LINK_NOTIFICATION,
                        kycRequest.getLoanApplicationId()))
            .doOnError(
                e ->
                    log.error(
                        "[{}] Async vKYC notification failed, loanAppId={}, err={}",
                        VCIP_LINK_NOTIFICATION,
                        kycRequest.getLoanApplicationId(),
                        e.getMessage()))
            .contextWrite(
                ctx ->
                    parentContext.hasKey(TRACE_ID)
                        ? ctx.put(TRACE_ID, parentContext.get(TRACE_ID))
                        : ctx)
            .subscribe(
                v -> {},
                err ->
                    log.error(
                        "[{}] Async vKYC terminal error: {}",
                        VCIP_LINK_NOTIFICATION,
                        err.getMessage()));

      } else {
        log.debug(
            "[{}] Request is not M2pKycCallBackWithAmlRequest, skipping VCIP async trigger",
            VCIP_LINK_NOTIFICATION);
      }
    } catch (Exception e) {
      log.error(
          "[{}] Async vKYC notification trigger failed: {}",
          VCIP_LINK_NOTIFICATION,
          e.getMessage());
    }
  }

  private Mono<Tuple2<String, String>> resolvePartnerInfo(String productCode) {

    if ("ELTO".equalsIgnoreCase(productCode)) {
      return Mono.just(Tuples.of("1001", "BharatPe"));
    }

    return partnerMasterService
        .findByProductCode(productCode)
        .map(e -> Tuples.of(String.valueOf(e.getPartnerId()), e.getPartnerName()))
        .switchIfEmpty(
            Mono.error(
                new IllegalStateException(
                    "No partner mapping found for productCode=" + productCode)));
  }

  private <T> Method getMethod(T requestBody, String functionName) throws NoSuchMethodException {
    Class<?> requestBodyClass = requestBody.getClass();
    if (requestBodyClass == LinkedHashMap.class) {
      requestBodyClass = Object.class;
    }
    return this.getClass()
        .getMethod(
            functionName,
            requestBodyClass,
            String.class,
            String.class,
            ProductControl.Flow.class,
            Boolean.class);
  }

  /**
   * Registers a rejection status callback to the partner without invoking M2P's CTA API.
   *
   * @param requestBody the request body containing details for the KYC callback with AML data
   * @param productCode the code representing the product
   * @param partnerCode the code representing the partner
   * @param flowData the flow data containing the partner's URI, call method, and retry count
   * @return a {@link Mono} that emits the partner's response or an error signal
   */
  public Mono<?> registerRejectionStatusToPartnerWithoutM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(flowData.getIdentifier(), isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setProductCode(productCode);
    callback.setReferenceId(requestBody.getLoanApplicationId());

    // triggering partner API call for callback register
    return partnerApi
        .registerRejectionStatusCallback(
            requestBody,
            flowData.getPartnerUri(),
            flowData.getCallMethod(),
            partnerCode,
            flowData.getRetryCount())
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            });
  }

  /**
   * Registers the FI (Financial Institution) status callback to the partner without invoking M2P's
   * CTA API.
   *
   * @param requestBody the request body containing details for the KYC callback with AML data
   * @param productCode the code representing the product
   * @param partnerCode the code representing the partner
   * @param flowData the flow data containing the partner's URI, call method, and retry count
   * @return a {@link Mono} that emits the partner's response or an error signal
   */
  public Mono<?> registerFiStatusToPartnerWithoutM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(flowData.getIdentifier(), isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setProductCode(productCode);
    callback.setReferenceId(requestBody.getLoanApplicationId());

    // triggering partner API call for callback register
    return partnerApi
        .registerFiStatusCallback(
            requestBody,
            flowData.getPartnerUri(),
            flowData.getCallMethod(),
            partnerCode,
            flowData.getRetryCount())
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            });
  }

  public Mono<?> registerCKycStatusToPartnerWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    CallbackLogEntity callback = getCallbackEntity(CKYC_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    if (Objects.equals(requestBody.getCKycFaceMatchStatus(), VERIFIED)
        && Objects.equals(requestBody.getCKycNameMatchStatus(), VERIFIED)) {
      if (flowData.isCtaCallFlag()) {
        return m2PWrapperApi
            .registerCta(requestBody.getLoanApplicationId(), flowData.getCtaName())
            .onErrorResume(
                error -> {
                  log.error(
                      "[{}] error while triggering cta on ckyc success for loan application: {},"
                          + " error: {}",
                      CKYC_CALLBACK_IDENTIFIER,
                      requestBody.getLoanApplicationId(),
                      error.getMessage());
                  return Mono.error(
                      new BaseException(
                          CKYC_FAIL, error.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
                })
            .flatMap(
                ctaResponse ->
                    registerCallbackWithUpdateLead(requestBody, partnerCode, callback, flowData));
      }
      return registerCallbackWithUpdateLead(requestBody, partnerCode, callback, flowData);
    }
    return registerPartnerCallback(
        requestBody, partnerCode, flowData, callback, CKYC_CALLBACK_IDENTIFIER);
  }

  private Mono<?> registerCallbackWithUpdateLead(
      M2pKycCallBackWithAmlRequest requestBody,
      String partnerCode,
      CallbackLogEntity callback,
      ProductControl.Flow flowData) {
    return registerPartnerCallback(
            requestBody, partnerCode, flowData, callback, CKYC_CALLBACK_IDENTIFIER)
        .flatMap(
            callbackResponse -> {
              boolean updateLead =
                  (boolean) flowData.getConditions().getOrDefault("updateLead", false);
              if (updateLead) {
                return leadService
                    .updateLeadOnCkycSuccess(requestBody.getLoanApplicationId())
                    .flatMap(updateLeadResponse -> Mono.just(callbackResponse))
                    .onErrorResume(
                        error -> {
                          log.error(
                              "[{}] error while updating lead after ckyc success: {}",
                              CKYC_CALLBACK_IDENTIFIER,
                              error.getMessage());
                          return Mono.just(callbackResponse);
                        });
              }
              return Mono.just(callbackResponse);
            });
  }

  public Mono<?> registerManualKycStatusToPartnerWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(MANUAL_KYC_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    // triggering partner api call for callback register
    return partnerApi
        .registerManualKycCallback(
            requestBody,
            flowData.getPartnerUri(),
            flowData.getCallMethod(),
            partnerCode,
            flowData.getRetryCount())
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            })
        .flatMap(
            data -> {
              if (!flowData.isCtaCallFlag()) {
                return Mono.just(data);
              }
              return m2PWrapperApi.registerCta(
                  requestBody.getLoanApplicationId(), flowData.getCtaName());
            });
  }

  public Mono<?> registerDisbursementStatusToPartnerWithoutM2pCta(
      M2pDisbursementCallBackRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(flowData.getIdentifier(), isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setProductCode(productCode);
    callback.setReferenceId(requestBody.getLoanApplicationId().toString());
    if (flowData.isParseDisbursementDate() && !Objects.isNull(requestBody.getDisbursementDate())) {
      requestBody.setDisbursementDate(
          DateTimeConverterUtil.convertToGivenDateFormat(
              requestBody.getDisbursementDate(), "MMM dd, yyyy", "dd-MM-yyyy"));
    }
    Event e =
        requestBody.getStatus().equalsIgnoreCase("Disbursed")
            ? Event.DISBURSED
            : Event.NOT_DISBURSED;
    publishEventKafkaAsync(
        () ->
            eventProducerService.publishEvent(
                new EventContext(e, requestBody.getLoanApplicationId().toString()), null, null));
    businessLoanEvaluationService.updateLoanTypeClassificationLanFromDisbursementCallbackAsync(
        requestBody, productCode);
    persistAndCacheLanId(requestBody)
        .doOnError(
            err ->
                log.error(
                    "Failed to persist or cache lan id for loanAppId {}",
                    requestBody.getLoanApplicationId(),
                    err))
        .onErrorResume(err -> Mono.empty())
        .subscribe();

    // triggering partner api call for callback register
    return insuranceService
        .enrichWithAssurekitInsuranceDetailsIfVerified(requestBody, flowData)
        .flatMap(
            updatedRequest -> {
              callback.setRequest(Json.of(gson.toJson(updatedRequest)));
              return partnerApi.registerDisbursementCallback(
                  updatedRequest,
                  flowData.getPartnerUri(),
                  flowData.getCallMethod(),
                  partnerCode,
                  flowData.getRetryCount());
            })
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService
                  .save(callback)
                  .flatMap(
                      data -> {

                        // 2. FIRE-AND-FORGET PSL Tagging Logic
                        loanTaggingService
                            .tagLoanForPsl(String.valueOf(requestBody.getLoanApplicationId()))
                            .doOnError(
                                (Throwable taggingError) ->
                                    log.error(
                                        "PSL Tagging failed for loanApplicationId: {}",
                                        requestBody.getLoanApplicationId(),
                                        taggingError))
                            .onErrorResume(taggingError -> Mono.empty())
                            .subscribe();

                        return Mono.just(response);
                      });
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).then(Mono.error(error));
            });
  }

  public Mono<Void> persistAndCacheLanId(M2pDisbursementCallBackRequest request) {

    if (request.getLanID() == null) {
      return Mono.empty();
    }

    Integer loanAppId = request.getLoanApplicationId();
    String productKey = request.getProductCode();
    String normalizedProductKey = "ELTO".equalsIgnoreCase(productKey) ? "ELO" : productKey;

    return partnerMasterService
        .findByProductCode(normalizedProductKey)
        .map(PartnerMasterEntity::getPartnerId)
        .flatMap(
            partnerId ->
                Mono.fromCallable(
                        () -> loanClientPartnerMapRepository.findByLoanApplicationId(loanAppId))
                    .flatMap(
                        entityMono ->
                            entityMono
                                .flatMap(
                                    entity -> {
                                      entity.setLanId(request.getLanID());
                                      entity.setUpdatedAt(LocalDateTime.now());
                                      return loanClientPartnerMapRepository.save(entity).then();
                                    })
                                .then(
                                    loanApplicationCacheService.cacheLoanAccountPartner(
                                        LoanAccountPartnerEntity.builder()
                                            .lanId(String.valueOf(request.getLanID()))
                                            .partnerId(String.valueOf(partnerId))
                                            .build()))))
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }

  public Mono<?> processClientValidationFunnelKycCallbackReject(
      ClientValidationServiceKYCCallbackResponse validationServiceKYCCallbackResponse,
      CallbackLogEntity callback,
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {
    // this is case where we get REJECT as the FinalStatus from client validation funnel.
    requestBody.setKycStatus(REJECTED_CAPS);
    List<String> rejectionReasons = getRejectionReasons(validationServiceKYCCallbackResponse);
    requestBody.setKycRejectionReason(rejectionReasons);

    // Reject loan first
    return validationFunnelBasedRejectLoanApplication(
            requestBody.getLoanApplicationId(), requestBody.getKycRejectionReason())
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error while rejecting loan application on AML/PEP failure:" + " {}",
                  KYC_CALLBACK_IDENTIFIER,
                  error.getMessage());
              return Mono.error(error);
            })
        .flatMap(
            rejectionResponse ->
                // After rejection, remove aml fields & call partner with rejected
                // status
                removeAmlFieldsFromKycCallBackResponseAndSetKycStatus(requestBody)
                    .flatMap(
                        response ->
                            registerPartnerCallback(
                                requestBody,
                                partnerCode,
                                flowData,
                                callback,
                                KYC_CALLBACK_IDENTIFIER)));
  }

  private Mono<?> processClientValidationFunnelOkycCallbackReject(
      ClientValidationServiceKYCCallbackResponse validationServiceKYCCallbackResponse,
      CallbackLogEntity callback,
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {
    requestBody.setKycStatus(REJECTED_CAPS);
    List<String> rejectionReasons = getRejectionReasons(validationServiceKYCCallbackResponse);
    requestBody.setKycRejectionReason(rejectionReasons);

    return validationFunnelBasedRejectLoanApplication(
            requestBody.getLoanApplicationId(), requestBody.getKycRejectionReason())
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error while rejecting loan application on validation funnel failure: {}",
                  OKYC_CALLBACK_IDENTIFIER,
                  error.getMessage());
              return Mono.error(error);
            })
        .flatMap(
            rejectionResponse ->
                removeAmlFieldsFromOkycCallBackResponseAndSetKycStatus(requestBody)
                    .flatMap(
                        response ->
                            registerPartnerCallback(
                                requestBody,
                                partnerCode,
                                flowData,
                                callback,
                                OKYC_CALLBACK_IDENTIFIER)));
  }

  private static List<String> getRejectionReasons(
      ClientValidationServiceKYCCallbackResponse validationServiceKYCCallbackResponse) {
    List<String> rejectionReasons = new ArrayList<>();

    // Define the REJECT status for cleaner comparison
    final ClientValidationFunnelStatus.StepStatus REJECT =
        ClientValidationFunnelStatus.StepStatus.REJECT;

    // NSDL PAN Validation
    if (REJECT.equals(validationServiceKYCCallbackResponse.getNsdlPanValidationStatus())) {
      rejectionReasons.add("NSDL PAN Validation Rejection");
    }

    // Karza PAN Validation
    if (REJECT.equals(validationServiceKYCCallbackResponse.getKarzaPanValidationStatus())) {
      rejectionReasons.add("TSP PAN Validation Rejection");
    }

    // Karza Name Similarity Check
    if (REJECT.equals(validationServiceKYCCallbackResponse.getKarzaNameSimilarityStatus())) {
      rejectionReasons.add("PAN Name Match Rejection");
    }

    // DOB WaterFall
    if (REJECT.equals(validationServiceKYCCallbackResponse.getDobWaterFallStatus())) {
      rejectionReasons.add("PAN DOB Match Rejection");
    }
    return rejectionReasons;
  }

  public Mono<?> registerKycStatusToPartnerWithMatchVerifiedWithAmlWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    CallbackLogEntity callback = getCallbackEntity(KYC_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    Mono<Boolean> funnelActiveAndRejectionOn =
        validationFunnelConfigService.isValidationFunnelActiveAndRejectionIsOn(productCode);

    // Fire-and-forget M2P persist: own subscription chain so failures never affect the main
    // callback pipeline.
    subscribeAsyncValidationFunnelPersistToM2p(
        requestBody, productCode, funnelActiveAndRejectionOn);

    // 1. If KYC already rejected and not retrying, reject and call partner
    if (Optional.of(requestBody.getKycStatus().equals(REJECTED_CAPS)).orElse(false)
        && Boolean.FALSE.equals(isRetry)) {
      requestBody.setKycRejectionReason(getKycFailureReason(requestBody));
      return amlAndKycBasedLoanApplicationRejection(
              requestBody.getLoanApplicationId(), requestBody.getKycRejectionReason())
          .onErrorResume(
              error -> {
                log.error(
                    "[{}] error while rejecting loan application on the kyc failure: {}",
                    KYC_CALLBACK_IDENTIFIER,
                    error.getMessage());
                return Mono.error(error);
              })
          .flatMap(
              rejectionResponse ->
                  removeAmlFieldsFromKycCallBackResponseAndSetKycStatus(requestBody)
                      .flatMap(
                          response ->
                              registerPartnerCallback(
                                  requestBody,
                                  partnerCode,
                                  flowData,
                                  callback,
                                  KYC_CALLBACK_IDENTIFIER)));
    }

    return runValidationFunnelStageForKycCallback(
            funnelActiveAndRejectionOn,
            requestBody,
            callback,
            productCode,
            partnerCode,
            flowData,
            isRetry)
        .flatMap(
            response -> {
              if (REJECTED_CAPS.equalsIgnoreCase(response.getKycStatus())) {
                return Mono.empty(); // funnel reject already sent partner callback
              }
              return handleKycVerificationAndAmlPepChecks(
                  response, partnerCode, flowData, callback, isRetry, productCode);
            });
  }

  private Mono<M2pKycCallBackWithAmlRequest> runValidationFunnelStageForKycCallback(
      Mono<Boolean> funnelActiveAndRejectionOn,
      M2pKycCallBackWithAmlRequest requestBody,
      CallbackLogEntity callback,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    return funnelActiveAndRejectionOn
        .flatMap(
            isActive -> {
              if (!Boolean.TRUE.equals(isActive)) {
                return Mono.just(requestBody);
              }
              return validationFunnelServiceUtil
                  .readValidationServiceFinalStatus(productCode, requestBody.getClientId())
                  .flatMap(
                      resp ->
                          processValidationFunnelResult(
                              resp,
                              callback,
                              requestBody,
                              productCode,
                              partnerCode,
                              flowData,
                              isRetry))
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.error(
                                "[KYC_CB][VALIDATION_FUNNEL][ERROR] Validation funnel was active"
                                    + " but couldn't get the result from the db.");
                            return Mono.just(requestBody);
                          }))
                  .onErrorResume(
                      err -> {
                        log.error(
                            "[KYC_CB][VALIDATION_FUNNEL][ERROR] Error while"
                                + " processValidationFunnelResult: {}",
                            err.getMessage(),
                            err);
                        return Mono.just(requestBody);
                      });
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[KYC_CB][VALIDATION_FUNNEL][ERROR] Validation funnel stage failed; proceeding"
                      + " to AML/PEP checks: {}",
                  e.getMessage(),
                  e);
              return Mono.just(requestBody);
            });
  }

  private Mono<M2pKycCallBackWithAmlRequest> processValidationFunnelResult(
      ClientValidationServiceKYCCallbackResponse validationFunnelResponse,
      CallbackLogEntity callback,
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      boolean isRetry) {

    if (Objects.isNull(validationFunnelResponse)) {
      log.error(
          "[KYC_CB][VALIDATION_SERVICE][ERROR] No data found from the client"
              + " validation funnel to set in the kyc callback.");
      return Mono.just(requestBody);
    }

    ClientValidationFunnelStatus.FinalStatus finalStatus =
        validationFunnelResponse.getFinalStatus();

    if (finalStatus == ClientValidationFunnelStatus.FinalStatus.REJECT) {
      log.info(
          "[VALIDATION_FUNNEL] processing validation funnel reject case for productCode: {} with"
              + " loanApplicationId: {}",
          productCode,
          requestBody.getLoanApplicationId());
      // REJECT CASE
      return processClientValidationFunnelKycCallbackReject(
              validationFunnelResponse,
              callback,
              requestBody,
              productCode,
              partnerCode,
              flowData,
              isRetry)
          .thenReturn(requestBody);
    }

    // PASS / MANUAL_REVIEW CASE
    return Mono.just(requestBody);
  }

  private boolean isAmlPepDecoupledFeatureEnabled(ProductControl.Flow flowData) {
    AmlPepConfig amlPepConfig = getAmlPepConfigOrDefault(flowData);
    return Boolean.TRUE.equals(amlPepConfig.getDecoupleFlag())
        && Boolean.TRUE.equals(amlPepConfig.getAmlPepFeatureFlag());
  }

  private AmlPepConfig getAmlPepConfigOrDefault(ProductControl.Flow flowData) {
    if (flowData == null || flowData.getAmlPepConfig() == null) {
      return DEFAULT_AML_PEP_CONFIG;
    }
    return flowData.getAmlPepConfig();
  }

  /**
   * Handles the Aadhaar verification checks, proceeds to AML/PEP evaluation, and orchestrates the
   * final partner callback or CTA registration.
   */
  private Mono<?> handleKycVerificationAndAmlPepChecks(
      M2pKycCallBackWithAmlRequest requestBody,
      String partnerCode,
      ProductControl.Flow flowData,
      CallbackLogEntity callback,
      Boolean isRetry,
      String productCode) {

    // Check for Aadhaar Verification Statuses
    if (Objects.equals(requestBody.getAadhaarXmlValidityStatus(), VERIFIED)
        && Objects.equals(requestBody.getAadhaarXmlFaceMatchStatus(), VERIFIED)
        && Objects.equals(requestBody.getAadhaarXmlNameMatchStatus(), VERIFIED)) {

      Mono<M2pKycCallBackWithAmlRequest> enrichedMono;

      if (isAmlPepDecoupledFeatureEnabled(flowData)) {
        enrichedMono =
            enrichKycCallbackWithAmlPep(
                requestBody,
                requestBody.getClientId(),
                getAmlPepConfigOrDefault(flowData),
                productCode);
      } else {
        enrichedMono = Mono.just(requestBody);
      }

      return enrichedMono.flatMap(
          enrichedRequest ->
              evaluateAndHandleAmlPepChecks(enrichedRequest, flowData)
                  .flatMap(
                      decision -> {
                        if (decision.getDecision() == AmlPepDecisionResult.DecisionOutcome.REJECT) {
                          enrichedRequest.setKycStatus(REJECTED_CAPS);
                          enrichedRequest.setKycRejectionReason(
                              List.of(decision.getReasonDescription()));

                          return amlAndKycBasedLoanApplicationRejection(
                                  enrichedRequest.getLoanApplicationId(),
                                  enrichedRequest.getKycRejectionReason())
                              .onErrorResume(
                                  error -> {
                                    log.error(
                                        "[{}] error while rejecting loan application on AML/PEP"
                                            + " failure: {}",
                                        KYC_CALLBACK_IDENTIFIER,
                                        error.getMessage());
                                    return Mono.error(error);
                                  })
                              .flatMap(
                                  rejectionResponse ->
                                      removeAmlFieldsFromKycCallBackResponseAndSetKycStatus(
                                              enrichedRequest)
                                          .flatMap(
                                              response ->
                                                  registerPartnerCallback(
                                                      enrichedRequest,
                                                      partnerCode,
                                                      flowData,
                                                      callback,
                                                      KYC_CALLBACK_IDENTIFIER)));
                        } else {
                          // PASS / MANUAL_REVIEW
                          // update name and mobile number in m_client on successful kyc
                          if (flowData.isMClientUpdate()) {
                            loanLevelClientDetailsService
                                .updateMClientOnSuccessfulKYC(
                                    requestBody.getClientId(),
                                    requestBody.getLoanApplicationId(),
                                    requestBody.getProductCode())
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                          }

                          publishEventKafkaAsync(
                              () ->
                                  eventProducerService.publishEvent(
                                      new EventContext(
                                          Event.KYC_VERIFIED,
                                          enrichedRequest.getLoanApplicationId(),
                                          enrichedRequest.getClientId()),
                                      null,
                                      null));

                          if (!flowData.isCtaCallFlag() || Boolean.TRUE.equals(isRetry)) {
                            return removeAmlFieldsFromKycCallBackResponseAndSetKycStatus(
                                    enrichedRequest)
                                .flatMap(
                                    response ->
                                        registerPartnerCallback(
                                            enrichedRequest,
                                            partnerCode,
                                            flowData,
                                            callback,
                                            KYC_CALLBACK_IDENTIFIER));
                          }
                          // CTA path
                          return m2PWrapperApi
                              .registerCta(
                                  enrichedRequest.getLoanApplicationId(), flowData.getCtaName())
                              .flatMap(
                                  ctaResponse ->
                                      removeAmlFieldsFromKycCallBackResponseAndSetKycStatus(
                                              enrichedRequest)
                                          .flatMap(
                                              response ->
                                                  registerPartnerCallback(
                                                      enrichedRequest,
                                                      partnerCode,
                                                      flowData,
                                                      callback,
                                                      KYC_CALLBACK_IDENTIFIER))
                                          .onErrorResume(
                                              e ->
                                                  Mono.deferContextual(
                                                      parentContext -> {
                                                        triggerPanAadharAsync(
                                                            enrichedRequest,
                                                            flowData,
                                                            parentContext);
                                                        return Mono.error(e);
                                                      })))
                              .flatMap(
                                  response ->
                                      Mono.deferContextual(
                                          parentContext -> {
                                            triggerPanAadharAsync(
                                                enrichedRequest, flowData, parentContext);
                                            return Mono.just(response);
                                          }));
                        }
                      }));
    }

    return Mono.error(new BaseException(KYC_FAIL, KYC_FAIL, HttpStatus.OK));
  }

  private Mono<AmlPepDecisionResult> evaluateAndHandleAmlPepChecks(
      M2pKycCallBackWithAmlRequest requestBody, ProductControl.Flow flowData) {

    String pepMatch = requestBody.getPepMatch(); // "Yes" / "No" / "CAN_NOT_BE_DONE"
    String amlStatus = requestBody.getAmlStatus(); // "REJECTED" / "VERIFIED" / "CAN_NOT_BE_DONE"
    Double amlScore = null;
    AmlPepConfig amlPepConfig = getAmlPepConfigOrDefault(flowData);

    if (CANNOT_BE_DONE.equalsIgnoreCase(pepMatch)) {
      requestBody.setPepMatch(NO);
    }

    try {
      amlScore =
          requestBody.getAmlBestMatchScore() != null
              ? Double.valueOf(requestBody.getAmlBestMatchScore())
              : null;
    } catch (NumberFormatException e) {
      log.warn("Invalid AML score received: {}", requestBody.getAmlBestMatchScore());
      amlScore = null;
    }

    AmlPepDecisionResult.AmlPepDecisionResultBuilder resultBuilder = AmlPepDecisionResult.builder();

    // 1. PEP CHECK

    if (isPepCheckEnabled(flowData)) {
      if (YES.equalsIgnoreCase(pepMatch)) {
        String reasonDescription = PEP_REJECTION_DESCRIPTION;
        resultBuilder
            .decision(AmlPepDecisionResult.DecisionOutcome.REJECT)
            .reasonDescription(reasonDescription);

        log.info(
            "[AML_PEP_VERIFY][PEP_REJECT] clientId={} | pepMatch={} | reason={}",
            requestBody.getClientId(),
            pepMatch,
            reasonDescription);
        return saveAmlPepDecision(
                requestBody, flowData, REJECT, pepMatch, reasonDescription, amlScore)
            .thenReturn(resultBuilder.build())
            .doOnSuccess(
                res ->
                    updateRiskCategorisationM2pApi(requestBody.getClientId(), "PEP Rejected")
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe());
      }
    }

    // 2. AML CHECK
    if (isAmlCheckEnabled(flowData)) {

      if (CANNOT_BE_DONE.equalsIgnoreCase(amlStatus)) {
        String reasonDescription = AML_CHECK_CANNOT_BE_DONE;
        requestBody.setAmlStatus(VERIFIED);
        requestBody.setPepMatch(NO);

        resultBuilder
            .decision(
                AmlPepDecisionResult.DecisionOutcome
                    .PASS) // Or MANUAL_REVIEW based on business rules
            .reasonDescription(reasonDescription);

        log.info(
            "[AML_PEP_VERIFY][AML_PASS_CANNOT_BE_DONE] clientId={} | amlScore={} | reason={}",
            requestBody.getClientId(),
            requestBody.getAmlBestMatchScore(),
            reasonDescription);
        return saveAmlPepDecision(
                requestBody, flowData, CANNOT_BE_DONE, pepMatch, reasonDescription, amlScore)
            .thenReturn(resultBuilder.build());
      }
      // Apply thresholds only if score is available
      if (amlScore != null) {
        if (amlScore >= amlPepConfig.getAmlRejectionThreshold()) {
          String reasonDescription = AML_REJECTION_DESCRIPTION;
          requestBody.setAmlStatus(REJECTED_CAPS);
          resultBuilder
              .decision(AmlPepDecisionResult.DecisionOutcome.REJECT)
              .reasonDescription(reasonDescription);

          log.info(
              "[AML_PEP_VERIFY][AML_REJECT]  clientId={} | amlScore={} | reason={}",
              requestBody.getClientId(),
              requestBody.getAmlBestMatchScore(),
              reasonDescription);

          return saveAmlPepDecision(
                  requestBody, flowData, REJECT, pepMatch, reasonDescription, amlScore)
              .thenReturn(resultBuilder.build())
              .doOnSuccess(
                  res ->
                      updateRiskCategorisationM2pApi(requestBody.getClientId(), "AML Rejected")
                          .subscribeOn(Schedulers.boundedElastic())
                          .subscribe());

        } else if (amlScore >= amlPepConfig.getAmlManualVerificationThreshold()) {
          String reasonDescription = MANUAL_REVIEW_DESCRIPTION;
          requestBody.setAmlStatus(VERIFIED);
          resultBuilder
              .decision(AmlPepDecisionResult.DecisionOutcome.MANUAL_REVIEW)
              .reasonDescription(reasonDescription);

          log.info(
              "[AML_PEP_VERIFY][AML_MANUAL_REVIEW]  clientId={} | amlScore={} | reason={}",
              requestBody.getClientId(),
              requestBody.getAmlBestMatchScore(),
              reasonDescription);

          return saveAmlPepDecision(
                  requestBody, flowData, MANUAL_REVIEW, pepMatch, reasonDescription, amlScore)
              .thenReturn(resultBuilder.build());

        } else {
          String reasonDescription = AML_VERIFIED_DESCRIPTION;
          requestBody.setAmlStatus(VERIFIED);
          resultBuilder
              .decision(AmlPepDecisionResult.DecisionOutcome.PASS)
              .reasonDescription(reasonDescription);

          log.info(
              "[AML_PEP_VERIFY][AML_PASS]  clientId={} | amlScore={} | reason={}",
              requestBody.getClientId(),
              requestBody.getAmlBestMatchScore(),
              reasonDescription);
          return saveAmlPepDecision(
                  requestBody, flowData, PASS, pepMatch, reasonDescription, amlScore)
              .thenReturn(resultBuilder.build());
        }
      }
    }

    // 3. DEFAULT CASE — All checks passed or disabled

    String reasonDescription = DEFAULT_AML_PEP_DISABLE_DESCRIPTION;
    resultBuilder
        .decision(AmlPepDecisionResult.DecisionOutcome.PASS)
        .reasonDescription(reasonDescription);

    log.info(
        "[AML_PEP_VERIFY][PEP_AML_PASS][AML_PEP_VALIDATION_DISABLED]  clientId={} | amlScore={} |"
            + " reason={}",
        requestBody.getClientId(),
        requestBody.getAmlBestMatchScore(),
        reasonDescription);
    return saveAmlPepDecision(
            requestBody,
            flowData,
            FEATURE_NOT_ENABLED,
            FEATURE_NOT_ENABLED,
            reasonDescription,
            amlScore)
        .thenReturn(resultBuilder.build());
  }

  private boolean isPepCheckEnabled(ProductControl.Flow flowData) {
    AmlPepConfig amlPepConfig = getAmlPepConfigOrDefault(flowData);
    return Boolean.TRUE.equals(amlPepConfig.isPepCheckEnabled());
  }

  private boolean isAmlCheckEnabled(ProductControl.Flow flowData) {
    AmlPepConfig amlPepConfig = getAmlPepConfigOrDefault(flowData);
    return Boolean.TRUE.equals(amlPepConfig.isAmlCheckEnabled());
  }

  private Mono<Void> saveAmlPepDecision(
      M2pKycCallBackWithAmlRequest requestBody,
      ProductControl.Flow flowData,
      String amlDecision,
      String pepDecision,
      String reasonDescription,
      Double amlScore) {

    // Always prepare M2P save
    Mono<Void> m2pSaveMono =
        saveAmlPepDecisionToM2p(
                requestBody.getLoanApplicationId(),
                amlDecision,
                amlScore,
                pepDecision,
                reasonDescription)
            .onErrorResume(
                e -> {
                  log.error(
                      "[AML_PEP_VERIFY][STORE][M2P_SAVE_ERROR] Failed to save to M2P for"
                          + " LoanApplicationId={}",
                      requestBody.getLoanApplicationId(),
                      e);
                  return Mono.empty(); // continue even if M2P fails
                });

    // DB save if feature flag enabled
    AmlPepConfig amlPepConfig = getAmlPepConfigOrDefault(flowData);
    if (Boolean.TRUE.equals(amlPepConfig.getAmlPepFeatureFlag())) {
      AmlPepFinalStatus finalStatus = resolveFinalStatus(pepDecision, amlDecision);

      Mono<Void> dbSaveMono =
          updateAmlAndPepDecisionInDb(
                  requestBody.getClientId(), finalStatus, requestBody.getLoanApplicationId())
              .onErrorResume(
                  e -> {
                    log.error(
                        "[AML_PEP_VERIFY][STORE][DB_SAVE_ERROR] Failed to save to DB for"
                            + " clientId={}",
                        requestBody.getClientId(),
                        e);
                    return Mono.empty();
                  });

      // Run both in parallel
      return Mono.when(dbSaveMono, m2pSaveMono)
          .doOnSubscribe(
              sub ->
                  log.info(
                      "[AML_PEP_VERIFY] Saving to both DB and M2P for clientId={} loanAppId={}",
                      requestBody.getClientId(),
                      requestBody.getLoanApplicationId()))
          .doOnSuccess(
              v ->
                  log.info(
                      "[AML_PEP_VERIFY][STORE] Successfully saved AML/PEP decision to both DB and"
                          + " M2P"))
          .doOnError(
              e -> log.error("[AML_PEP_VERIFY][STORE][ERROR] Failure during save to DB or M2P", e))
          .then();
    } else {
      // Only M2P save
      return m2pSaveMono
          .doOnSubscribe(
              sub ->
                  log.info(
                      "[AML_PEP_VERIFY][CONFIG] Feature flag off — saving only to M2P for"
                          + " loanAppId={}",
                      requestBody.getLoanApplicationId()))
          .doOnSuccess(
              v ->
                  log.info(
                      "[AML_PEP_VERIFY][STORE] Successfully saved AML/PEP decision only to M2P"))
          .then();
    }
  }

  private Mono<Void> updateAmlAndPepDecisionInDb(
      String leadId, AmlPepFinalStatus finalStatus, String loanApplicationId) {

    return amlPepResultsRepository
        .findFirstByClientIdAndLeadId(leadId, loanApplicationId)
        .flatMap(
            entity -> {
              entity.setFinalStatus(finalStatus.name());
              entity.setUpdatedAt(java.time.LocalDateTime.now());
              return amlPepResultsRepository.save(entity);
            })
        .doOnSuccess(
            r ->
                log.info(
                    "[AML_PEP_VERIFY][STORE][DB] Updated AML and PEP decision in DB for LeadID {}",
                    leadId))
        .doOnError(
            e ->
                log.error(
                    "[AML_PEP_VERIFY][STORE][DB] Failed to update AML and PEP decision in DB for"
                        + " LeadID {}",
                    leadId,
                    e))
        .then();
  }

  public static AmlPepFinalStatus resolveFinalStatus(String pepStatus, String amlStatus) {

    // FAIL (PEP = Yes) → REJECT_PEP
    if (YES.equalsIgnoreCase(pepStatus)) {
      return AmlPepFinalStatus.REJECT_PEP;
    }

    // PASS PASS → PASS
    if (NO.equalsIgnoreCase(pepStatus) && PASS.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.PASS;
    }

    // PASS FAIL → REJECT_AML
    if (NO.equalsIgnoreCase(pepStatus) && REJECT.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.REJECT_AML;
    }

    // CAN_NOT_BE_DONE PASS → MANUAL_VERIFY_PEP
    if (CANNOT_BE_DONE.equalsIgnoreCase(pepStatus) && PASS.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.MANUAL_VERIFY_PEP;
    }

    // CAN_NOT_BE_DONE FAIL → REJECT_AML
    if (CANNOT_BE_DONE.equalsIgnoreCase(pepStatus) && REJECT.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.REJECT_AML;
    }

    // CAN_NOT_BE_DONE MANUAL_REVIEW → MANUAL_VERIFY_AML_PEP
    if (CANNOT_BE_DONE.equalsIgnoreCase(pepStatus) && MANUAL_REVIEW.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.MANUAL_VERIFY_AML_PEP;
    }

    // CAN_NOT_BE_DONE CAN_NOT_BE_DONE → MANUAL_VERIFY_AML_PEP
    if (CANNOT_BE_DONE.equalsIgnoreCase(pepStatus) && CANNOT_BE_DONE.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.MANUAL_VERIFY_AML_PEP;
    }

    // PASS CAN_NOT_BE_DONE → MANUAL_VERIFY_AML
    if (NO.equalsIgnoreCase(pepStatus) && CANNOT_BE_DONE.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.MANUAL_VERIFY_AML;
    }

    // PASS MANUAL_REVIEW → MANUAL_VERIFY_AML
    if (NO.equalsIgnoreCase(pepStatus) && MANUAL_REVIEW.equalsIgnoreCase(amlStatus)) {
      return AmlPepFinalStatus.MANUAL_VERIFY_AML;
    }
    // Default fallback
    return AmlPepFinalStatus.MANUAL_VERIFY_AML_PEP;
  }

  private void subscribeAsyncValidationFunnelPersistToM2p(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      Mono<Boolean> funnelActiveAndRejectionOn) {

    funnelActiveAndRejectionOn
        .flatMap(
            isActive ->
                Boolean.TRUE.equals(isActive)
                    ? validationFunnelServiceUtil.readValidationServiceFinalStatus(
                        productCode, requestBody.getClientId())
                    : Mono.empty())
        .flatMap(
            resp ->
                saveValidationFunnelDataOnCallback(
                    requestBody.getClientId(), requestBody.getLoanApplicationId(), resp))
        .onErrorResume(
            e -> {
              log.error(
                  "[KYC_CB][VALIDATION_SERVICE][DB_SAVE_ERROR] Async funnel M2P persist failed"
                      + " (ignored for callback pipeline): {}",
                  e.getMessage(),
                  e);
              return Mono.empty();
            })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            null,
            e ->
                log.error(
                    "[KYC_CB][VALIDATION_SERVICE][DB_SAVE_ERROR] Async funnel M2P persist terminal"
                        + " error (ignored for callback pipeline)",
                    e));
  }

  private Mono<Void> saveValidationFunnelDataOnCallback(
      String clientId, String leadId, ClientValidationServiceKYCCallbackResponse result) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    String date = LocalDateTime.now().format(formatter);

    ValidationFunnelVerificationResultCallbackLog callbackLog =
        ValidationFunnelVerificationResultCallbackLog.builder()
            .clientId(clientId)
            .leadId(leadId)
            .nsdlPanVerification(String.valueOf(result.getNsdlPanValidationStatus()))
            .karzaPanVerification(String.valueOf(result.getKarzaPanValidationStatus()))
            .karzaNameVerification(String.valueOf(result.getKarzaNameSimilarityStatus()))
            .dobWaterfallResult(String.valueOf(result.getDobWaterFallStatus()))
            .timestamp(date)
            .finalVerificationResult(String.valueOf(result.getFinalStatus()))
            .nameFuzzyMatchPercentage(result.getNameFuzzyMatchPercentage())
            .build();

    return m2PWrapperApi
        .persistValidationFunnelResult(callbackLog, clientId)
        .doOnSuccess(
            r ->
                log.info(
                    "[KYC_CB][VALIDATION_FUNNEL][LOG_IN_DATA_TABLE] Successfully saved validation"
                        + " funnel data for leadId {}",
                    leadId))
        .doOnError(
            e ->
                log.error(
                    "Failed to save validation funnel data for leadId {}, error= {}",
                    leadId,
                    e.getMessage()))
        .then();
  }

  private Mono<Void> saveAmlPepDecisionToM2p(
      String leadId,
      String amlDecision,
      Double amlScore,
      String pepMatch,
      String reasonDescription) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AML_PEP_DATE_TIME_FORMAT);
    String decisionDateValue = LocalDateTime.now().format(formatter);

    AmlPepDatatableDTO dto =
        AmlPepDatatableDTO.builder()
            .leadId(leadId)
            .amlDecision(amlDecision)
            .amlNameMatchScore(amlScore)
            .pepResult(pepMatch)
            .reasonDescription(reasonDescription)
            .decisionDate(decisionDateValue)
            .dateFormat(AML_PEP_DATE_TIME_FORMAT)
            .locale(ENGLISH_PREFIX)
            .build();
    return m2PWrapperApi
        .saveAmlPepResult(dto, leadId)
        .doOnSuccess(
            r ->
                log.info(
                    "[AML_PEP_VERIFY][STORE][M2P] Saved AML/PEP decision for LeadID {}", leadId))
        .doOnError(
            e ->
                log.error(
                    "[AML_PEP_VERIFY][STORE][M2P] Failed to save AML/PEP decision for LeadID {}",
                    leadId,
                    e))
        .then();
  }

  private void triggerPanAadharAsync(
      M2pKycCallBackWithAmlRequest requestBody,
      ProductControl.Flow flowData,
      ContextView parentContext) {
    triggerPanAadharProcess(requestBody, flowData)
        .subscribeOn(Schedulers.parallel())
        .contextWrite(
            context ->
                context
                    .put(TRACE_ID, parentContext.get(TRACE_ID))
                    .put(PARTNER_ID, parentContext.get(PARTNER_ID)))
        .subscribe();
  }

  private Mono<?> triggerPanAadharProcess(
      M2pKycCallBackWithAmlRequest requestBody, ProductControl.Flow flowData) {

    if (flowData.isAadhaarPanLinkCheck()) {
      log.info(
          "[PAN_AADHAAR_LINK] Pan Aadhaar Link process triggered for client"
              + " Id {} and loan Application Id {}",
          requestBody.getClientId(),
          requestBody.getLoanApplicationId());
      return m2PWrapperApi
          .getPanAadhaarDetailsByClientId(requestBody.getClientId())
          .flatMap(
              panAadhaarDetailsResponse ->
                  karzaApi
                      .getPanAadhaarLinkStatus(
                          KarazaPanAadhaarLinkCheckDTO.builder()
                              .pan(panAadhaarDetailsResponse.getPanNumber())
                              .aadhaarNo(panAadhaarDetailsResponse.getAadhaarId())
                              .consent("Y")
                              .build(),
                          requestBody.getLoanApplicationId(),
                          requestBody.getClientId())
                      .flatMap(
                          karzaResponse ->
                              // SAVING DATA IN LOS REPOSITORY
                              panAadhaarLinkageRepository
                                  .save(
                                      PanAadhaarLinkageEntity.builder()
                                          .pan(maskPan(panAadhaarDetailsResponse.getPanNumber()))
                                          .aadhaar(panAadhaarDetailsResponse.getAadhaarId())
                                          .productCode(requestBody.getProductCode())
                                          .clientId(requestBody.getClientId())
                                          .loanId(requestBody.getLoanApplicationId())
                                          .linked(
                                              String.valueOf(karzaResponse.getResult().getLinked()))
                                          .kycType(AadhaarXMLType.DIGI_LOCKER.getDisplayName())
                                          .createdAt(LocalDateTime.now())
                                          .build())
                                  .then(
                                      // SAVING DATA IN M2P REPOSITORY
                                      m2PWrapperApi.addPanAadhaarLinkDetailsDataTable(
                                          PanAadhaarLinkStatusDataTableDTO.builder()
                                              .pan(panAadhaarDetailsResponse.getPanNumber())
                                              .adhaar(panAadhaarDetailsResponse.getAadhaarId())
                                              .linked(karzaResponse.getResult().getLinked())
                                              .locale("en")
                                              .dateFormat("dd MMMM yyyy")
                                              .build(),
                                          requestBody.getLoanApplicationId())))
                      .onErrorResume(
                          error -> {
                            log.error(
                                "[PAN_AADHAAR_LINK] [ERROR] Error occurred in pan aadhaar"
                                    + " flow {}",
                                error.getMessage());
                            return Mono.error(error); // Ignore error and continue
                          }));
    }
    log.info(
        "[PAN_AADHAAR_LINK] Skipping Pan Aadhaar Link process for client Id"
            + " {} and loan Application Id {}",
        requestBody.getClientId(),
        requestBody.getLoanApplicationId());
    return Mono.empty();
  }

  /**
   * Async wrapper for OKYC-specific PAN-Aadhaar link check. Uses referenceIdPrefix from
   * aadhaar_reference_id table as Aadhaar number.
   */
  private void triggerPanAadharAsyncForOkyc(
      M2pKycCallBackWithAmlRequest requestBody,
      ProductControl.Flow flowData,
      ContextView parentContext) {
    triggerPanAadharProcessForOkyc(requestBody, flowData)
        .subscribeOn(Schedulers.parallel())
        .contextWrite(context -> context.putAll(parentContext))
        .subscribe();
  }

  /**
   * OKYC-specific PAN-Aadhaar link process. Fetches PAN from M2P API and uses referenceIdPrefix
   * from aadhaar_reference_id table as Aadhaar number.
   */
  private Mono<?> triggerPanAadharProcessForOkyc(
      M2pKycCallBackWithAmlRequest requestBody, ProductControl.Flow flowData) {

    if (flowData.isAadhaarPanLinkCheck()) {
      log.info(
          "[PAN_AADHAAR_LINK_OKYC] Pan Aadhaar Link process triggered for client"
              + " Id {} and loan Application Id {}",
          requestBody.getClientId(),
          requestBody.getLoanApplicationId());

      // Fetch PAN from M2P API
      Mono<String> panMono =
          m2PWrapperApi
              .getPanAadhaarDetailsByClientId(requestBody.getClientId())
              .flatMap(
                  panAadhaarDetailsResponse -> {
                    String panNumber = panAadhaarDetailsResponse.getPanNumber();
                    if (panNumber == null || panNumber.isBlank()) {
                      log.error(
                          "[PAN_AADHAAR_LINK_OKYC] PAN is null or blank for clientId: {}, skipping"
                              + " flow",
                          requestBody.getClientId());
                      return Mono.empty();
                    }
                    return Mono.just(panNumber);
                  })
              .switchIfEmpty(
                  Mono.defer(
                      () -> {
                        log.error(
                            "[PAN_AADHAAR_LINK_OKYC] PAN details not found for clientId: {},"
                                + " skipping flow",
                            requestBody.getClientId());
                        return Mono.empty();
                      }))
              .onErrorResume(
                  e -> {
                    log.error(
                        "[PAN_AADHAAR_LINK_OKYC] Error fetching PAN for clientId: {}, error: {},"
                            + " skipping flow",
                        requestBody.getClientId(),
                        e.getMessage());
                    return Mono.empty();
                  });

      // Fetch referenceIdPrefix from our table (latest OKYC entry for clientId)
      Mono<String> aadhaarPrefixMono =
          aadhaarReferenceIdRepository
              .findFirstByClientIdAndDocumentTypeOrderByIdDesc(
                  requestBody.getClientId(), DocumentType.AADHAAR_OKYC)
              .flatMap(
                  entity -> {
                    String referenceIdPrefix = entity.getDocumentId();
                    if (referenceIdPrefix == null || referenceIdPrefix.isBlank()) {
                      log.error(
                          "[PAN_AADHAAR_LINK_OKYC] referenceIdPrefix is null or blank for clientId:"
                              + " {}, skipping flow",
                          requestBody.getClientId());
                      return Mono.empty();
                    }
                    return Mono.just(referenceIdPrefix);
                  })
              .switchIfEmpty(
                  Mono.defer(
                      () -> {
                        log.error(
                            "[PAN_AADHAAR_LINK_OKYC] No referenceIdPrefix found in table for"
                                + " clientId: {}, skipping flow",
                            requestBody.getClientId());
                        return Mono.empty();
                      }));

      return Mono.zip(panMono, aadhaarPrefixMono)
          .flatMap(
              tuple -> {
                String pan = tuple.getT1();
                String aadhaarPrefix = tuple.getT2();

                log.info(
                    "[PAN_AADHAAR_LINK_OKYC] Calling Karza API with PAN and referenceIdPrefix for"
                        + " clientId: {}",
                    requestBody.getClientId());

                return karzaApi
                    .getPanAadhaarLinkStatus(
                        KarazaPanAadhaarLinkCheckDTO.builder()
                            .pan(pan)
                            .aadhaarNo(aadhaarPrefix)
                            .consent("Y")
                            .build(),
                        requestBody.getLoanApplicationId(),
                        requestBody.getClientId())
                    .flatMap(
                        karzaResponse ->
                            // SAVING DATA IN LOS REPOSITORY
                            panAadhaarLinkageService
                                .savePanAadhaarLinkage(
                                    maskPan(pan),
                                    aadhaarPrefix,
                                    requestBody.getProductCode(),
                                    requestBody.getClientId(),
                                    requestBody.getLoanApplicationId(),
                                    String.valueOf(karzaResponse.getResult().getLinked()),
                                    AadhaarXMLType.OKYC.getDisplayName())
                                .then(
                                    // SAVING DATA IN M2P REPOSITORY
                                    m2PWrapperApi.addPanAadhaarLinkDetailsDataTable(
                                        PanAadhaarLinkStatusDataTableDTO.builder()
                                            .pan(pan)
                                            .adhaar(aadhaarPrefix)
                                            .linked(karzaResponse.getResult().getLinked())
                                            .locale("en")
                                            .dateFormat("dd MMMM yyyy")
                                            .build(),
                                        requestBody.getLoanApplicationId())))
                    .onErrorResume(
                        error -> {
                          log.error(
                              "[PAN_AADHAAR_LINK_OKYC] [ERROR] Error occurred in pan aadhaar flow:"
                                  + " {}",
                              error.getMessage());
                          return Mono.empty();
                        });
              });
    }

    log.info(
        "[PAN_AADHAAR_LINK_OKYC] Skipping Pan Aadhaar Link process for client Id"
            + " {} and loan Application Id {}",
        requestBody.getClientId(),
        requestBody.getLoanApplicationId());
    return Mono.empty();
  }

  private Mono<?> checkAndPopulatePanAadhaarLinkStatus(M2pKycCallBackWithAmlRequest requestBody) {
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(requestBody.getProductCode());
    return productConfigTuple.flatMap(
        productControlConfigData -> {
          ProductControl.Flow flowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), KYC_CALLBACK_IDENTIFIER);

          // npe checks for product configuration data
          if (Objects.isNull(flowData)) {
            throw new BaseException(
                SOMETHING_WENT_WRONG_CONFIG,
                SOMETHING_WENT_WRONG_CONFIG,
                HttpStatus.INTERNAL_SERVER_ERROR);
          }
          // extracting parameters for driving the callback-cta flow for partners
          if (flowData.isAadhaarPanLinkCheck()) {
            m2PWrapperApi
                .getPanAadhaarDetailsByClientId(requestBody.getClientId())
                .flatMap(
                    panAadhaarDetailsResponse ->
                        karzaApi
                            .getPanAadhaarLinkStatus(
                                KarazaPanAadhaarLinkCheckDTO.builder()
                                    .pan(panAadhaarDetailsResponse.getPanNumber())
                                    .aadhaarNo(panAadhaarDetailsResponse.getAadhaarId())
                                    .consent("Y")
                                    .build(),
                                requestBody.getLoanApplicationId(),
                                requestBody.getClientId())
                            .flatMap(
                                karzaResponse ->
                                    m2PWrapperApi.addPanAadhaarLinkDetailsDataTable(
                                        PanAadhaarLinkStatusDataTableDTO.builder()
                                            .pan(panAadhaarDetailsResponse.getPanNumber())
                                            .adhaar(panAadhaarDetailsResponse.getAadhaarId())
                                            .linked(karzaResponse.getResult().getLinked())
                                            .locale("en")
                                            .dateFormat("dd MMMM yyyy")
                                            .build(),
                                        requestBody.getLoanApplicationId())));
          }
          return null;
        });
  }

  private Mono<M2pLoanRejectResponseDTO> validationFunnelBasedRejectLoanApplication(
      String loanApplicationId, List<String> kycRejectionReason) {
    LoanReject loanReject =
        LoanReject.builder()
            .reasonCode(reasonCodeFactory.getKycFailReasonCode())
            .description(kycRejectionReason.toString())
            .build();
    return m2PWrapperApi.rejectLoanApplication(loanReject, loanApplicationId);
  }

  private Mono<M2pLoanRejectResponseDTO> amlAndKycBasedLoanApplicationRejection(
      String loanApplicationId, List<String> kycRejectionReason) {
    LoanReject loanReject =
        LoanReject.builder()
            .reasonCode(reasonCodeFactory.getKycFailReasonCode())
            .description(kycRejectionReason.toString())
            .build();
    return m2PWrapperApi.rejectLoanApplication(loanReject, loanApplicationId);
  }

  private Mono<String> removeAmlFieldsFromKycCallBackResponseAndSetKycStatus(
      M2pKycCallBackWithAmlRequest requestBody) {
    return Mono.fromCallable(
            () -> {
              if (REJECTED_CAPS.equalsIgnoreCase(requestBody.getKycStatus())
                  || !Objects.equals(requestBody.getAadhaarXmlValidityStatus(), VERIFIED)
                  || !Objects.equals(requestBody.getAadhaarXmlFaceMatchStatus(), VERIFIED)
                  || !Objects.equals(requestBody.getAadhaarXmlNameMatchStatus(), VERIFIED)) {
                requestBody.setKycStatus(REJECTED_CAPS);
              } else {
                requestBody.setKycStatus(VERIFIED);
              }
              requestBody.setAmlThreshold(null);
              requestBody.setAmlBestMatchName(null);
              requestBody.setAmlBestMatchScore(null);
              requestBody.setAmlStatusCode(null);
              return "aml fields removed from the kyc callback request body";
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error occurred while processing aml fields from the kyc callback request"
                      + " body: {}",
                  KYC_CALLBACK_IDENTIFIER,
                  error.getMessage());
              return Mono.just(
                  "error occurred while processing aml fields from the kyc callback request body");
            });
  }

  public Mono<?> registerKycStatusToPartnerWithMatchVerifiedWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {
    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(KYC_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    // triggering partner api call for callback register
    return partnerApi
        .registerAadhaarXmlCallback(
            requestBody,
            flowData.getPartnerUri(),
            flowData.getCallMethod(),
            partnerCode,
            flowData.getRetryCount())
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            })
        .flatMap(
            data -> {
              // using isRetry field for deciding whether to call cta or not
              // if isRetry -> true, no cta call as it a retried callback
              // if isRetry -> false, then decision will be taken by isCtaCallFlag
              if (!flowData.isCtaCallFlag() || Boolean.TRUE.equals(isRetry)) {
                return Mono.just(data);
              }
              if (Objects.equals(requestBody.getAadhaarXmlValidityStatus(), VERIFIED)
                  && Objects.equals(requestBody.getAadhaarXmlFaceMatchStatus(), VERIFIED)
                  && Objects.equals(requestBody.getAadhaarXmlNameMatchStatus(), VERIFIED)) {
                // triggering m2p api for kyc callback cta register
                return m2PWrapperApi.registerCta(
                    requestBody.getLoanApplicationId(), flowData.getCtaName());
              }
              return Mono.error(
                  new BaseException(KYC_FAIL, getKycFailureReason(requestBody), HttpStatus.OK));
            });
  }

  private List<String> getKycFailureReason(M2pKycCallBackWithAmlRequest requestBody) {
    List<String> failedValidationMessages = new ArrayList<>();
    if (!Objects.equals(requestBody.getAadhaarXmlValidityStatus(), VERIFIED)) {
      failedValidationMessages.add(
          "XML downloaded before" + aadharXmlValidity + "months of application");
    }
    if (!Objects.equals(requestBody.getAadhaarXmlFaceMatchStatus(), VERIFIED)) {
      failedValidationMessages.add("Face match % is below threshold");
    }
    if (!Objects.equals(requestBody.getAadhaarXmlNameMatchStatus(), VERIFIED)) {
      failedValidationMessages.add("Name match % is below threshold");
    }
    return failedValidationMessages;
  }

  public Mono<?> registerOKycWithoutPartnerWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(OKYC_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    String loanId = requestBody.getLoanApplicationId();
    return m2PWrapperApi
        .registerCta(loanId, flowData.getCtaName())
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            });
  }

  public Mono<?> registerOKycStatusToPartnerWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(OKYC_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    Mono<Boolean> funnelActiveAndRejectionOn =
        validationFunnelConfigService.isValidationFunnelActiveAndRejectionIsOn(productCode);
    subscribeAsyncValidationFunnelPersistToM2p(
        requestBody, productCode, funnelActiveAndRejectionOn);
    log.info(
        "[OKYC_CB][FLOW] Starting OKYC callback flow for leadId={}, clientId={}",
        requestBody.getLoanApplicationId(),
        requestBody.getClientId());

    String loanId = requestBody.getLoanApplicationId();
    return deriveOkycRejectionFromValidationFunnelIfNeeded(requestBody, productCode, isRetry)
        .flatMap(
            finalRequestBody ->
                // triggering partner api call for callback register
                partnerApi
                    .registerOkycCallback(
                        finalRequestBody,
                        flowData.getPartnerUri(),
                        flowData.getCallMethod(),
                        partnerCode,
                        flowData.getRetryCount())
                    .flatMap(
                        response -> {
                          callback.setResponse(Json.of(gson.toJson(response)));
                          return callbackStoreService
                              .save(callback)
                              .flatMap(data -> Mono.just(response));
                        })
                    .onErrorResume(
                        error -> {
                          setErrorDataInCallbackEntity(callback, error);
                          return callbackStoreService
                              .save(callback)
                              .flatMap(data -> Mono.error(error));
                        })
                    .flatMap(
                        data -> {
                          if (!flowData.isCtaCallFlag()) {
                            return Mono.just(data);
                          }
                          return m2PWrapperApi
                              .registerCta(loanId, flowData.getCtaName())
                              .flatMap(
                                  ctaResponse ->
                                      Mono.deferContextual(
                                          parentContext -> {
                                            triggerPanAadharAsyncForOkyc(
                                                finalRequestBody, flowData, parentContext);
                                            return Mono.just(ctaResponse);
                                          }))
                              .onErrorResume(
                                  e ->
                                      Mono.deferContextual(
                                          parentContext -> {
                                            triggerPanAadharAsyncForOkyc(
                                                finalRequestBody, flowData, parentContext);
                                            return Mono.error(e);
                                          }));
                        }));
  }

  private Mono<M2pKycCallBackWithAmlRequest> deriveOkycRejectionFromValidationFunnelIfNeeded(
      M2pKycCallBackWithAmlRequest requestBody, String productCode, Boolean isRetry) {

    if (Optional.of(requestBody.getKycStatus().equals(REJECTED_CAPS)).orElse(false)
        && Boolean.FALSE.equals(isRetry)) {
      return Mono.just(requestBody);
    }

    return validationFunnelConfigService
        .isValidationFunnelActiveAndRejectionIsOn(productCode)
        .flatMap(
            isActive -> {
              if (!Boolean.TRUE.equals(isActive)) {
                return Mono.just(requestBody);
              }
              return validationFunnelServiceUtil
                  .readValidationServiceFinalStatus(productCode, requestBody.getClientId())
                  .flatMap(
                      resp -> {
                        if (Objects.nonNull(resp)
                            && resp.getFinalStatus()
                                == ClientValidationFunnelStatus.FinalStatus.REJECT) {
                          requestBody.setKycStatus(REJECTED_CAPS);
                          requestBody.setKycRejectionReason(getRejectionReasons(resp));
                        }
                        return Mono.just(requestBody);
                      })
                  .switchIfEmpty(Mono.just(requestBody));
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[OKYC_CB][VALIDATION_FUNNEL][ERROR] Failed to derive rejection from validation"
                      + " funnel. Proceeding with original request body: {}",
                  e.getMessage(),
                  e);
              return Mono.just(requestBody);
            });
  }

  public Mono<?> registerESignStatusToPartnerWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {
    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(E_SIGN_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    // triggering partner api for callback register
    return partnerApi
        .registerESignCallback(
            requestBody,
            flowData.getPartnerUri(),
            flowData.getCallMethod(),
            partnerCode,
            flowData.getRetryCount())
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            })
        .flatMap(
            data -> {
              // using isRetry field for deciding whether to call cta or not
              // if isRetry -> true, no cta call as it a retried callback
              // if isRetry -> false, then decision will be taken by isCtaCallFlag
              if (!flowData.isCtaCallFlag() || Boolean.TRUE.equals(isRetry)) {
                return Mono.just(data);
              }
              String loanId = requestBody.getLoanApplicationId();
              return m2PWrapperApi.registerCta(loanId, flowData.getCtaName());
            });
  }

  /**
   * Registers e-sign status to partner with M2P CTA, then for CREDIT_LINE_KCL product invokes the
   * credit line status callback.
   */
  public Mono<?> registerESignStatusToPartnerWithM2pCtaAndCreditLineCallback(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {
    return registerESignStatusToPartnerWithM2pCta(
            requestBody, productCode, partnerCode, flowData, isRetry)
        .flatMap(
            data -> {
              if (!CREDIT_LINE_KCL.equals(productCode)) {
                return Mono.just(data);
              }
              log.info(
                  "Invoking processCreditLineStatusCallback for loanApplicationId={},"
                      + " productCode={}, partnerCode={}",
                  requestBody.getLoanApplicationId(),
                  productCode,
                  partnerCode);

              log.info("REQUEST: {}", requestBody);

              CreditLineStatusCallbackRequest creditLineCallbackRequest =
                  CreditLineStatusCallbackRequest.builder()
                      .status(SUCCESS)
                      .loanApplicationId(
                          Optional.ofNullable(requestBody.getLoanApplicationId())
                              .orElse(StringUtils.EMPTY))
                      .productkey(
                          Optional.ofNullable(requestBody.getProductCode())
                              .orElse(StringUtils.EMPTY))
                      .timeStamp(
                          Optional.ofNullable(requestBody.getTimeStamp()).orElse(StringUtils.EMPTY))
                      .build();

              /*
               * <ul>
               *   <li>Calls generate limit API
               *   <li>Calls approve limit API
               *   <li>Calls activate limit API
               *   <li>Updates timestamps in credit_line table
               *   <li>Sends callback to partner
               * </ul>
               */
              return creditLineServiceProvider
                  .getObject()
                  .processCreditLineStatusCallback(creditLineCallbackRequest, productCode)
                  .doOnSuccess(
                      result ->
                          log.info(
                              "Credit Line Successfully Processed and callback sent successfully"
                                  + " for loanApplicationId={}, productCode={}, partnerId={}",
                              requestBody.getLoanApplicationId(),
                              productCode,
                              partnerCode))
                  .onErrorResume(
                      error -> {
                        log.error(
                            "Failed to process the credit line on e-sign callback for"
                                + " loanApplicationId={}, productCode={}, partnerCode={}",
                            requestBody.getLoanApplicationId(),
                            productCode,
                            partnerCode,
                            error);
                        return Mono.just(data);
                      })
                  .thenReturn(data);
            })
        .doOnSuccess(
            result ->
                log.info(
                    "Credit Line Processed and Callback sent successfully for loanApplicationId={},"
                        + " productCode={}, partnerCode={}",
                    requestBody.getLoanApplicationId(),
                    productCode,
                    partnerCode))
        .doOnError(
            error ->
                log.error(
                    "Failure in Credit Line Process and Callback failed for loanApplicationId={},"
                        + " productCode={}, partnerCode={}",
                    requestBody.getLoanApplicationId(),
                    productCode,
                    partnerCode,
                    error));
  }

  public Mono<?> registerBreStatusToPartnerWithM2pCta(
      HashMap<String, Object> requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    String loanId = extractFieldValue(requestBody, "loanId");
    String action = extractFieldValue(requestBody, "action");

    if (APPROVED.equalsIgnoreCase(action)) {
      requestBody.put("action", ELIGIBLE);
    }

    // Building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(BRE_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(loanId);
    callback.setProductCode(productCode);

    // Registering the BRE callback
    Mono<?> registerBreCallbackMono =
        partnerApi
            .registerBreCallback(
                requestBody,
                flowData.getPartnerUri(),
                flowData.getCallMethod(),
                partnerCode,
                flowData.getRetryCount())
            .flatMap(
                response -> {
                  callback.setResponse(Json.of(gson.toJson(response)));
                  return callbackStoreService
                      .save(callback)
                      .then(
                          breStatusService.findByExternalIdAndBreType(
                              loanId, BreType.SANCTION.getDisplayName()))
                      .flatMap(
                          breStatus -> {
                            breStatus.setCallbackId(callback.getId());
                            breStatus.setCallbackId(callback.getId());
                            breStatus.setUpdatedAt(LocalDateTime.now());
                            return breStatusService.save(breStatus);
                          });
                })
            .onErrorResume(
                error -> {
                  setErrorDataInCallbackEntity(callback, error);
                  return callbackStoreService
                      .save(callback)
                      .then(
                          breStatusService.findByExternalIdAndBreType(
                              loanId, BreType.SANCTION.getDisplayName()))
                      .flatMap(
                          breStatus -> {
                            breStatus.setCallbackId(callback.getId());
                            breStatus.setCallbackId(callback.getId());
                            breStatus.setUpdatedAt(LocalDateTime.now());
                            return breStatusService.save(breStatus);
                          });
                });

    // Registering the CTA independently
    Mono<?> registerCtaMono =
        Mono.defer(
            () -> {
              if (action == null) {
                return Mono.empty();
              }

              // Always call registerCta
              if (flowData.isCtaCallFlag()) {
                return m2PWrapperApi
                    .registerCta(loanId, flowData.getCtaName())
                    .then(
                        Mono.defer(
                            () -> {
                              boolean offerDowngrade =
                                  flowData.getConditions() != null
                                      && flowData.getConditions().containsKey("offerDowngrade")
                                      && Boolean.TRUE.equals(
                                          flowData.getConditions().get("offerDowngrade"));
                              // Reject application if not approved or eligible
                              if (!APPROVED.equalsIgnoreCase(action)
                                  && !ELIGIBLE.equalsIgnoreCase(action)) {
                                LoanReject loanReject =
                                    LoanReject.builder()
                                        .reasonCode(reasonCodeFactory.getBreFailReasonCode())
                                        .description("BRE Rejected")
                                        .build();
                                return m2PWrapperApi.rejectLoanApplication(loanReject, loanId);
                              } else {
                                // event publish for BRE approved.
                                publishEventKafkaAsync(
                                    () ->
                                        eventProducerService.publishEvent(
                                            new EventContext(Event.BRE_VERIFIED, loanId),
                                            null,
                                            null));
                              }

                              if (APPROVED.equalsIgnoreCase(action) && offerDowngrade) {
                                return m2PWrapperApi.registerCta(loanId, OFFER_DOWN_CTA_IDENTIFIER);
                              }
                              return Mono.empty();
                            }));
              }

              return Mono.empty();
            });

    // Combine both Mono
    return Mono.deferContextual(
        contextView ->
            registerBreCallbackMono.onErrorResume(error -> Mono.empty()).then(registerCtaMono));
  }

  /**
   * Handles the OKYC status callback from a partner, incorporating AML checks and triggering
   * actions based on the verification and rejection criteria. It processes the OKYC status for a
   * loan application and integrates with various APIs as required, including partner callbacks and
   * CTA (Call to Action) registrations.
   *
   * <p>This method includes the following steps:
   *
   * <ul>
   *   <li>Logs the callback request details and sets up the callback entity.
   *   <li>Processes OKYC rejection scenarios with AML checks and updates the loan application
   *       status accordingly.
   *   <li>Removes sensitive AML-related fields from the callback response.
   *   <li>Registers the Aadhaar XML callback to the partner API, saving the callback response to
   *       the database.
   *   <li>Triggers M2P CTA API for OKYC callback if all conditions are met.
   * </ul>
   *
   * @param requestBody the callback request payload containing OKYC and AML details.
   * @param productCode the product code associated with the callback.
   * @param partnerCode the partner code to identify the external partner.
   * @param flowData the flow configuration data, including partner API URI and retry settings.
   * @param isRetry a flag indicating whether this is a retry attempt.
   * @return a {@link Mono} representing the asynchronous operation, with the result depending on
   *     the final action executed. Possible outcomes:
   *     <ul>
   *       <li>Rejection of the loan application due to OKYC or AML failure.
   *       <li>Successful registration of the Aadhaar XML callback to the partner.
   *       <li>Triggering of the M2P CTA API for further actions, if applicable.
   *     </ul>
   *
   * @throws BaseException if any error occurs during processing, such as OKYC or CTA failures.
   */
  public Mono<?> registerOkycStatusToPartnerWithMatchVerifiedWithAmlWithM2pCta(
      M2pKycCallBackWithAmlRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    CallbackLogEntity callback = getCallbackEntity(OKYC_CALLBACK_IDENTIFIER, isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setReferenceId(requestBody.getLoanApplicationId());
    callback.setProductCode(productCode);

    Mono<Boolean> funnelActiveAndRejectionOn =
        validationFunnelConfigService.isValidationFunnelActiveAndRejectionIsOn(productCode);
    subscribeAsyncValidationFunnelPersistToM2p(
        requestBody, productCode, funnelActiveAndRejectionOn);

    // 1) Partner payload already rejected → loan reject + callback (ends here).
    if (Optional.of(requestBody.getKycStatus().equals(REJECTED_CAPS)).orElse(false)
        && Boolean.FALSE.equals(isRetry)) {
      return rejectLoanAndNotifyPartnerForOkycAlreadyRejected(
          requestBody, callback, partnerCode, flowData);
    }

    // 2) Validation funnel (when enabled) may reject before AML/PEP; otherwise 3) AML/PEP +
    // callback.
    return runOkycValidationFunnelThenAmlPepPipeline(
        requestBody,
        callback,
        productCode,
        partnerCode,
        flowData,
        isRetry,
        funnelActiveAndRejectionOn);
  }

  /**
   * Partner sent REJECTED on the callback payload: reject application and register partner
   * callback.
   */
  private Mono<?> rejectLoanAndNotifyPartnerForOkycAlreadyRejected(
      M2pKycCallBackWithAmlRequest requestBody,
      CallbackLogEntity callback,
      String partnerCode,
      ProductControl.Flow flowData) {

    requestBody.setKycRejectionReason(getOkycFailureReason(requestBody));
    log.info(
        "[OKYC_CB][FLOW] Partner payload already rejected; rejecting loan and notifying partner for"
            + " leadId={}",
        requestBody.getLoanApplicationId());
    return amlAndKycBasedLoanApplicationRejection(
            requestBody.getLoanApplicationId(), requestBody.getKycRejectionReason())
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error while rejecting loan application on the okyc failure: {}",
                  OKYC_CALLBACK_IDENTIFIER,
                  error.getMessage());
              return Mono.error(error);
            })
        .flatMap(
            rejectionResponse ->
                removeAmlFieldsFromOkycCallBackResponseAndSetKycStatus(requestBody)
                    .flatMap(
                        response ->
                            registerPartnerCallback(
                                requestBody,
                                partnerCode,
                                flowData,
                                callback,
                                OKYC_CALLBACK_IDENTIFIER)));
  }

  /**
   * After partner-rejected early exit: resolve funnel outcome, then either funnel reject + callback
   * or AML/PEP + callback ({@link #runOkycAmlPepAndPartnerCallbackStage}).
   */
  private Mono<?> runOkycValidationFunnelThenAmlPepPipeline(
      M2pKycCallBackWithAmlRequest requestBody,
      CallbackLogEntity callback,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry,
      Mono<Boolean> funnelActiveAndRejectionOn) {

    return determineOkycFunnelRejectionResponseIfAny(
            requestBody, productCode, funnelActiveAndRejectionOn)
        .flatMap(
            maybeFunnelRejectResp -> {
              if (maybeFunnelRejectResp.isPresent()) {
                log.info(
                    "[OKYC_CB][FLOW] Validation funnel returned REJECT; skipping AML/PEP and"
                        + " triggering rejection callback for leadId={}",
                    requestBody.getLoanApplicationId());
                return processClientValidationFunnelOkycCallbackReject(
                    maybeFunnelRejectResp.get(),
                    callback,
                    requestBody,
                    productCode,
                    partnerCode,
                    flowData,
                    isRetry);
              }
              log.info(
                  "[OKYC_CB][FLOW] Validation funnel did not reject; proceeding to AML/PEP stage"
                      + " for leadId={}",
                  requestBody.getLoanApplicationId());
              return runOkycAmlPepAndPartnerCallbackStage(
                  requestBody, callback, productCode, partnerCode, flowData, isRetry);
            });
  }

  /**
   * When validation funnel is on and final status is {@code REJECT}, returns that response;
   * otherwise empty (caller runs AML/PEP). Funnel inactive, empty read, or errors yield empty.
   */
  private Mono<Optional<ClientValidationServiceKYCCallbackResponse>>
      determineOkycFunnelRejectionResponseIfAny(
          M2pKycCallBackWithAmlRequest requestBody,
          String productCode,
          Mono<Boolean> funnelActiveAndRejectionOn) {

    return funnelActiveAndRejectionOn
        .flatMap(
            isActive -> {
              if (!Boolean.TRUE.equals(isActive)) {
                log.info(
                    "[OKYC_CB][VALIDATION_FUNNEL] Funnel inactive for productCode={}, leadId={};"
                        + " skipping funnel rejection check.",
                    productCode,
                    requestBody.getLoanApplicationId());
                return Mono.just(Optional.<ClientValidationServiceKYCCallbackResponse>empty());
              }
              log.info(
                  "[OKYC_CB][VALIDATION_FUNNEL] Funnel active for productCode={}, leadId={};"
                      + " reading final status.",
                  productCode,
                  requestBody.getLoanApplicationId());
              return validationFunnelServiceUtil
                  .readValidationServiceFinalStatus(productCode, requestBody.getClientId())
                  .map(
                      resp -> {
                        if (Objects.nonNull(resp)
                            && resp.getFinalStatus()
                                == ClientValidationFunnelStatus.FinalStatus.REJECT) {
                          log.info(
                              "[OKYC_CB][VALIDATION_FUNNEL] Final status REJECT for leadId={}.",
                              requestBody.getLoanApplicationId());
                          return Optional.of(resp);
                        }
                        log.info(
                            "[OKYC_CB][VALIDATION_FUNNEL] Final status is {} for leadId={};"
                                + " continuing AML/PEP path.",
                            Objects.nonNull(resp) ? resp.getFinalStatus() : "NULL",
                            requestBody.getLoanApplicationId());
                        return Optional.<ClientValidationServiceKYCCallbackResponse>empty();
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.error(
                                "[OKYC_CB][VALIDATION_FUNNEL][ERROR] Validation funnel was active"
                                    + " but couldn't get the result from the db.");
                            return Mono.just(Optional.empty());
                          }))
                  .onErrorResume(
                      e -> {
                        log.error(
                            "[OKYC_CB][VALIDATION_FUNNEL][ERROR] Validation funnel read failed;"
                                + " proceeding to AML/PEP: {}",
                            e.getMessage(),
                            e);
                        return Mono.just(Optional.empty());
                      });
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[OKYC_CB][VALIDATION_FUNNEL][ERROR] isValidationFunnelActive check failed;"
                      + " proceeding to AML/PEP: {}",
                  e.getMessage(),
                  e);
              return Mono.just(Optional.empty());
            });
  }

  /** Face/name match → AML/PEP decision → partner callback (and optional CTA). */
  private Mono<?> runOkycAmlPepAndPartnerCallbackStage(
      M2pKycCallBackWithAmlRequest requestBody,
      CallbackLogEntity callback,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {
    if (Objects.equals(requestBody.getAadhaarOkycFaceMatchStatus(), VERIFIED)
        && Objects.equals(requestBody.getAadhaarOkycNameMatchStatus(), VERIFIED)) {

      Mono<M2pKycCallBackWithAmlRequest> enrichedMono;
      if (isAmlPepDecoupledFeatureEnabled(flowData)) {
        enrichedMono =
            enrichKycCallbackWithAmlPep(
                requestBody,
                requestBody.getClientId(),
                getAmlPepConfigOrDefault(flowData),
                productCode);
      } else {
        enrichedMono = Mono.just(requestBody);
      }

      return enrichedMono.flatMap(
          enrichedRequest ->
              evaluateAndHandleAmlPepChecks(enrichedRequest, flowData)
                  .flatMap(
                      decision -> {
                        if (decision.getDecision() == AmlPepDecisionResult.DecisionOutcome.REJECT) {
                          requestBody.setKycStatus(REJECTED_CAPS);
                          requestBody.setKycRejectionReason(
                              List.of(decision.getReasonDescription()));

                          return amlAndKycBasedLoanApplicationRejection(
                                  requestBody.getLoanApplicationId(),
                                  requestBody.getKycRejectionReason())
                              .onErrorResume(
                                  error -> {
                                    log.error(
                                        "[{}] error while rejecting loan application on AML/PEP"
                                            + " failure: {}",
                                        OKYC_CALLBACK_IDENTIFIER,
                                        error.getMessage());
                                    return Mono.error(error);
                                  })
                              .flatMap(
                                  rejectionResponse ->
                                      removeAmlFieldsFromOkycCallBackResponseAndSetKycStatus(
                                              requestBody)
                                          .flatMap(
                                              response ->
                                                  registerPartnerCallback(
                                                      requestBody,
                                                      partnerCode,
                                                      flowData,
                                                      callback,
                                                      OKYC_CALLBACK_IDENTIFIER)));
                        } else {

                          if (flowData.isMClientUpdate()) {
                            loanLevelClientDetailsService
                                .updateMClientOnSuccessfulKYC(
                                    requestBody.getClientId(),
                                    requestBody.getLoanApplicationId(),
                                    requestBody.getProductCode())
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                          }

                          publishEventKafkaAsync(
                              () ->
                                  eventProducerService.publishEvent(
                                      new EventContext(
                                          Event.KYC_VERIFIED,
                                          enrichedRequest.getLoanApplicationId(),
                                          enrichedRequest.getClientId()),
                                      null,
                                      null));

                          if (!flowData.isCtaCallFlag() || Boolean.TRUE.equals(isRetry)) {
                            return removeAmlFieldsFromOkycCallBackResponseAndSetKycStatus(
                                    requestBody)
                                .flatMap(
                                    response ->
                                        registerPartnerCallback(
                                            requestBody,
                                            partnerCode,
                                            flowData,
                                            callback,
                                            OKYC_CALLBACK_IDENTIFIER));
                          }
                          return m2PWrapperApi
                              .registerCta(
                                  requestBody.getLoanApplicationId(), flowData.getCtaName())
                              .flatMap(
                                  ctaResponse ->
                                      removeAmlFieldsFromOkycCallBackResponseAndSetKycStatus(
                                              requestBody)
                                          .flatMap(
                                              response ->
                                                  registerPartnerCallback(
                                                      requestBody,
                                                      partnerCode,
                                                      flowData,
                                                      callback,
                                                      OKYC_CALLBACK_IDENTIFIER))
                                          .onErrorResume(
                                              e ->
                                                  Mono.deferContextual(
                                                      parentContext -> {
                                                        triggerPanAadharAsyncForOkyc(
                                                            enrichedRequest,
                                                            flowData,
                                                            parentContext);
                                                        return Mono.error(e);
                                                      })))
                              .flatMap(
                                  response ->
                                      Mono.deferContextual(
                                          parentContext -> {
                                            triggerPanAadharAsyncForOkyc(
                                                enrichedRequest, flowData, parentContext);
                                            return Mono.just(response);
                                          }));
                        }
                      }));
    }

    return Mono.error(
        new BaseException(OKYC_FAIL, getOkycFailureReason(requestBody), HttpStatus.OK));
  }

  public Mono<?> registerClosureStatusToPartnerWithoutM2pCta(
      M2pLoanClosureCallBackRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    // building a callback log entity
    CallbackLogEntity callback = getCallbackEntity(flowData.getIdentifier(), isRetry);
    callback.setRequest(Json.of(gson.toJson(requestBody)));
    callback.setProductCode(productCode);
    callback.setReferenceId(requestBody.getLoanApplicationId());

    // triggering partner api call for callback register
    return partnerApi
        .registerClosureCallback(
            requestBody,
            flowData.getPartnerUri(),
            flowData.getCallMethod(),
            partnerCode,
            flowData.getRetryCount())
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            });
  }

  private List<String> getOkycFailureReason(M2pKycCallBackWithAmlRequest requestBody) {
    List<String> failedValidationMessages = new ArrayList<>();
    if (!Objects.equals(requestBody.getAadhaarOkycFaceMatchStatus(), VERIFIED)) {
      failedValidationMessages.add("Face match % is below threshold");
    }
    if (!Objects.equals(requestBody.getAadhaarOkycNameMatchStatus(), VERIFIED)) {
      failedValidationMessages.add("Name match % is below threshold");
    }
    return failedValidationMessages;
  }

  private Mono<String> removeAmlFieldsFromOkycCallBackResponseAndSetKycStatus(
      M2pKycCallBackWithAmlRequest requestBody) {
    return Mono.fromCallable(
            () -> {
              if (!Objects.equals(requestBody.getAadhaarOkycNameMatchStatus(), VERIFIED)
                  || !Objects.equals(requestBody.getAadhaarOkycFaceMatchStatus(), VERIFIED)) {
                requestBody.setKycStatus(REJECTED_CAPS);
              } else {
                requestBody.setKycStatus(VERIFIED);
              }
              requestBody.setAmlThreshold(null);
              requestBody.setAmlBestMatchName(null);
              requestBody.setAmlBestMatchScore(null);
              requestBody.setAmlStatusCode(null);
              return "aml fields removed from the okyc callback request body";
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error occurred while processing aml fields from the okyc callback request"
                      + " body: {}",
                  OKYC_CALLBACK_IDENTIFIER,
                  error.getMessage());
              return Mono.just(
                  "error occurred while processing aml fields from the okyc callback request body");
            });
  }

  public Mono<Object> getClientLoansPerformanceReport(
      String clientId, String reportApi, String loanId) {
    return m2PWrapperApi.getClientLoanPerformanceReport(clientId, reportApi, loanId);
  }

  private Mono<Object> registerPartnerCallback(
      M2pKycCallBackWithAmlRequest requestBody,
      String partnerCode,
      ProductControl.Flow flowData,
      CallbackLogEntity callback,
      String loggerHeader) {
    return partnerApi
        .registerPartnerCallback(
            requestBody,
            flowData.getPartnerUri(),
            flowData.getCallMethod(),
            partnerCode,
            flowData.getRetryCount(),
            loggerHeader)
        .flatMap(
            response -> {
              callback.setResponse(Json.of(gson.toJson(response)));
              return callbackStoreService.save(callback).flatMap(data -> Mono.just(response));
            })
        .onErrorResume(
            error -> {
              setErrorDataInCallbackEntity(callback, error);
              return callbackStoreService.save(callback).flatMap(data -> Mono.error(error));
            });
  }

  private CallbackLogEntity getCallbackEntity(String type, Boolean isRetry) {
    return CallbackLogEntity.builder()
        .createdAt(LocalDateTime.now())
        .type(type)
        .isRetry(isRetry)
        .build();
  }

  private void setErrorDataInCallbackEntity(CallbackLogEntity callback, Throwable error) {
    if (error instanceof BaseException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    }
    if (error instanceof ClientSideException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    }
    if (error instanceof ForbiddenException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    }
    if (error instanceof NotFoundException exception) {
      callback.setUri(exception.getUrl());
      callback.setException(exception.toString());
    }
    if (error instanceof ServerErrorException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    }
    if (error instanceof Exception exception) {
      callback.setException(exception.toString());
    }
  }

  /**
   * Adds/Updates data for risk categorization failed cases.
   *
   * @param loanApplicationId: loan application id of failed case.
   */
  public Mono<?> updateRiskCategorizationFailedCase(String loanApplicationId) {
    return riskCategorizationFailureRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            caseData -> {
              caseData.setStatus(FAIL);
              caseData.setLastUpdatedAt(convertEpochMilliToIst(System.currentTimeMillis()));

              log.info(
                  RISK_CATEGORIZATION_RETRY.concat(
                      "[RISK FAILURE][UPDATING] Updating failed case for:{}"),
                  loanApplicationId);
              return riskCategorizationFailureRepository.save(caseData);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  RiskCategorizationFailureEntity newCase = new RiskCategorizationFailureEntity();
                  newCase.setLoanApplicationId(loanApplicationId);
                  newCase.setStatus(FAIL);
                  newCase.setCreatedAt(convertEpochMilliToIst(System.currentTimeMillis()));
                  newCase.setLastUpdatedAt(convertEpochMilliToIst(System.currentTimeMillis()));

                  log.info(
                      RISK_CATEGORIZATION_RETRY.concat(
                          "[RISK FAILURE][ADDING] Adding failed case for:{}"),
                      loanApplicationId);
                  return riskCategorizationFailureRepository.save(newCase);
                }))
        .onErrorResume(
            error -> {
              log.error(
                  RISK_CATEGORIZATION_RETRY.concat(
                      "[RISK FAILURE][ERROR]error while adding/updating risk"
                          + " categorization case:{}"),
                  error.getMessage());
              return Mono.error(error);
            });
  }

  public Mono<?> registerLoanApprovalStatus(M2pLoanApprovalCallBackRequest requestBody) {
    if (loanDataSaveCheck) {
      return partnerMasterService
          .findByProductCode(requestBody.getLosProductKey())
          .flatMap(
              partnerMasterEntity ->
                  getAndSaveDocDetails(
                      requestBody.getLoanApplicationId(),
                      requestBody.getClientId(),
                      partnerMasterEntity.getPartnerId()))
          .doOnSuccess(result -> log.info("getAndSaveDocDetails completed with result: {}", result))
          .doOnError(error -> log.error("Error in getAndSaveDocDetails", error));
    } else {
      log.info("Loan data save check is disabled - no need to save loan data.");
      return Mono.empty();
    }
  }

  public Mono<DocDetailRequest> getLoanAndRpsDetails(String leadId) {
    Mono<JsonNode> repaymentScheduleMono =
        m2PWrapperApi
            .getRepaymentScheduleByLoanId(leadId)
            .flatMap(
                data -> {
                  if (data == null) {
                    return Mono.<JsonNode>error(
                        new RuntimeException("Repayment schedule data is null"));
                  }
                  return Mono.just(objectMapper.valueToTree(data));
                })
            .switchIfEmpty(
                Mono.<JsonNode>error(new RuntimeException("Repayment schedule data is empty")));

    Mono<GetDocketDetailsResponseDto> docketDetailsResponseDtoMono =
        m2PWrapperApi
            .getLoanAndClientDetailsForDocketPopulationByLoanId(leadId)
            .flatMap(
                data -> {
                  if (data == null) {
                    return Mono.<GetDocketDetailsResponseDto>error(
                        new RuntimeException("Docket details data is null"));
                  }
                  return Mono.just(data);
                })
            .switchIfEmpty(
                Mono.<GetDocketDetailsResponseDto>error(
                    new RuntimeException("Docket details data is empty")));

    return Mono.zip(docketDetailsResponseDtoMono, repaymentScheduleMono)
        .map(
            tuple -> {
              GetDocketDetailsResponseDto docketDetails = tuple.getT1();
              JsonNode repaymentScheduleJson = tuple.getT2();
              return DocDetailRequest.builder()
                  .docketDetailsResponseDto(docketDetails)
                  .repaymentSchedule(repaymentScheduleJson)
                  .build();
            });
  }

  public Mono<String> getAndSaveDocDetails(String leadId, String clientId, String partnerId) {
    return getLoanAndRpsDetails(leadId)
        .doOnNext(
            docDetailRequest ->
                log.info("Fetched doc details for leadId={} and clientId={}", leadId, clientId))
        .flatMap(
            docDetailRequest ->
                docSignServiceApi
                    .saveLoanDocDetails(leadId, clientId, docDetailRequest, partnerId)
                    .doOnSuccess(
                        response ->
                            log.info(
                                "Successfully saved doc details for leadId={} and clientId={}",
                                leadId,
                                clientId)))
        .thenReturn(
            String.format(
                "Doc details saved successfully for leadId %s and clientId %s", leadId, clientId))
        .onErrorResume(
            e -> {
              log.error(
                  "Failed to get or save doc details for leadId={} and clientId={}. Error: {}",
                  leadId,
                  clientId,
                  e.toString());
              return Mono.error(e); // propagate error downstream to break flow
            });
  }

  private Mono<Void> updateRiskCategorisationM2pApi(String clientId, String reason) {
    return Mono.deferContextual(
            contextView -> {
              M2pRiskCategorisationCallRequest riskRequest =
                  M2pRiskCategorisationCallRequest.builder()
                      .riskCdRisk(String.valueOf(riskCodeConfig.getUhrId()))
                      .date(DateTimeConverterUtil.getTodayDate())
                      .reason(reason)
                      .locale(EN)
                      .dateFormat(DATE_FORMAT)
                      .build();

              return m2PWrapperApi
                  .getRiskCategorisationTable(clientId)
                  .flatMap(
                      response -> {
                        boolean hasData =
                            response instanceof List && !((List<?>) response).isEmpty();
                        if (hasData) {
                          log.info(
                              "[UPDATE_RISK_CATEGORIZATION] Risk categorisation exists. Updating"
                                  + " (PUT) for clientId={}",
                              clientId);
                          return m2PWrapperApi.updateRiskCategorisationTablePut(
                              clientId, riskRequest);
                        }
                        log.info(
                            "[UPDATE_RISK_CATEGORIZATION] No risk categorisation found. Creating"
                                + " (POST) for clientId={}",
                            clientId);
                        return m2PWrapperApi.updateRiskCategorisationTable(clientId, riskRequest);
                      })
                  .contextWrite(contextView)
                  .then();
            })
        .doOnError(
            e -> log.error("[RISK_CATEGORISATION][ASYNC] Failed for clientId={}", clientId, e))
        .onErrorResume(e -> Mono.empty()); // suppress error
  }

  public Mono<Void> incrementPortfolioRiskParameters(M2pDisbursementCallBackRequest requestBody) {
    return riskParameterService
        .incrementPortfolioRiskParameters(
            String.valueOf(requestBody.getLoanApplicationId()),
            requestBody.getProductCode(),
            requestBody.getNetDisbursement())
        .then();
  }

  public Mono<Void> decrementPortfolioRiskParameters(M2pLoanClosureCallBackRequest requestBody) {
    return riskParameterService
        .decrementPortfolioRiskParameters(
            String.valueOf(requestBody.getLoanApplicationId()),
            requestBody.getProductKey(),
            requestBody.getDisbursedAmount())
        .then();
  }

  private void publishEventKafkaAsync(Runnable eventTask) {
    Mono.fromRunnable(eventTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error while publishing the event", error));
  }

  private String maskPan(String pan) {
    if (Objects.isNull(pan) || pan.length() < 4) {
      return StringUtils.EMPTY;
    }
    int visible = 4;
    return "X".repeat(pan.length() - visible) + pan.substring(pan.length() - visible);
  }

  public Mono<M2pPlatformHealthResponse> getM2pHealthStatus() {
    return m2PWrapperApi.getM2pHealthStatus();
  }
}
