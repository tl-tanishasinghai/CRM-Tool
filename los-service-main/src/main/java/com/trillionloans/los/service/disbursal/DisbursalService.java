package com.trillionloans.los.service.disbursal;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.AUTO;
import static com.trillionloans.los.constant.StringConstants.AUTO_DISBURSAL_ERROR;
import static com.trillionloans.los.constant.StringConstants.BUSINESS_LOAN_CONFIG_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.COMPLETED;
import static com.trillionloans.los.constant.StringConstants.DD_MMMM_YYYY;
import static com.trillionloans.los.constant.StringConstants.DD_MM_YYYY;
import static com.trillionloans.los.constant.StringConstants.DISBURSAL_IN_PROGRESS;
import static com.trillionloans.los.constant.StringConstants.DISBURSEMENT_CONFIG;
import static com.trillionloans.los.constant.StringConstants.EN;
import static com.trillionloans.los.constant.StringConstants.MANUAL;
import static com.trillionloans.los.constant.StringConstants.MANUAL_DISB;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.PRE_DISBURSAL_CHECK_FAILURE;
import static com.trillionloans.los.constant.StringConstants.PRE_DISBURSAL_VALIDATION;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.STATUS;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.TRIGGER_DISB_CTA_IDENTIFIER;

import com.google.gson.Gson;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.TransactionApi;
import com.trillionloans.los.constant.DisbursalStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.mapper.InsuranceStatus;
import com.trillionloans.los.model.dto.AutoDisburseDatatableDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.dto.internal.RuleEvaluationResultDTO;
import com.trillionloans.los.model.entity.DisbursalRegistryEntity;
import com.trillionloans.los.model.request.AutoDisbursalCallbackRequest;
import com.trillionloans.los.model.request.AutoDisbursalRequest;
import com.trillionloans.los.model.request.m2p.LoanClassificationDetailsM2pRequest;
import com.trillionloans.los.model.request.m2p.M2pInitiateDisbursalDTO;
import com.trillionloans.los.model.request.m2p.M2pLoanDisburseRequestDTO;
import com.trillionloans.los.model.response.GetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.DisbursalAmountResponse;
import com.trillionloans.los.repository.LoanClassificationReportingRepository;
import com.trillionloans.los.repository.LoanInsuranceDetailsRepository;
import com.trillionloans.los.repository.PartnerMasterRepository;
import com.trillionloans.los.service.BusinessLoanEvaluationService;
import com.trillionloans.los.service.CustomRuleEngineService;
import com.trillionloans.los.service.InsuranceService;
import com.trillionloans.los.service.LoanApplicationService;
import com.trillionloans.los.service.db.DisbursalRegistryStoreService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/** Service to manage the disbursal process for loans, including auto and manual disbursements. */
@RequiredArgsConstructor
@Service
@Slf4j
public class DisbursalService {

  private final LoanApplicationService loanApplicationService;
  private final Gson gson;
  private final CustomRuleEngineService ruleEngineService;
  private final TransactionApi transactionApi;
  private final ProductConfigMasterService productConfigMasterService;
  private final M2PWrapperApi m2pWrapperApi;
  private static final String APPLICATION_ID_LOG_LITERAL = " application id: {}";
  private final InsuranceService insuranceService;
  private final LoanInsuranceDetailsRepository loanInsuranceDetailsRepository;
  private final DisbursalRegistryStoreService disbursalRegistryStoreService;
  private final PartnerMasterRepository partnerMasterRepository;
  private final BusinessLoanEvaluationService businessLoanEvaluationService;
  private final LoanClassificationReportingRepository loanClassificationReportingRepository;

  public Mono<?> triggerDisbursement(
      String loanApplicationId, String clientId, String productCode) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlData -> {
              ProductControl.Flow businessLoanConfig =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlData.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
              boolean isBusinessLoanEnabled =
                  businessLoanConfig != null
                      && Boolean.TRUE.equals(businessLoanConfig.getIsBusinessLoan());

              return loanApplicationService
                  .getPanAadhaarLinkage(productCode, loanApplicationId)
                  .flatMap(
                      linkage ->
                          handleDisbursement(
                              loanApplicationId, clientId, productCode, productControlData))
                  .doOnSuccess(
                      response -> {
                        if (isBusinessLoanEnabled) {
                          asyncMarkLeadTerminalClassificationStatusAndPostM2pData(
                              clientId, loanApplicationId);
                        }
                      });
            });
  }

  /**
   * Marks the loan as MERCHANT_LOAN asynchronously after successful disbursement. Fetches
   * productCode from the loan (M2P) and saves it on the classification row. Fire-and-forget.
   */
  private void asyncMarkLeadTerminalClassificationStatusAndPostM2pData(
      String clientId, String loanId) {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    m2pWrapperApi
        .getLoanApplicationByLoanId(loanId)
        .map(
            response -> {
              if (response instanceof Map) {
                Object productKeyObj = ((Map<?, ?>) response).get("losProductKey");
                return productKeyObj != null ? String.valueOf(productKeyObj).trim() : null;
              }
              return null;
            })
        .flatMap(
            productCode ->
                businessLoanEvaluationService
                    .markAsTerminalLoanAtDisburse(loanId, productCode)
                    .then(syncLoanClassificationDetailsToM2p(clientId, loanId)))
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(
            result -> {
              if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
              }
              log.info(
                  "[LOAN_DISB] Successfully processed loan type classification for loanId: {}",
                  loanId);
            })
        .doOnError(
            error -> {
              if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
              }
              log.error(
                  "[LOAN_DISB] Failed to mark loan type for loanId: {}, error: {}",
                  loanId,
                  error.getMessage());
            })
        .subscribe();
  }

  /**
   * Loads classification/evaluation rows for the loan and POSTs them to M2P. Errors are logged and
   * swallowed so disbursement side-effects are not failed by M2P sync.
   */
  private Mono<Void> syncLoanClassificationDetailsToM2p(String clientId, String loanApplicationId) {
    return loanClassificationReportingRepository
        .findClassificationLinesForLoan(loanApplicationId)
        .collectList()
        .flatMap(
            lines -> {
              if (lines.isEmpty()) {
                return Mono.empty();
              }
              return Flux.fromIterable(lines)
                  .map(
                      line ->
                          LoanClassificationDetailsM2pRequest.fromClassificationLine(
                              loanApplicationId, line))
                  .filter(Objects::nonNull)
                  .switchIfEmpty(
                      Flux.defer(
                          () -> {
                            log.warn(
                                "[LOAN_DISB] Skipped M2P classification sync: lead_id is not"
                                    + " numeric for loanId: {}",
                                loanApplicationId);
                            return Flux.empty();
                          }))
                  .concatMap(
                      body ->
                          m2pWrapperApi
                              .postLoanClassificationDetailsToM2p(clientId, body)
                              .doOnSuccess(
                                  r ->
                                      log.info(
                                          "[LOAN_DISB] Posted loan classification row to M2P for"
                                              + " loanId: {}, documentName: {}",
                                          loanApplicationId,
                                          body.getDocumentName()))
                              .doOnError(
                                  e ->
                                      log.error(
                                          "[LOAN_DISB] Failed posting classification row to M2P for"
                                              + " loanId: {}, documentName: {}",
                                          loanApplicationId,
                                          body.getDocumentName(),
                                          e))
                              .onErrorResume(e -> Mono.empty()))
                  .then();
            });
  }

  private Mono<?> handleDisbursement(
      String loanApplicationId,
      String clientId,
      String productCode,
      Tuple2<String, ProductControl> productControlData) {
    ProductControl.Flow flowData =
        productConfigMasterService.getFlowFromProductConfig(
            productControlData.getT2(), TRIGGER_DISB_CTA_IDENTIFIER);
    return loanApplicationService
        .getLoanApplicationByLoanIdV2(loanApplicationId)
        .flatMap(
            response -> {
              if (Objects.equals(response.getLoanApplicationStatus(), "APPLICATION_CREATED")
                  || Objects.equals(response.getLoanApplicationStatus(), "APPLICATION_REJECTED")
                  || Objects.equals(response.getLoanApplicationStatus(), "LOAN_CLOSED")) {
                log.error(
                    "[{}] [ERROR] trying to disburse an un-approved/closed/rejected application,"
                        + " loan application: {}",
                    DISBURSEMENT_CONFIG,
                    loanApplicationId);
                return Mono.error(
                    new ForbiddenException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        "trying to disburse an un-approved/closed/rejected loan application",
                        HttpStatus.FORBIDDEN));
              }
              if (Objects.equals(response.getLoanApplicationStatus(), "APPLICATION_DISBURSE")
                  || Objects.equals(response.getLoanApplicationStatus(), "DISBURSE_INITIATED")) {
                log.info(
                    "[{}] idempotent success - loan already disbursed or disbursal in progress,"
                        + " loan application: {}",
                    DISBURSEMENT_CONFIG,
                    loanApplicationId);
                return Mono.just(Map.of(STATUS, SUCCESS));
              }
              if (Objects.isNull(flowData)) {
                log.error(
                    "[{}] [ERROR] no flow data found for the disbursement handling for loan"
                        + APPLICATION_ID_LOG_LITERAL,
                    DISBURSEMENT_CONFIG,
                    loanApplicationId);
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }
              if (insuranceService.isInsuranceFeatureEnabled(flowData)) {
                loanInsuranceDetailsRepository
                    .findFirstByLoanApplicationIdOrderByIdDesc(Integer.parseInt(loanApplicationId))
                    .flatMap(
                        entity -> {
                          if (Boolean.TRUE.equals((entity.getIsOpted()))) {
                            entity.setStatus(InsuranceStatus.PENDING.name());
                            return loanInsuranceDetailsRepository
                                .save(entity)
                                .doOnSuccess(
                                    updatedEntity ->
                                        log.info(
                                            "[INSURANCE] [VERIFY] [SUCCESS] insurance opted updated"
                                                + " for loanApplicationId: {}",
                                            loanApplicationId));
                          } else {
                            log.info(
                                "[INSURANCE] [VERIFY] [SKIP] no update needed for"
                                    + " loanApplicationId: {}",
                                loanApplicationId);
                            return Mono.just(entity);
                          }
                        })
                    .switchIfEmpty(
                        Mono.fromRunnable(
                            () ->
                                log.info(
                                    "[INSURANCE] [VERIFY] [NOT_FOUND] no insurance record for"
                                        + " loanApplicationId: {}",
                                    loanApplicationId)))
                    .subscribe(
                        success -> {},
                        error ->
                            log.error(
                                "[INSURANCE] [VERIFY] [ERROR] error updating insurance for"
                                    + " loanApplicationId: {}, error: {}",
                                loanApplicationId,
                                error.getMessage()));
              }
              log.info(
                  "[{}] pre-disbursal validations initiated for loan application id: {}",
                  PRE_DISBURSAL_VALIDATION,
                  loanApplicationId);
              return loanApplicationService
                  .processPreDisbursementQcChecks(
                      flowData, loanApplicationId, clientId, productCode)
                  .flatMap(
                      qcChecksResponse -> {
                        if (Boolean.TRUE.equals(qcChecksResponse)) {
                          return Mono.error(
                              new ForbiddenException(
                                  PRE_DISBURSAL_CHECK_FAILURE,
                                  PRE_DISBURSAL_CHECK_FAILURE,
                                  HttpStatus.FORBIDDEN));
                        } else {
                          return flowData.isNewDisbursementFlow()
                              ? handleDisbursementV2WithAutoDisbursementFlow(
                                  loanApplicationId, clientId, productCode, flowData, response)
                              : handleDisbursementV1(loanApplicationId, clientId, flowData);
                        }
                      });
            });
  }

  /**
   * Checks whether a CTA (Call-To-Action) is required based on the flow configuration. If required,
   * it triggers the loan disbursement process, otherwise returns an error response.
   *
   * @param loanApplicationId Loan ID.
   * @param clientId Lead ID.
   * @param flowData Flow configuration that determines if CTA is required.
   * @return Mono signaling the result of CTA handling (disbursement or error).
   */
  private Mono<?> handleDisbursementV1(
      String loanApplicationId, String clientId, ProductControl.Flow flowData) {
    log.info(
        "[{}] moving ahead with disbursement v1, client id: {}, loan application id: {}",
        DISBURSEMENT_CONFIG,
        clientId,
        loanApplicationId);
    if (flowData.isCtaCallFlag()) {
      return m2pWrapperApi.registerCta(loanApplicationId, flowData.getCtaName());
    }
    return Mono.error(
        new BaseException(
            SOMETHING_WENT_WRONG_CONFIG,
            SOMETHING_WENT_WRONG_CONFIG,
            HttpStatus.INTERNAL_SERVER_ERROR));
  }

  /**
   * Processes new disbursement flows based on loan details and flow configuration.
   *
   * @param loanApplicationId Loan ID.
   * @param clientId Lead ID.
   * @param productCode Product Code.
   * @param flowData Flow configuration.
   * @param response
   * @return Mono signaling completion or error.
   */
  private Mono<?> handleDisbursementV2WithAutoDisbursementFlow(
      String loanApplicationId,
      String clientId,
      String productCode,
      ProductControl.Flow flowData,
      GetLoanV2ResponseDTO response) {
    log.info(
        "[{}] moving ahead with disbursement v2, client id: {}, loan application id: {}",
        DISBURSEMENT_CONFIG,
        clientId,
        loanApplicationId);
    return loanApplicationService
        .getBankDetailsFromLoanLevelDataTable(loanApplicationId)
        .flatMap(
            bankDetails ->
                validateBankDetails(bankDetails)
                    .flatMap(
                        bankAccountId ->
                            initiateDisbursementV2(
                                loanApplicationId,
                                clientId,
                                bankAccountId,
                                productCode,
                                flowData,
                                response)));
  }

  /**
   * Validates bank details for the disbursement.
   *
   * @param bankDetailsMap Map of bank details.
   * @return Mono with the bank ID or an error.
   */
  private Mono<String> validateBankDetails(Map<String, String> bankDetailsMap) {
    if (!bankDetailsMap.isEmpty()) {
      return Mono.just(bankDetailsMap.get("bank_id"));
    }
    return Mono.error(
        new BaseException(
            "loan disbursal failed, bank account not mapped",
            "loan disbursal failed, bank account not mapped",
            HttpStatus.BAD_REQUEST));
  }

  private M2pInitiateDisbursalDTO createM2pInitiateDisbursementDTO(String bankId) {
    M2pInitiateDisbursalDTO dto = new M2pInitiateDisbursalDTO();
    dto.setPaymentTypeId(1);
    dto.setDateFormat(DD_MMMM_YYYY);
    LocalDate today = LocalDate.now(ZoneId.of(ASIA_KOLKATA));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DD_MMMM_YYYY);
    dto.setExpectedDisbursementDate(today.format(formatter));
    dto.setBankAccountDetailId(bankId);
    return dto;
  }

  private Mono<?> initiateDisbursementV2(
      String loanApplicationId,
      String clientId,
      String bankId,
      String productCode,
      ProductControl.Flow flowData,
      GetLoanV2ResponseDTO loanApplicationData) {
    return m2pWrapperApi
        .registerCta(loanApplicationId, flowData.getCtaName())
        .flatMap(
            ctaResponse ->
                disbursalRegistryStoreService
                    .findByReferenceId1(loanApplicationId, false)
                    .flatMap(
                        disburseTransaction -> {
                          // if status is manual -> triggerManual
                          // if status is auto, then no triggering for auto allowed
                          if (Objects.equals(disburseTransaction.getDisburseType(), MANUAL)) {
                            log.info(
                                "[{}] [RE_TRIGGER] re-triggering manual disbursement, client id:"
                                    + " {}, loan"
                                    + APPLICATION_ID_LOG_LITERAL,
                                DISBURSEMENT_CONFIG,
                                clientId,
                                loanApplicationId);
                            return processManualDisbursal(
                                    loanApplicationId, productCode, bankId, null)
                                .thenReturn(ctaResponse);
                          } else if (Objects.equals(disburseTransaction.getDisburseType(), AUTO)) {
                            return Mono.error(
                                new ForbiddenException(
                                    DISBURSAL_IN_PROGRESS,
                                    DISBURSAL_IN_PROGRESS,
                                    HttpStatus.FORBIDDEN));
                          }
                          return Mono.error(
                              new BaseException(
                                  "something went wrong while disbursement",
                                  "something went wrong while disbursement - table",
                                  HttpStatus.INTERNAL_SERVER_ERROR));
                        })
                    .switchIfEmpty(
                        Mono.defer(
                            () -> {
                              boolean isAutoDisbursementEnabled =
                                  isIsAutoDisbursementEnabled(flowData);
                              if (isAutoDisbursementEnabled) {
                                log.info(
                                    "[{}] [FRESH] moving ahead with auto disbursement as flag"
                                        + " enabled, client id: {}, loan"
                                        + APPLICATION_ID_LOG_LITERAL,
                                    DISBURSEMENT_CONFIG,
                                    clientId,
                                    loanApplicationId);
                                return performAllValidations(
                                        loanApplicationId, clientId, productCode, bankId, flowData)
                                    .flatMap(
                                        validationResponse -> {
                                          log.info(
                                              "[AUTO_DISBURSAL_VALIDATIONS] validation result: {}"
                                                  + " for auto-disbursal for loan application id:"
                                                  + " {}, client id: {}, bank id: {}",
                                              validationResponse,
                                              loanApplicationId,
                                              clientId,
                                              bankId);
                                          if (Boolean.TRUE.equals(validationResponse.getT1())) {
                                            return saveDisbursalRegistryEntity(
                                                    clientId,
                                                    loanApplicationId,
                                                    productCode,
                                                    bankId,
                                                    AUTO,
                                                    DisbursalStatus.AUTO_INI,
                                                    null,
                                                    null)
                                                .flatMap(
                                                    entity ->
                                                        startAutoDisbursalFlow(
                                                                loanApplicationId,
                                                                clientId,
                                                                bankId,
                                                                productCode,
                                                                flowData)
                                                            .thenReturn(ctaResponse));
                                          }
                                          log.info(
                                              "[{}] [FRESH] moving ahead with manual disbursement"
                                                  + " as validations failed for auto-disbursal,"
                                                  + " client id: {}, loan"
                                                  + APPLICATION_ID_LOG_LITERAL,
                                              DISBURSEMENT_CONFIG,
                                              clientId,
                                              loanApplicationId);
                                          return saveDisbursalRegistryEntity(
                                                  clientId,
                                                  loanApplicationId,
                                                  productCode,
                                                  bankId,
                                                  MANUAL,
                                                  DisbursalStatus.INIT,
                                                  loanApplicationData.getApprovedAmount(),
                                                  loanApplicationData.getNetDisburseAmount())
                                              .flatMap(
                                                  entity ->
                                                      processManualDisbursal(
                                                              loanApplicationId,
                                                              productCode,
                                                              bankId,
                                                              "AUTO_DISBURSAL_NSTP_VALIDATIONS_FAILED:"
                                                                  + " "
                                                                  + validationResponse.getT2())
                                                          .thenReturn(ctaResponse));
                                        });
                              }
                              log.info(
                                  "[{}] [FRESH] moving ahead with manual disbursement, client id:"
                                      + " {}, loan"
                                      + APPLICATION_ID_LOG_LITERAL,
                                  DISBURSEMENT_CONFIG,
                                  clientId,
                                  loanApplicationId);
                              return saveDisbursalRegistryEntity(
                                      clientId,
                                      loanApplicationId,
                                      productCode,
                                      bankId,
                                      MANUAL,
                                      DisbursalStatus.INIT,
                                      loanApplicationData.getApprovedAmount(),
                                      loanApplicationData.getNetDisburseAmount())
                                  .flatMap(
                                      entity ->
                                          processManualDisbursal(
                                                  loanApplicationId,
                                                  productCode,
                                                  bankId,
                                                  "AUTO_DISBURSAL_NOT_ENABLED")
                                              .thenReturn(ctaResponse));
                            })));
  }

  public Mono<Tuple2<Boolean, String>> performAllValidations(
      String loanApplicationId,
      String clientId,
      String productCode,
      String bankId,
      ProductControl.Flow flowData) {

    boolean validationsEnabled =
        Objects.nonNull(flowData.getConditions())
            && (boolean) flowData.getConditions().getOrDefault("validationsEnabled", false);

    if (!validationsEnabled) {
      log.info(
          "[AUTO_DISBURSAL_VALIDATIONS] auto-disbursal validations are disabled for product code:"
              + " {}",
          productCode);
      return Mono.just(Tuples.of(true, ""));
    }

    log.info(
        "[AUTO_DISBURSAL_VALIDATIONS] auto-disbursal validations are enabled for product code: {}",
        productCode);

    Mono<Boolean> nsdlPanMono =
        loanApplicationService
            .getNsdlPanValidationStatus(productCode, clientId)
            .doOnSuccess(
                result ->
                    log.info(
                        "[AUTO_DISBURSAL_VALIDATIONS] [NSDL_PAN] result: {} for clientId: {},"
                            + " productCode: {}",
                        result,
                        clientId,
                        productCode));

    Mono<Boolean> pennyDropMono =
        loanApplicationService
            .getPennyDropStatus(clientId, bankId)
            .doOnSuccess(
                result ->
                    log.info(
                        "[AUTO_DISBURSAL_VALIDATIONS] [PENNY_DROP] result: {} for clientId: {},"
                            + " bankId: {}",
                        result,
                        clientId,
                        bankId));

    Mono<Boolean> amlPepMono =
        loanApplicationService
            .getAmlPepStatusFromM2p(loanApplicationId)
            .doOnSuccess(
                result ->
                    log.info(
                        "[AUTO_DISBURSAL_VALIDATIONS] [AML_PEP] result: {} for loanApplicationId:"
                            + " {}",
                        result,
                        loanApplicationId));

    Mono<Boolean> panAadhaarLinkageStatusMono =
        loanApplicationService
            .getPanAadhaarLinkageStatus(productCode, loanApplicationId)
            .doOnSuccess(
                result ->
                    log.info(
                        "[AUTO_DISBURSAL_VALIDATIONS] [PAN_AADHAAR_LINKAGE] result: {} for"
                            + " loanApplicationId: {}, productCode: {}",
                        result,
                        loanApplicationId,
                        productCode));

    return Mono.zip(nsdlPanMono, pennyDropMono, amlPepMono, panAadhaarLinkageStatusMono)
        .map(
            results -> {
              boolean finalResult =
                  results.getT1() && results.getT2() && results.getT3() && results.getT4();

              String finalResultString =
                  String.format(
                      "final combined result: [NSDL_PAN: %s, PENNY_DROP: %s, AML_PEP: %s,"
                          + " PAN_AADHAAR_LINKAGE: %s]",
                      results.getT1(), results.getT2(), results.getT3(), results.getT4());

              log.info(
                  "[AUTO_DISBURSAL_VALIDATIONS] final combined result: {} [NSDL_PAN: {},"
                      + " PENNY_DROP: {}, AML_PEP: {}, PAN_AADHAAR_LINKAGE: {}]",
                  finalResult,
                  results.getT1(),
                  results.getT2(),
                  results.getT3(),
                  results.getT4());

              return Tuples.of(finalResult, finalResultString);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[AUTO_DISBURSAL_VALIDATIONS] error while executing parallel validations", error);
              return Mono.just(Tuples.of(false, "error while executing parallel validations"));
            });
  }

  private static boolean isIsAutoDisbursementEnabled(ProductControl.Flow flowData) {
    return Objects.nonNull(flowData.getConditions())
        && (boolean) flowData.getConditions().getOrDefault("autoDisbursementFlag", false);
  }

  private Mono<?> startAutoDisbursalFlow(
      String loanApplicationId,
      String clientId,
      String bankId,
      String productCode,
      ProductControl.Flow flowData) {
    return loanApplicationService
        .getLoanApplicationByLoanId(loanApplicationId)
        .flatMap(
            loanData ->
                getAutoDisbursalResult(loanData, productCode)
                    .flatMap(
                        result -> {
                          if (Boolean.TRUE.equals(result)) {
                            log.info(
                                "[{}] rules valid for auto-disbursement for client id: {}, loan"
                                    + APPLICATION_ID_LOG_LITERAL,
                                DISBURSEMENT_CONFIG,
                                clientId,
                                loanApplicationId);
                            return processAutoDisbursal(
                                loanApplicationId,
                                clientId,
                                bankId,
                                productCode,
                                loanData,
                                flowData);
                          } else {
                            log.info(
                                "[{}] rules not valid for auto-disbursement for client id: {}, loan"
                                    + " application id: {}, and manual allowed",
                                DISBURSEMENT_CONFIG,
                                clientId,
                                loanApplicationId);
                            return processManualDisbursal(
                                loanApplicationId,
                                productCode,
                                bankId,
                                "AUTO_DISBURSAL_RULES_NOT_VALID");
                          }
                        }))
        .switchIfEmpty(
            Mono.error(
                new BaseException(
                    "loan application not found: " + loanApplicationId,
                    "loan application not found: " + loanApplicationId,
                    HttpStatus.NOT_FOUND)));
  }

  private Mono<Boolean> getAutoDisbursalResult(Object loanData, String productCode) {
    Map<String, Object> facts = new HashMap<>();
    facts.put("loanApplication", loanData);
    RuleEvaluationResultDTO evaluationResult = new RuleEvaluationResultDTO();
    facts.put("evaluationResult", evaluationResult);
    return ruleEngineService
        .executeRules(facts, "autoDisbursal", productCode)
        .flatMap(
            result -> {
              Boolean autoDisbursal = getAutoDisbursalResult(result);
              if (Objects.nonNull(autoDisbursal)) {
                return Boolean.TRUE.equals(autoDisbursal) ? Mono.just(true) : Mono.just(false);
              } else {
                return Mono.just(false);
              }
            });
  }

  private Boolean getAutoDisbursalResult(RuleEvaluationResultDTO result) {
    RuleEvaluationResultDTO modifiedEvaluationResult =
        (RuleEvaluationResultDTO) result.getResults().get("evaluationResult");
    return (Boolean) modifiedEvaluationResult.getResult("autoDisbursal");
  }

  /**
   * Handles the automatic disbursement process.
   *
   * @param loanApplicationId Loan ID.
   * @param clientId Lead ID.
   * @param productCode Product Code.
   * @param loanData Loan data to process.
   * @param flowData Flow configuration for the product.
   * @return Mono signaling the completion of the auto disbursal process or failure.
   */
  @SuppressWarnings("unchecked")
  private Mono<?> processAutoDisbursal(
      String loanApplicationId,
      String clientId,
      String bankId,
      String productCode,
      Object loanData,
      ProductControl.Flow flowData) {
    Map<String, Object> loanDataMap = (Map<String, Object>) loanData;
    Double netDisburseAmount = (Double) loanDataMap.get("netDisbursementAmount");
    return loanApplicationService
        .getBankDetailsFromLoanLevelDataTable(loanApplicationId)
        .flatMap(
            bankData ->
                prepareAutoDisbursalTransactionRequest(
                    loanApplicationId, netDisburseAmount, flowData, bankData, productCode))
        .flatMap(
            requestBody -> transactionApi.autoDisburse(requestBody, loanApplicationId, clientId))
        .flatMap(
            response -> {
              if (response instanceof Map) {
                Map<String, String> responseMap = (Map<String, String>) response;
                String status = responseMap.get(STATUS);
                if (SUCCESS.equalsIgnoreCase(status)) {
                  return updateTransactionStatusInDisbursalRegistry(
                          loanApplicationId,
                          DisbursalStatus.AUTO_INI,
                          AUTO,
                          productCode,
                          null,
                          null,
                          null)
                      .flatMap(
                          savedTransaction ->
                              m2pWrapperApi.addAutoDisbursalStatusInDatatable(
                                  getAutoDisburseDatatableRequestBody(true), loanApplicationId));
                } else {
                  return processManualDisbursal(
                      loanApplicationId, productCode, bankId, "AUTO_DISBURSAL_FAILED_FROM_VENDOR");
                }
              }
              return Mono.error(
                  new BaseException(
                      AUTO_DISBURSAL_ERROR,
                      "invalid response from auto-disbursal api from bank transaction service",
                      HttpStatus.INTERNAL_SERVER_ERROR));
            })
        .onErrorResume(
            e -> {
              if (e instanceof ClientSideException clientSideException) {
                ResponseDTO<String> response =
                    gson.fromJson(
                        gson.toJson(clientSideException.getClientResponse()), ResponseDTO.class);
                if (Objects.equals(response.getMessage(), "transaction already exists")) {
                  log.error(
                      "[AUTO_DISB] transaction already exists for loan application id: {}, with"
                          + " error: {}",
                      loanApplicationId,
                      e.getMessage());
                  return Mono.error(
                      new ForbiddenException(
                          DISBURSAL_IN_PROGRESS, DISBURSAL_IN_PROGRESS, HttpStatus.FORBIDDEN));
                }
              }
              log.error(
                  "[AUTO_DISB] error during auto disbursal for loan application id: {}, with error:"
                      + " {}",
                  loanApplicationId,
                  e.getMessage(),
                  e);
              log.info(
                  "[AUTO_DISB] auto disbursal failed, proceeding with manual disbursal for loan"
                      + " application id: {}, client id: {}",
                  loanApplicationId,
                  clientId);
              return processManualDisbursal(
                  loanApplicationId, productCode, bankId, "AUTO_DISBURSAL_FAILED");
            });
  }

  /**
   * Prepares the auto disbursal transaction request using loan details, flow configuration, and
   * bank data.
   *
   * @param loanApplicationId Loan ID.
   * @param netDisburseAmount Amount to be disbursed.
   * @param flowData Flow configuration for the product.
   * @param bankData Bank details for the beneficiary.
   * @return Mono containing the prepared auto disbursal request.
   */
  private Mono<AutoDisbursalRequest> prepareAutoDisbursalTransactionRequest(
      String loanApplicationId,
      Double netDisburseAmount,
      ProductControl.Flow flowData,
      Map<String, String> bankData,
      String productCode) {

    AutoDisbursalRequest request =
        AutoDisbursalRequest.builder()
            .systemExternalId(loanApplicationId)
            .partnerId(MDC.get(PARTNER_ID))
            .paymentProvider((String) flowData.getConditions().get("paymentProvider"))
            .paymentMode((String) flowData.getConditions().get("paymentMode"))
            .amount(netDisburseAmount)
            .sourceAccountId((String) flowData.getConditions().get("sourceAccountId"))
            .initiatedBy("system")
            .approvedBy("system")
            .productCode(productCode)
            .beneficiaryDetails(
                AutoDisbursalRequest.BeneficiaryDetailsDTO.builder()
                    .bankAccountId(bankData.get("bank_id"))
                    .accountHolderName(bankData.get("account_holder_name"))
                    .accountNumber(bankData.get("bank_account_number"))
                    .accountType(bankData.get("account_type"))
                    .bankIfscCode(bankData.get("ifsc_code"))
                    .build())
            .build();

    return Mono.just(request);
  }

  private Mono<?> processManualDisbursal(
      String loanApplicationId, String productCode, String bankId, String reason) {
    log.info(
        "[{}] proceeding with the manual disbursement for the loan application id: {}",
        MANUAL_DISB,
        loanApplicationId);
    return m2pWrapperApi
        .addAutoDisbursalStatusInDatatable(
            getAutoDisburseDatatableRequestBody(false), loanApplicationId)
        .flatMap(
            status -> initiateLoanDisbursalAtM2p(loanApplicationId, productCode, bankId, reason));
  }

  private Mono<?> initiateLoanDisbursalAtM2p(
      String loanApplicationId, String productCode, String bankId, String reason) {
    return m2pWrapperApi
        .initiateLoanDisbursement(loanApplicationId, createM2pInitiateDisbursementDTO(bankId))
        .flatMap(
            apiResponse -> {
              log.info(
                  "[{}] success response from m2p for initiate disbursement call: {}",
                  MANUAL_DISB,
                  loanApplicationId);
              return loanApplicationService
                  .getLoanApplicationByLoanIdV2(loanApplicationId)
                  .flatMap(
                      initiatedDisburseLoanDetails -> {
                        log.info(
                            "[{}] success response from m2p for get loan details call: {}",
                            MANUAL_DISB,
                            loanApplicationId);

                        return partnerMasterRepository
                            .findByProductCodeAndIsRemitXEnabled(productCode, true)
                            .map(partner -> DisbursalStatus.MANUAL_INI)
                            .defaultIfEmpty(DisbursalStatus.MANUAL_M2P)
                            .flatMap(
                                status ->
                                    m2pWrapperApi
                                        .getDisbursalAmount(loanApplicationId)
                                        .take(1)
                                        .collectList()
                                        .flatMap(
                                            rows -> {
                                              if (rows.isEmpty()) {
                                                return updateTransactionStatusInDisbursalRegistry(
                                                    loanApplicationId,
                                                    status,
                                                    MANUAL,
                                                    productCode,
                                                    reason,
                                                    initiatedDisburseLoanDetails
                                                        .getNetDisburseAmount(),
                                                    initiatedDisburseLoanDetails
                                                        .getNetDisburseAmount());
                                              }
                                              DisbursalAmountResponse resp = rows.get(0);
                                              return updateTransactionStatusInDisbursalRegistry(
                                                  loanApplicationId,
                                                  status,
                                                  MANUAL,
                                                  productCode,
                                                  reason,
                                                  resp.getGrossDisbursalAmount(),
                                                  resp.getNetDisbursalAmount());
                                            }));
                      });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] fail response from m2p for initiate disbursement call: {}",
                  "MANUAL_DISB_ERROR",
                  loanApplicationId);
              return updateTransactionStatusInDisbursalRegistry(
                      loanApplicationId,
                      DisbursalStatus.REJECTED,
                      MANUAL,
                      productCode,
                      null,
                      null,
                      null)
                  .flatMap(response -> Mono.error(error));
            });
  }

  private AutoDisburseDatatableDTO getAutoDisburseDatatableRequestBody(boolean autoDisbursed) {
    return AutoDisburseDatatableDTO.builder()
        .autoDisbursed(autoDisbursed)
        .locale(EN)
        .dateFormat(DD_MMMM_YYYY)
        .build();
  }

  public Mono<?> updateTransactionStatusInDisbursalRegistry(
      String loanApplicationId,
      DisbursalStatus disburseStatus,
      String disburseType,
      String productCode,
      String reason,
      BigDecimal grossDisbursementAmount,
      BigDecimal netDisburseAmount) {
    DisbursalRegistryEntity disbursalRegistryEntity =
        DisbursalRegistryEntity.builder()
            .referenceId1(loanApplicationId)
            .productCode(productCode)
            .disburseType(disburseType)
            .disburseStatus(disburseStatus)
            .build();

    if (grossDisbursementAmount != null)
      disbursalRegistryEntity.setGrossDisbursalAmount(grossDisbursementAmount);
    if (netDisburseAmount != null) disbursalRegistryEntity.setNetDisbursalAmount(netDisburseAmount);

    return disbursalRegistryStoreService.updateDisbursalRegistryEntity(
        disbursalRegistryEntity, reason);
  }

  /**
   * Marks a loan as disbursed
   *
   * @param requestBody the callback request containing disbursement details
   * @param loanApplicationId the loan ID to process
   * @return a Mono containing the response after marking the loan as disbursed
   */
  @SuppressWarnings("unchecked")
  public Mono<?> markLoanDisbursed(
      AutoDisbursalCallbackRequest requestBody, String loanApplicationId, String productCode) {
    return loanApplicationService
        .getBankDetailsFromLoanLevelDataTable(loanApplicationId)
        .flatMap(
            bankData ->
                validateBankDetails(bankData)
                    .flatMap(
                        bankId ->
                            loanApplicationService
                                .getLoanApplicationByLoanId(loanApplicationId)
                                .flatMap(
                                    loanApplication -> {
                                      Map<String, Object> loanDataMap =
                                          (Map<String, Object>) loanApplication;
                                      String loanApplicationReferenceId =
                                          loanDataMap.get("loanApplicationReferenceId").toString();
                                      M2pLoanDisburseRequestDTO disburseRequest =
                                          M2pLoanDisburseRequestDTO.builder()
                                              .actualDisbursementDate(
                                                  requestBody.getSettlementDate())
                                              .dateFormat(DD_MM_YYYY)
                                              .paymentTypeId("1")
                                              .receiptNumber(requestBody.getTransferUtr())
                                              .bankAccountDetailId(Integer.parseInt(bankId))
                                              .build();
                                      return m2pWrapperApi
                                          .markLoanDisbursed(
                                              disburseRequest, loanApplicationReferenceId)
                                          .flatMap(
                                              response -> {
                                                log.info(
                                                    "[{}] loan marked disbursed successfully for"
                                                        + " loan application id: {}",
                                                    "LOAN_DISB",
                                                    loanApplicationId);
                                                return updateTransactionStatusInDisbursalRegistry(
                                                        requestBody.getSystemExternalId(),
                                                        DisbursalStatus.SUCCESS,
                                                        AUTO,
                                                        productCode,
                                                        null,
                                                        null,
                                                        null)
                                                    .then(Mono.just(response));
                                              })
                                          .onErrorResume(
                                              error -> {
                                                log.error(
                                                    "[{}] [ERROR] loan disbursement marking process"
                                                        + " failed for loan application id: {},"
                                                        + " with error: {}",
                                                    "LOAN_DISB",
                                                    loanApplicationId,
                                                    error.getMessage());
                                                return updateTransactionStatusInDisbursalRegistry(
                                                        requestBody.getSystemExternalId(),
                                                        DisbursalStatus.REJECTED,
                                                        AUTO,
                                                        productCode,
                                                        "MARK_LOAN_DISBURSED_FAILED",
                                                        null,
                                                        null)
                                                    .then(
                                                        Mono.error(
                                                            new BaseException(
                                                                "loan disbursement marking failed",
                                                                SOMETHING_WENT_WRONG,
                                                                HttpStatus.INTERNAL_SERVER_ERROR)));
                                              });
                                    })));
  }

  /**
   * Processes a failed auto disbursal by updating the disbursement status in the datatable and
   * triggering a manual disbursement.
   *
   * @param loanApplicationId the loan ID to process
   * @return a Mono containing the response after processing the failed disbursal
   */
  public Mono<?> processFailedAutoDisbursal(String loanApplicationId, String productCode) {
    return loanApplicationService
        .getBankDetailsFromLoanLevelDataTable(loanApplicationId)
        .flatMap(
            bankDetails ->
                validateBankDetails(bankDetails)
                    .flatMap(
                        bankId ->
                            processManualDisbursal(
                                loanApplicationId,
                                productCode,
                                bankId,
                                "AUTO_DISBURSAL_FAILED_AFTER_CALLBACK_FROM_VENDOR")));
  }

  /**
   * Initiates the process to check and process the auto-disbursal transaction status.
   *
   * @return a Mono containing a message indicating the process has started
   */
  public Mono<?> checkAndProcessAutoDisbursalStatus() {
    return Mono.deferContextual(
        context -> {
          checkAutoDisbursalStatus()
              .subscribeOn(Schedulers.parallel())
              .contextWrite(
                  ctx ->
                      ctx.put(TRACE_ID, context.get(TRACE_ID))
                          .put(PARTNER_ID, context.get(PARTNER_ID)))
              .doOnError(
                  error ->
                      log.error(
                          "[AUTO_DISB_STATUS] error occurred while checking transaction status:"
                              + " {}",
                          error.getMessage()))
              .subscribe();
          return Mono.just("started checking auto disbursal transaction status");
        });
  }

  /**
   * Checks the transaction status for all initiated auto-disbursal transactions and processes their
   * updates.
   *
   * @return a Flux containing the updated transaction statuses
   */
  private Flux<?> checkAutoDisbursalStatus() {
    return disbursalRegistryStoreService
        .findByDisbursalStatus(DisbursalStatus.AUTO_INI)
        .flatMap(this::checkAndUpdateStatus)
        .doOnError(error -> log.error("[AUTO_DISB_STATUS] error processing transactions", error));
  }

  /**
   * Checks the status of a disbursement transaction and updates it if necessary. If the transaction
   * is successful and completed, it marks the loan as disbursed.
   *
   * @param transaction the disburse transaction to check and update
   * @return a Mono containing the updated transaction status or an empty Mono if no updates are
   *     needed
   */
  private Mono<?> checkAndUpdateStatus(DisbursalRegistryEntity transaction) {
    return transactionApi
        .checkTransactionStatus(transaction.getReferenceId1())
        .flatMap(
            response -> {
              if (SUCCESS.equalsIgnoreCase(response.getStatus())
                  && COMPLETED.equalsIgnoreCase(response.getData().getStatus())) {
                return updateTransactionStatusInDisbursalRegistry(
                        response.getData().getSystemExternalId(),
                        DisbursalStatus.SUCCESS,
                        AUTO,
                        transaction.getProductCode(),
                        null,
                        null,
                        null)
                    .flatMap(
                        updateResp -> {
                          AutoDisbursalCallbackRequest request =
                              AutoDisbursalCallbackRequest.builder()
                                  .systemExternalId(response.getData().getSystemExternalId())
                                  .settlementDate(response.getData().getSettlementDate())
                                  .build();
                          return markLoanDisbursed(
                              request,
                              response.getData().getSystemExternalId(),
                              transaction.getProductCode());
                        });
              }
              return Mono.empty();
            });
  }

  public Mono<DisbursalRegistryEntity> saveDisbursalRegistryEntityWithAnchor(
      String clientId,
      String referenceId1,
      String referenceId2,
      String productCode,
      String anchorId,
      String disbursalType,
      DisbursalStatus disbursalStatus,
      BigDecimal grossDisbursalAmount,
      BigDecimal netDisbursalAmount) {
    DisbursalRegistryEntity registryEntity =
        DisbursalRegistryEntity.builder()
            .referenceId1(referenceId1)
            .referenceId2(referenceId2)
            .clientId(clientId)
            .productCode(productCode)
            .anchorId(anchorId)
            .disburseType(disbursalType)
            .disburseStatus(disbursalStatus)
            .grossDisbursalAmount(grossDisbursalAmount)
            .netDisbursalAmount(netDisbursalAmount)
            .isHydrated(false)
            .isDeleted(false)
            .build();
    return disbursalRegistryStoreService
        .save(registryEntity)
        .doOnSuccess(
            saved ->
                log.info(
                    "[DISBURSAL_REGISTRY] successfully saved disbursal registry for transaction id:"
                        + " {}",
                    referenceId1))
        .doOnError(
            error ->
                log.error(
                    "[DISBURSAL_REGISTRY] failed to save disbursal registry for loan transaction"
                        + " id: {}, error: {}",
                    referenceId1,
                    error.getMessage()));
  }

  Mono<DisbursalRegistryEntity> saveDisbursalRegistryEntity(
      String clientId,
      String loanApplicationId,
      String productCode,
      String bankId,
      String disbursalType,
      DisbursalStatus disbursalStatus,
      BigDecimal grossDisbursalAmount,
      BigDecimal netDisbursalAmount) {
    DisbursalRegistryEntity registryEntity =
        DisbursalRegistryEntity.builder()
            .referenceId1(loanApplicationId)
            .clientId(clientId)
            .productCode(productCode)
            .bankAccountId(bankId)
            .disburseType(disbursalType)
            .disburseStatus(disbursalStatus)
            .grossDisbursalAmount(grossDisbursalAmount)
            .netDisbursalAmount(netDisbursalAmount)
            .isHydrated(false)
            .isDeleted(false)
            .build();
    return disbursalRegistryStoreService
        .save(registryEntity)
        .doOnSuccess(
            saved ->
                log.info(
                    "[DISBURSAL_REGISTRY] successfully saved disbursal registry for loan"
                        + APPLICATION_ID_LOG_LITERAL,
                    loanApplicationId))
        .doOnError(
            error ->
                log.error(
                    "[DISBURSAL_REGISTRY] failed to save disbursal registry for loan application"
                        + " id: {}, error: {}",
                    loanApplicationId,
                    error.getMessage()));
  }
}
