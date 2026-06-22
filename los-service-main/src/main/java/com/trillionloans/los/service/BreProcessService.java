package com.trillionloans.los.service;

import static com.trillionloans.los.constant.BreType.SANCTION;
import static com.trillionloans.los.constant.StringConstants.ACTION;
import static com.trillionloans.los.constant.StringConstants.APPROVED;
import static com.trillionloans.los.constant.StringConstants.BRE;
import static com.trillionloans.los.constant.StringConstants.BRE_COMPLETED;
import static com.trillionloans.los.constant.StringConstants.BRE_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.BRE_FAIL;
import static com.trillionloans.los.constant.StringConstants.BRE_INITIATED;
import static com.trillionloans.los.constant.StringConstants.BRE_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.BUREAU_HARD_PULL;
import static com.trillionloans.los.constant.StringConstants.ELIGIBLE;
import static com.trillionloans.los.constant.StringConstants.FAIL;
import static com.trillionloans.los.constant.StringConstants.FINAL_LIMIT;
import static com.trillionloans.los.constant.StringConstants.INELIGIBLE;
import static com.trillionloans.los.constant.StringConstants.INITIATED;
import static com.trillionloans.los.constant.StringConstants.LEAD_ACKNOWLEDGEMENT;
import static com.trillionloans.los.constant.StringConstants.LIMIT;
import static com.trillionloans.los.constant.StringConstants.LOANID;
import static com.trillionloans.los.constant.StringConstants.LOANS;
import static com.trillionloans.los.constant.StringConstants.LOAN_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.LOAN_TENURE;
import static com.trillionloans.los.constant.StringConstants.LOGGING_RESPONSE;
import static com.trillionloans.los.constant.StringConstants.M2P_UPDATE;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.REASONS;
import static com.trillionloans.los.constant.StringConstants.REQUESTED_AMT_GREATER_THAN_APPROVED_AMT;
import static com.trillionloans.los.constant.StringConstants.ROI;
import static com.trillionloans.los.constant.StringConstants.SCIENAPTIC;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.constant.StringConstants.TENURE;
import static com.trillionloans.los.constant.StringConstants.TENURE_NOT_FOUNT;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.TRUE;
import static com.trillionloans.los.util.BreDataUtil.extractBureauId;
import static com.trillionloans.los.util.JsonUtils.extractFieldValueByPath;
import static com.trillionloans.los.util.JsonUtils.parseJson;
import static com.trillionloans.los.util.JsonUtils.retainBreResponseFromJson;
import static com.trillionloans.los.util.Util.castToMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trillionloans.los.api.partner.KycAdaptorApi;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.ScienapticApi;
import com.trillionloans.los.constant.BreStage;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.model.dto.BreDatatableDTO;
import com.trillionloans.los.model.dto.MessagePayloadObject;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.BreStatus;
import com.trillionloans.los.model.entity.ScienapticEntity;
import com.trillionloans.los.model.response.BankUnderwritingGstUnderwritingResponseDTO;
import com.trillionloans.los.model.response.BreInitiatedResponseDTO;
import com.trillionloans.los.model.response.PerformanceDataDTO;
import com.trillionloans.los.model.response.m2p.LeadLoanDataResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pBreDataResponseDTO;
import com.trillionloans.los.service.db.BreStatusService;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.ScienapticStoreService;
import com.trillionloans.los.util.BreDataUtil;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service class for handling Business Rule Engine (BRE) processes. This class manages various
 * stages of the BRE process including data posting, bureau data fetching, Scienaptic processing,
 * and M2P updates. Created on: 2024-09-12 Last updated on: 2024-09-24
 *
 * @author Ganesh Budhwant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BreProcessService {

  private final M2PWrapperApi m2PWrapperApi;
  private final BreStatusService breStatusService;
  private final ScienapticApi scienapticApi;
  private final M2pFacadeService m2pFacadeService;
  private final ProductConfigMasterService productConfigMasterService;
  private final LoanApplicationService loanApplicationService;
  private final PublisherService publisherService;
  private final Gson gson;
  private final ScienapticStoreService scienapticStoreService;
  private final KycAdaptorApi kycAdaptorApi;
  private final PartnerMasterService partnerMasterService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final LeadAcknowledgementService leadAcknowledgementService;

  /** This method is for sync BRE */
  public Mono<M2pBreDataResponseDTO.AdditionalResponseData.M2pBreOutput> postBreDataML(
      Object requestBody, String loanId, String productCode) {
    return m2PWrapperApi
        .postBreData(requestBody, loanId)
        .cast(M2pBreDataResponseDTO.class)
        .flatMap(
            data -> {
              if (Objects.isNull(data)
                  || Objects.isNull(data.getAdditionalResponseData())
                  || Objects.isNull(data.getAdditionalResponseData().getOutput())) {
                return Mono.error(
                    new BaseException(BRE_FAIL, null, HttpStatus.INTERNAL_SERVER_ERROR));
              }
              if (!data.getAdditionalResponseData().getOutput().isSuccess()) {
                return Mono.just(data.getAdditionalResponseData().getOutput());
              }
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlConfigData -> {
                        ProductControl.Flow flowData =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControlConfigData.getT2(), BRE_CTA_IDENTIFIER);
                        if (Objects.isNull(flowData)) {
                          return Mono.error(
                              new BaseException(
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                        return m2PWrapperApi
                            .registerCta(loanId, flowData.getCtaName())
                            .map(response -> data.getAdditionalResponseData().getOutput());
                      });
            });
  }

  /** Posts the BRE data based on the product configuration. */
  public Mono<BreInitiatedResponseDTO> postBreDataProductWise(
      Object requestBody,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {
    // Step 1: Check if BRE has already been processed for this loan.
    return breStatusService
        .findByExternalIdAndBreType(loanId, SANCTION.getDisplayName())
        .flatMap(
            // Return early if BRE is already completed.
            breStatus -> {
              if (isBreCompleted(breStatus, flowData)) {
                return buildBreCompletedResponse();
              } else {
                return initiateBreProcessWithContext(
                    requestBody, loanId, productCode, flowData, partnerCode);
              }
            })
        // Step 2: Initiate BRE process if not already completed.
        .switchIfEmpty(
            initiateBreProcessWithContext(requestBody, loanId, productCode, flowData, partnerCode));
  }

  private boolean isBreCompleted(BreStatus breStatus, ProductControl.Flow flowData) {
    Double retryLimit = (Double) flowData.getConditions().get("retryLimit");
    Double rejectedLimit = (Double) flowData.getConditions().get("rejectedLimit");
    return breStatus.getRetryCount() >= retryLimit.longValue()
        || breStatus.getRejectedCount() >= rejectedLimit.longValue();
  }

  private Mono<BreInitiatedResponseDTO> buildBreCompletedResponse() {
    BreInitiatedResponseDTO responseDTO = new BreInitiatedResponseDTO();
    responseDTO.setStatus("BRE already completed for this loan");
    responseDTO.setStatusCode(BRE_COMPLETED);
    log.info(LOGGING_RESPONSE, BRE_RESPONSE, "trillion", gson.toJson(responseDTO));
    return Mono.just(responseDTO);
  }

  private Mono<BreInitiatedResponseDTO> initiateBreProcessWithContext(
      Object requestBody,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {

    if (!flowData.isLeadAcknowledgement()) {
      log.info("[{}] Skipping lead acknowledgment for LoanAppId={}", LEAD_ACKNOWLEDGEMENT, loanId);
      return initiateBreWithResponse(requestBody, loanId, productCode, flowData, partnerCode);
    }
    return leadAcknowledgementService
        .processAndVerifyAcknowledgement(loanId)
        .then(initiateBreWithResponse(requestBody, loanId, productCode, flowData, partnerCode));
  }

  private Mono<BreInitiatedResponseDTO> initiateBreWithResponse(
      Object requestBody,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {

    return Mono.deferContextual(
        parentContext -> {
          BreInitiatedResponseDTO responseDTO = new BreInitiatedResponseDTO();
          responseDTO.setStatus(BreStage.INITIATED.getDisplayName());
          responseDTO.setStatusCode(BRE_INITIATED);

          initiateBreProcess(requestBody, loanId, productCode, flowData, partnerCode)
              .subscribeOn(Schedulers.parallel())
              .contextWrite(
                  context ->
                      context
                          .put(TRACE_ID, parentContext.get(TRACE_ID))
                          .put(PARTNER_ID, parentContext.get(PARTNER_ID)))
              .subscribe();
          return Mono.just(responseDTO);
        });
  }

  /** Initiates the BRE process, checking if the process should be resumed from a failed stage. */
  public Mono<?> initiateBreProcess(
      Object request,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {
    Mono<BreStatus> breStatusMono =
        breStatusService.findByExternalIdAndBreTypeAndIsActive(
            loanId, SANCTION.getDisplayName(), true);
    // Step 1: Check if BRE status exists for the loan.
    return breStatusMono
        .hasElement()
        .flatMap(
            hasElement -> {
              if (Boolean.TRUE.equals(hasElement)) {
                return breStatusMono.flatMap(
                    existingBreStatus -> {
                      // If BRE previously failed, resume from the failed stage.
                      if (FAIL.equals(existingBreStatus.getStatus())) {
                        return resumeFromFailedStage(
                            existingBreStatus, loanId, productCode, flowData, partnerCode);
                      } else {
                        // Otherwise, start the process from the beginning.
                        return startFromBeginning(
                            request, loanId, productCode, flowData, partnerCode);
                      }
                    });
              } else {
                // Start BRE process from the beginning if no status exists.
                return startFromBeginning(request, loanId, productCode, flowData, partnerCode);
              }
            });
  }

  /** Starts the BRE process from the beginning. */
  private Mono<Object> startFromBeginning(
      Object request,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {
    // Step 1: Create an initial BRE status object.
    BreStatus breStatus = createInitialBreStatus(request, loanId, productCode);
    // Step 2: Log the BRE process status and process the stages.
    return breStatusService
        .logBreProcessStatus(breStatus)
        .flatMap(
            initialBreStatus -> processStages(request, loanId, productCode, flowData, partnerCode))
        .onErrorResume(
            error ->
                handleUpdateError(
                    request, BreStage.INITIATED.getDisplayName(), productCode, loanId, error));
  }

  private Mono<Object> resumeFromFailedStage(
      BreStatus existingBreStatus,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {
    // Step 1: Deserialize the saved request from the failed BRE status.
    Object jsonRequest = gson.fromJson(existingBreStatus.getRequest().asString(), Object.class);
    // Step 2: Resume the BRE process from the appropriate stage based on the current stage.
    return switch (existingBreStatus.getStage()) {
      case INITIATED, BUREAU_HARD_PULL ->
          processStages(jsonRequest, loanId, productCode, flowData, partnerCode);
      case SCIENAPTIC ->
          processScienapticStage(jsonRequest, loanId, productCode, flowData, partnerCode);
      case M2P_UPDATE ->
          processM2PUpdateStage(jsonRequest, loanId, productCode, flowData, partnerCode);
      default ->
          Mono.error(
              new BaseException(
                  BRE_FAIL,
                  "Invalid stage for resumeFromFailedStage",
                  HttpStatus.INTERNAL_SERVER_ERROR));
    };
  }

  /**
   * Processes the stages of the BRE workflow based on the skip stages specified in the flow data.
   */
  private Mono<Object> processStages(
      Object request,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {
    // Step 1: Get the list of stages to be skipped based on the flow configuration.
    List<String> skipStages =
        List.of(flowData.getConditions().get("skipStage").toString().split(","));
    // Step 2: If "BUREAU_HARD_PULL" is in the skip stages, skip to the Scienaptic stage.
    if (!skipStages.isEmpty() && skipStages.contains(BUREAU_HARD_PULL)) {
      return processScienapticStage(request, loanId, productCode, flowData, partnerCode);
    }
    // Step 3: If both "SCIENAPTIC" and "BUREAU_HARD_PULL" stages are to be skipped, move to M2P
    // update stage.
    if (!skipStages.isEmpty()
        && skipStages.contains(SCIENAPTIC)
        && skipStages.contains(BUREAU_HARD_PULL)) {
      return processM2PUpdateStage(request, loanId, productCode, flowData, partnerCode);
    }
    // Step 4: If no stages are skipped, proceed with fetching and processing Bureau data.
    return fetchAndProcessBureauData(request, loanId, productCode, flowData, partnerCode);
  }

  /** Fetches Bureau data and processes it, then posts the data to Scienaptic. */
  private Mono<Object> fetchAndProcessBureauData(
      Object jsonRequest,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {
    // Step 1: Delay the execution by 30 seconds before fetching Bureau data as it takes time at m2p
    // end.
    boolean isPerformanceDataRequired =
        flowData.getConditions() != null
            && flowData.getConditions().containsKey("requiredPerformanceData")
            && Boolean.TRUE.equals(flowData.getConditions().get("requiredPerformanceData"));

    boolean isMigrationSwitchEnabled =
        flowData.getConditions() != null
            && flowData.getConditions().containsKey("migrationSwitch")
            && Boolean.TRUE.equals(flowData.getConditions().get("migrationSwitch"));
    Double bureauDelay = (Double) flowData.getConditions().getOrDefault("bureauDelay", 20.0);
    if (isPerformanceDataRequired && !isMigrationSwitchEnabled) {
      return Mono.delay(Duration.ofSeconds(bureauDelay.longValue()))
          .flatMap(delay -> fetchBureauData(loanId))
          // Step 2: Process the Bureau response and pass it to Scienaptic.
          .flatMap(
              bureauResponse -> {
                Object scienapticRequest =
                    getScienapticRequest(jsonRequest, bureauResponse, productCode);
                // Post Bureau data to Scienaptic and process the response.
                String reportApiName =
                    flowData.getConditions().getOrDefault("reportApiName", "").toString();
                return m2pFacadeService
                    .findClientIdAndOrigReqAmountFromLoanId(loanId)
                    .flatMap(
                        dto ->
                            m2pFacadeService
                                .getClientLoansPerformanceReport(
                                    dto.getClientId(), reportApiName, loanId)
                                .flatMap(
                                    it ->
                                        appendPerformanceDataInJsonRequest(
                                                jsonRequest,
                                                it,
                                                flowData) // Process performance data
                                            .flatMap(
                                                success ->
                                                    postBreDataToScienaptic(
                                                            scienapticRequest,
                                                            productCode,
                                                            flowData,
                                                            loanId)
                                                        .flatMap(
                                                            scienapticResponse ->
                                                                processScienapticResponse(
                                                                    jsonRequest,
                                                                    scienapticResponse,
                                                                    loanId,
                                                                    productCode,
                                                                    flowData,
                                                                    partnerCode,
                                                                    dto.getLoanAmountRequested()))))
                                .onErrorResume(
                                    error ->
                                        handleUpdateError(
                                            jsonRequest,
                                            BreStage.SCIENAPTIC.getDisplayName(),
                                            productCode,
                                            loanId,
                                            error)));
              })
          // Step 3: Handle errors that occur during the Bureau data fetch and processing.
          .onErrorResume(
              error ->
                  handleUpdateError(
                      jsonRequest,
                      BreStage.BUREAU_HARD_PULL.getDisplayName(),
                      productCode,
                      loanId,
                      error));
    } else {
      return Mono.delay(Duration.ofSeconds(bureauDelay.longValue()))
          .flatMap(delay -> fetchBureauData(loanId))
          // Step 2: Process the Bureau response and pass it to Scienaptic.
          .flatMap(
              bureauResponse -> {
                Object scienapticRequest =
                    getScienapticRequest(jsonRequest, bureauResponse, productCode);
                // Post Bureau data to Scienaptic and process the response.
                return m2pFacadeService
                    .findClientIdAndOrigReqAmountFromLoanId(loanId) // changed
                    .flatMap(
                        dto ->
                            postBreDataToScienaptic(
                                    scienapticRequest, productCode, flowData, loanId)
                                .flatMap(
                                    scienapticResponse ->
                                        processScienapticResponse(
                                            jsonRequest,
                                            scienapticResponse,
                                            loanId,
                                            productCode,
                                            flowData,
                                            partnerCode,
                                            dto.getLoanAmountRequested()))) // new param
                ;
              })
          // Step 3: Handle errors that occur during the Bureau data fetch and processing.
          .onErrorResume(
              error ->
                  handleUpdateError(
                      jsonRequest,
                      BreStage.BUREAU_HARD_PULL.getDisplayName(),
                      productCode,
                      loanId,
                      error));
    }
  }

  /** Processes the Scienaptic stage by posting data to Scienaptic and handling the response. */
  private Mono<Object> processScienapticStage(
      Object jsonRequest,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {
    boolean isPerformanceDataRequired =
        flowData.getConditions() != null
            && flowData.getConditions().containsKey("requiredPerformanceData")
            && Boolean.TRUE.equals(flowData.getConditions().get("requiredPerformanceData"));

    boolean isMigrationSwitchEnabled =
        flowData.getConditions() != null
            && flowData.getConditions().containsKey("migrationSwitch")
            && Boolean.TRUE.equals(flowData.getConditions().get("migrationSwitch"));

    // Check if 'performanceData' is required and migrationSwitch is false
    if (isPerformanceDataRequired && !isMigrationSwitchEnabled) {

      String reportApiName = flowData.getConditions().getOrDefault("reportApiName", "").toString();
      return m2pFacadeService
          .findClientIdAndOrigReqAmountFromLoanId(loanId) // changed
          .flatMap(
              dto ->
                  m2pFacadeService
                      .getClientLoansPerformanceReport(
                          dto.getClientId(), reportApiName, loanId) // changed
                      .flatMap(
                          it ->
                              appendPerformanceDataInJsonRequest(
                                      jsonRequest, it, flowData) // Process performance data
                                  .flatMap(
                                      success ->
                                          postBreDataToScienaptic(
                                                  jsonRequest,
                                                  productCode,
                                                  flowData,
                                                  loanId) // Post BRE data to scienaptic
                                              .flatMap(
                                                  scienapticResponse ->
                                                      processScienapticResponse(
                                                          jsonRequest,
                                                          scienapticResponse,
                                                          loanId,
                                                          productCode,
                                                          flowData,
                                                          partnerCode,
                                                          dto.getLoanAmountRequested())) // new
                                              // param
                                              .onErrorResume(
                                                  error -> {
                                                    return handleUpdateError(
                                                        jsonRequest,
                                                        BreStage.SCIENAPTIC.getDisplayName(),
                                                        productCode,
                                                        loanId,
                                                        error); // Error handling for Scienaptic
                                                    // processing
                                                  })))
                      .onErrorResume(
                          error ->
                              handleUpdateError(
                                  jsonRequest,
                                  BreStage.SCIENAPTIC.getDisplayName(),
                                  productCode,
                                  loanId,
                                  error) // General error handling
                          ));
    } else {
      return m2pFacadeService
          .findClientIdAndOrigReqAmountFromLoanId(loanId) // changed
          .flatMap(
              dto ->
                  postBreDataToScienaptic(jsonRequest, productCode, flowData, loanId)
                      .flatMap(
                          scienapticResponse ->
                              processScienapticResponse(
                                  jsonRequest,
                                  scienapticResponse,
                                  loanId,
                                  productCode,
                                  flowData,
                                  partnerCode,
                                  dto.getLoanAmountRequested()))) // new param
          // Step 2: Handle any errors that occur during the Scienaptic processing.
          .onErrorResume(
              error ->
                  handleUpdateError(
                      jsonRequest,
                      BreStage.SCIENAPTIC.getDisplayName(),
                      productCode,
                      loanId,
                      error));
    }
  }

  /** append performance data in bre request input */
  public Mono<?> appendPerformanceDataInJsonRequest(
      Object jsonRequest, Object performanceDataReport, ProductControl.Flow flowData) {
    Map<String, Object> requestMap =
        castToMap(jsonRequest, "[ERROR] Invalid request format: expected a Map at the root level");
    Map<String, Object> values =
        castToMap(
            requestMap.get("values"), "[ERROR] Invalid request format: 'values' key not found");
    Map<String, Object> input =
        castToMap(
            values.get("input"),
            "[ERROR] Invalid request format: 'input' key not found within 'values'");

    Boolean skipPerformanceDataFormatting =
        (Boolean) flowData.getConditions().getOrDefault("skipPerformanceDataFormatting", true);
    if (skipPerformanceDataFormatting == null || skipPerformanceDataFormatting) {
      // Append performance data directly
      PerformanceDataDTO newPerformanceData =
          PerformanceDataDTO.builder().loans(performanceDataReport).build();
      Map<String, Object> newPerformanceDataMap =
          objectMapper.convertValue(
              newPerformanceData, new TypeReference<Map<String, Object>>() {});
      input.compute(
          "performanceData",
          (key, existing) -> {
            if (existing instanceof Map) {
              return mergePerformanceData((Map<String, Object>) existing, newPerformanceDataMap);
            }
            return newPerformanceDataMap;
          });
      return Mono.just(jsonRequest);
    } else {
      // Process performance data
      PerformanceDataDTO performanceData =
          processPerformanceDataSafely(performanceDataReport, flowData);
      if (performanceData == null) {
        return Mono.just(jsonRequest); // Return original request if no data generated
      }
      // Append processed performance data
      input.put("performanceData", performanceData);
      return Mono.just(jsonRequest);
    }
  }

  private Map<String, Object> mergePerformanceData(
      Map<String, Object> existing, Map<String, Object> newData) {
    Map<String, Object> newDataMap = castToMap(newData, "[ERROR] Invalid performance data format");
    newDataMap.forEach(existing::putIfAbsent);
    return existing;
  }

  private PerformanceDataDTO processPerformanceDataSafely(
      Object performanceDataReport, ProductControl.Flow flowData) {
    try {
      List<LeadLoanDataResponseDTO> dataList =
          objectMapper.convertValue(
              performanceDataReport, new TypeReference<List<LeadLoanDataResponseDTO>>() {});
      return processPerformanceData(dataList, flowData);
    } catch (Exception e) {
      throw new RuntimeException(
          "[CLIENT_LOANS_DETAILS_REPORT] Failed to convert and process data", e);
    }
  }

  /** map leadLoanDataList to performance data */
  private PerformanceDataDTO processPerformanceData(
      List<LeadLoanDataResponseDTO> leadLoanDataList, ProductControl.Flow flowData) {
    // calculate total outstanding amount
    BigDecimal totalOutstanding =
        leadLoanDataList.stream()
            .map(LeadLoanDataResponseDTO::getDisbursedAmountOrDrawdownAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // calculate max DPD
    int maxDpd =
        leadLoanDataList.stream()
            .map(LeadLoanDataResponseDTO::getMaxDPDDays)
            .filter(Objects::nonNull) // filter out null values if getMaxDPDDays returns Integer
            .mapToInt(Integer::intValue) // unbox Integer to int
            .max()
            .orElse(0);
    // calculate max DPD on active loans
    int maxDpdOnActiveLoans =
        leadLoanDataList.stream()
            .filter(it -> mapLoanStatus(it.status).equals("ACTIVE"))
            .map(LeadLoanDataResponseDTO::getMaxDPDDays)
            .filter(Objects::nonNull) // filter out null values if getMaxDPDDays returns Integer
            .mapToInt(Integer::intValue) // unbox Integer to int
            .max()
            .orElse(0);
    // calculate count of 30 plus DPD on closed loans
    long dpd30PlusCountOnClosedLoans =
        leadLoanDataList.stream()
            .filter(it -> mapLoanStatus(it.getStatus()).equals("CLOSED"))
            .filter(it -> it.getCurrentDPD() >= 30)
            .count();

    // Build and return the PerformanceDataDTO
    return PerformanceDataDTO.builder()
        .totalOutstanding(totalOutstanding.toString())
        .maxDpd(Integer.toString(maxDpd))
        .maxDpdOnActiveLoans(maxDpdOnActiveLoans)
        .dpd30PlusCountOnClosedLoans((int) dpd30PlusCountOnClosedLoans)
        .build();
  }

  /** map loan status from status enum code */
  private String mapLoanStatus(int status) {
    Set<Integer> activeLoanStatuses = Set.of(300);
    Set<Integer> closedLoanStatuses = Set.of(600, 700);

    if (activeLoanStatuses.contains(status)) {
      return "ACTIVE";
    } else if (closedLoanStatuses.contains(status)) {
      return "CLOSED";
    } else {
      log.warn("[BRE_PROCESS] Unrecognized loan status encountered: {}", status);
      return "";
    }
  }

  /** Processes the M2P Update stage by sending BRE data to M2P and updating the process status. */
  private Mono<Object> processM2PUpdateStage(
      Object jsonRequest,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode) {

    // Step 1: Convert the request data to BreDatatableDTO and reset request/response fields as m2p
    // cannot save large data.
    BreDatatableDTO breDatatableDto =
        new Gson().fromJson(new Gson().toJson(jsonRequest), BreDatatableDTO.class);
    breDatatableDto.setRequest("");
    breDatatableDto.setResponse("");

    // Fetch origReqAmt from LoanInfoDTO and set in breDatatableDto
    return m2pFacadeService
        .findClientIdAndOrigReqAmountFromLoanId(loanId)
        .doOnNext(
            dto -> {
              if (dto.getLoanAmountRequested() != null) {
                breDatatableDto.setOrig_req_amt(dto.getLoanAmountRequested());
              }
            })
        // Step 2: Send the updated data to M2P and update the BRE process status.
        .flatMap(
            dto ->
                m2PWrapperApi
                    .updateBreDataToM2P(breDatatableDto, loanId)
                    .flatMap(
                        updateM2pResponse ->
                            updateBreProcessStatus(
                                    jsonRequest,
                                    BreStage.COMPLETED.getDisplayName(),
                                    productCode,
                                    loanId,
                                    null,
                                    SUCCESS,
                                    true,
                                    breDatatableDto.getScienapticstatus())
                                .flatMap(
                                    breStatus ->
                                        triggerCallback(
                                            jsonRequest, SUCCESS, productCode, loanId, flowData)))
                    // Step 3: Handle any errors during the M2P update process.
                    .onErrorResume(
                        error ->
                            handleUpdateError(
                                jsonRequest,
                                BreStage.M2P_UPDATE.getDisplayName(),
                                productCode,
                                loanId,
                                error)));
  }

  /** Processes the response from Scienaptic and updates the corresponding BRE process status. */
  @SuppressWarnings("unchecked")
  private Mono<Object> processScienapticResponse(
      Object jsonRequest,
      Object scienapticResponse,
      String loanId,
      String productCode,
      ProductControl.Flow flowData,
      String partnerCode,
      BigDecimal origReqAmt) {
    // Step 1: Convert the request and response data to BreDatatableDTO for further processing.
    BreDatatableDTO breDatatableDTO = getBreDatatableDTO(jsonRequest, scienapticResponse, loanId);
    breDatatableDTO.setOrig_req_amt(origReqAmt);
    // Step 2: Extract BRE response data from the Scienaptic response and update the DTO.
    return getBreResponse(gson.toJson(scienapticResponse), flowData, loanId)
        .flatMap(
            breResponse -> {
              breDatatableDTO.setScienapticstatus(
                  breResponse.getOrDefault(ACTION, "false").toString());
              breDatatableDTO.setRoi(breResponse.getOrDefault(ROI, "-").toString());
              breDatatableDTO.setAmount(breResponse.getOrDefault(LIMIT, "-").toString());
              breDatatableDTO.setTenure(breResponse.getOrDefault(TENURE, "-").toString());
              breDatatableDTO.setReasons(breResponse.getOrDefault(REASONS, "").toString());
              breDatatableDTO.setData(
                  breResponse.containsKey("data") ? gson.toJson(breResponse.get("data")) : null);

              if (APPROVED.equalsIgnoreCase(breResponse.get("action").toString())) {
                breDatatableDTO.setScienapticstatus(
                    APPROVED.equalsIgnoreCase(breResponse.get("action").toString())
                        ? ELIGIBLE
                        : INELIGIBLE);
              }

              // Step 3: Build a message payload and publish it to a sqs queue.
              MessagePayloadObject messagePayload =
                  MessagePayloadObject.builder()
                      .externalId(loanId)
                      .request((Map<String, Object>) jsonRequest)
                      .response((Map<String, Object>) scienapticResponse)
                      .build();

              return Mono.fromCallable(() -> gson.toJson(messagePayload))
                  .flatMap(
                      payloadJson ->
                          publisherService
                              .sendToFifoQueue(payloadJson, "BRE", loanId, productCode, partnerCode)
                              .then(Mono.just(payloadJson))
                              .onErrorResume(error -> Mono.just(payloadJson)))
                  // Step 4: Save Scienaptic data and handle potential errors in the save process.
                  .flatMap(
                      payloadJson ->
                          partnerMasterService
                              .findByProductCode(productCode) // <-- ADDED
                              .flatMap(
                                  partnerEntity -> // <-- ADDED
                                  saveScienapticEntity(
                                              jsonRequest,
                                              scienapticResponse,
                                              loanId,
                                              SANCTION.getDisplayName(),
                                              breResponse.getOrDefault(ACTION, "false").toString(),
                                              partnerEntity.getPartnerId()) // <-- ADDED
                                          .onErrorResume(
                                              error -> {
                                                log.error(
                                                    "Failed to save scienaptic data for loanId :"
                                                        + " {}",
                                                    loanId,
                                                    error);
                                                return Mono.empty();
                                              }))
                              .then(Mono.just(payloadJson)) // <-- ADDED
                      )
                  // Step 5: Update BRE data to M2P and update the process status.
                  .flatMap(
                      payloadJson -> {
                        BreDatatableDTO breDatatableDTOTemp = new BreDatatableDTO(breDatatableDTO);
                        breDatatableDTOTemp.setResponse("");
                        breDatatableDTOTemp.setRequest("");
                        return m2PWrapperApi
                            .updateBreDataToM2P(breDatatableDTOTemp, loanId)
                            .flatMap(
                                updateM2pResponse ->
                                    updateBreProcessStatus(
                                            breDatatableDTO,
                                            BreStage.COMPLETED.getDisplayName(),
                                            productCode,
                                            loanId,
                                            null,
                                            SUCCESS,
                                            false,
                                            breDatatableDTO.getScienapticstatus())
                                        .flatMap(
                                            breStatus ->
                                                triggerCallback(
                                                    breResponse,
                                                    SUCCESS,
                                                    productCode,
                                                    loanId,
                                                    flowData)))
                            // Step 6: Handle any errors during M2P update.
                            .onErrorResume(
                                error ->
                                    handleUpdateError(
                                        breDatatableDTO,
                                        BreStage.M2P_UPDATE.getDisplayName(),
                                        productCode,
                                        loanId,
                                        error));
                      });
            });
  }

  /** Saves the Scienaptic data into the Scienaptic entity store. */
  private Mono<ScienapticEntity> saveScienapticEntity(
      Object jsonRequest,
      Object scienapticResponse,
      String loanId,
      String breType,
      String sciennapticStatus,
      String partnerId) {
    // Step 1: Build the ScienapticEntity object to be saved.
    ScienapticEntity scienapticEntity =
        ScienapticEntity.builder()
            .externalId(loanId)
            .breType(breType)
            .request(Json.of(gson.toJson(jsonRequest)))
            .response(Json.of(gson.toJson(scienapticResponse)))
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .scienapticStatus(sciennapticStatus)
            .partnerId(partnerId)
            .build();
    // Step 2: Save the Scienaptic entity and log the result.
    return scienapticStoreService
        .save(scienapticEntity)
        .doOnSuccess(
            savedStatus -> log.info("Scienaptic data successfully saved for loanId: {}", loanId));
  }

  /** Triggers a callback based on the BRE process outcome status. */
  private Mono<Object> triggerCallback(
      Object request,
      String status,
      String productCode,
      String loanId,
      ProductControl.Flow flowData) {
    // Step 1: Based on the status, prepare the BRE response to trigger the callback.
    Mono<Map<String, Object>> breResponseMono;
    // If the status is SUCCESS, use the original request as the BRE response.
    if (SUCCESS.equals(status)) {
      breResponseMono = Mono.just((Map<String, Object>) request);
    }
    // If the status is FAIL, create a response indicating failure.
    else if (FAIL.equals(status)) {
      Map<String, Object> failResponse = new HashMap<>();
      failResponse.put("success", "false");
      breResponseMono = Mono.just(failResponse);
    }
    // If neither, return an empty response.
    else {
      breResponseMono = Mono.just(new HashMap<>());
    }
    // Step 2: Add loanId to the BRE response and trigger the M2P registration of the BRE status.
    return breResponseMono
        .flatMap(
            breResponse -> {
              breResponse.put(LOANID, loanId);
              return m2pFacadeService.registerBreStatus(breResponse, productCode);
            })
        .flatMap(result -> Mono.just((Object) result))
        // Step 3: Handle any errors during the callback and log the issue.
        .onErrorResume(
            callbackError -> {
              log.error(
                  "[{}] Error occurred while triggering callback: {}",
                  "BRE_COMPLETED",
                  callbackError.getMessage());
              return Mono.empty();
            });
  }

  /**
   * Handles errors that occur during the BRE process, logs them, and updates the process status.
   */
  private Mono<Object> handleUpdateError(
      Object jsonRequest, String stage, String productCode, String loanId, Throwable error) {
    // Log the error details at the specific BRE stage.
    log.error("[{}] Error occurred at stage {}: {}", BRE + stage, stage, error.getMessage());
    // Step 1: Update the BRE process status as 'FAIL' and trigger the callback.
    return updateBreProcessStatus(jsonRequest, stage, productCode, loanId, error, FAIL, true, null)
        .onErrorResume(
            updateError -> {
              log.error(
                  "[{}] Error occurred while updating process status: {}",
                  BRE + stage,
                  updateError.getMessage());
              return Mono.empty();
            })
        .flatMap(breStatus -> triggerCallback(null, FAIL, productCode, loanId, null));
  }

  /** Updates the status of the BRE process in the BreStatus table. */
  private Mono<BreStatus> updateBreProcessStatus(
      Object request,
      String stage,
      String productCode,
      String loanId,
      Throwable error,
      String status,
      boolean isActive,
      String scienapticStatus) {
    // Convert error message into a JSON string.
    String response = error != null ? setErrorMessageInBreResponse(error) : null;
    // Serialize the request object to a JSON string.
    String jsonRequest = gson.toJson(request);
    // Log the update process status.
    log.info(
        "[{}] updateBreProcessStatus message: {}", BRE + stage, response == null ? "" : response);
    // Step 1: Create a BreStatus object representing the current process status.
    BreStatus breStatus =
        BreStatus.builder()
            .externalId(loanId)
            .status(status)
            .stage(stage)
            .request(Json.of(jsonRequest))
            .response(response != null ? Json.of(response) : null)
            .isActive(isActive)
            .productCode(productCode)
            .scienapticStatus(scienapticStatus)
            .build();
    // Step 2: Log the BRE process status in the database.
    return breStatusService.logBreProcessStatus(breStatus);
  }

  /** Fetches Bureau data from an external service based on the loan ID. */
  private Mono<Object> fetchBureauData(String loanId) {

    // Step 1: Call an external API (CRIF) to retrieve Bureau data.
    return m2PWrapperApi
        .callExperian(loanId)
        .flatMap(
            response -> {
              String bureauId = extractBureauId(response, "id");
              // Step 2: Fetch Bureau data using the retrieved Bureau ID.
              return m2PWrapperApi
                  .fetchBureauData(bureauId)
                  .map(
                      encodedData -> {
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
                        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
                        // Step 3: Parse the decoded Bureau data into a JSON object.
                        return parseJson(decodedString);
                      });
            });
  }

  private Mono<BankUnderwritingGstUnderwritingResponseDTO> fetchBankUnderwritingAndGstWritingData(
      String loanId, String productCode) {
    return partnerMasterService
        .findByProductCode(productCode)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Partner not found for product code: " + productCode)))
        .flatMap(
            partnerMasterEntity ->
                kycAdaptorApi.getBankUnderwritingAndGstUnderwritingData(
                    loanId, partnerMasterEntity.getPartnerId()));
  }

  /** Posts BRE data to the Scienaptic API based on the Bureau request and product code. */
  private Mono<Object> postBreDataToScienaptic(
      Object bureauRequest, String productCode, ProductControl.Flow flowData, String loanId) {

    boolean addBankUnderwritingAndGstUnderwriting =
        flowData.getConditions().containsKey("addBankUnderwritingAndGstUnderwriting")
            && flowData.getConditions().get("addBankUnderwritingAndGstUnderwriting").equals("true");

    // If the flag is true, fetch bank underwriting and GST underwriting data and append it to the
    if (addBankUnderwritingAndGstUnderwriting) {
      return fetchBankUnderwritingAndGstWritingData(loanId, productCode)
          .flatMap(
              bankAndGstData -> {
                Object bureauRequestWithUnderwritingData =
                    getScienapticRequestWithBankDataAndGstData(
                        bureauRequest, productCode, bankAndGstData);
                // Extract the application type from the Bureau request object.
                String applicationTypePath =
                    (Objects.equals(productCode, "ELO") || Objects.equals(productCode, "BPBL1"))
                        ? "values.input.application_type"
                        : "values.input.applicationType";
                String applicationType =
                    extractFieldValueByPath(bureauRequestWithUnderwritingData, applicationTypePath);
                // Post Bureau data to Scienaptic API, appending the application type to the product
                // code.
                return scienapticApi.postBureauData(
                    bureauRequestWithUnderwritingData, productCode + "_" + applicationType, loanId);
              });
    } else {
      // Extract the application type from the Bureau request object.
      String applicationTypePath =
          Objects.equals(productCode, "ELO")
              ? "values.input.application_type"
              : "values.input.applicationType";
      String applicationType = extractFieldValueByPath(bureauRequest, applicationTypePath);
      // Post Bureau data to Scienaptic API, appending the application type to the product code.
      return scienapticApi.postBureauData(
          bureauRequest, productCode + "_" + applicationType, loanId);
    }
  }

  /** Generates a Scienaptic request DTO based on the request and response data. */
  private Object getScienapticRequest(Object request, Object response, String productCode) {
    return BreDataUtil.getScienapticDetailsDTO(request, response, productCode);
  }

  /** Creates a BreDatatableDTO based on the request, response, and loan ID. */
  private BreDatatableDTO getBreDatatableDTO(Object request, Object response, String loanId) {
    return BreDataUtil.getBreDatatableDTO(request, response, loanId);
  }

  /** Converts an error into a standardized error message to be used in BRE responses. */
  private String setErrorMessageInBreResponse(Throwable error) {
    // Differentiate between various exception types and return a corresponding error message.
    if (error instanceof ServerErrorException exception) {
      return gson.toJson(exception.getClientResponse());
    }
    if (error instanceof ClientSideException exception) {
      return gson.toJson(exception.getClientResponse());
    }
    if (error instanceof ForbiddenException exception) {
      return gson.toJson(exception.getClientResponse());
    }
    if (error instanceof BaseException exception) {
      return gson.toJson(exception.getClientResponse());
    }
    if (error instanceof NotFoundException exception) {
      return gson.toJson(exception.toString());
    }
    if (error instanceof UnsupportedMediaTypeException exception) {
      return gson.toJson(exception.toString());
    }
    // If the error type is unknown, return the error message as a string.
    return gson.toJson(error.getMessage());
  }

  /** Creates an initial BreStatus object when the BRE process is initiated. */
  private BreStatus createInitialBreStatus(Object request, String loanId, String productCode) {
    return BreStatus.builder()
        .externalId(loanId)
        .status(SUCCESS)
        .stage(BreStage.INITIATED.getDisplayName())
        .request(Json.of(gson.toJson(request)))
        .response(
            Json.of(gson.toJson(BreInitiatedResponseDTO.builder().status("Initiated").build())))
        .isActive(true)
        .productCode(productCode)
        .build();
  }

  /** Processes the response received from Scienaptic and generates the BRE response. */
  private Mono<Map<String, Object>> getBreResponse(
      String response, ProductControl.Flow flowData, String loanId) {
    try {
      // Try processing the BRE response with validation if applicable.
      boolean validate = (boolean) flowData.getConditions().get("validate");
      List<String> breResponseKeyList =
          Arrays.asList(flowData.getConditions().get("responseKeyList").toString().split(","));
      Map<String, Object> initialBreResponse =
          retainBreResponseFromJson(breResponseKeyList, response);
      initialBreResponse.put(LOANID, loanId);
      // If validation is required, validate the response, otherwise return it as is. This is to get
      // limit, roi and tenure based on conditions.
      if (validate) {
        return validateResponse(initialBreResponse, flowData);
      } else {
        initialBreResponse.put(LIMIT, initialBreResponse.getOrDefault(LOAN_AMOUNT, "-"));
        initialBreResponse.put(TENURE, initialBreResponse.getOrDefault(LOAN_TENURE, "-"));
        return Mono.just(initialBreResponse);
      }
    } catch (IllegalArgumentException | ClassCastException e) {
      return Mono.error(
          new BaseException("Invalid configuration for BRE processing", e, HttpStatus.BAD_REQUEST));
    }
  }

  /** Validates the initial BRE response and updates the response based on loan data if needed. */
  private Mono<Map<String, Object>> validateResponse(
      Map<String, Object> initialResponseMap, ProductControl.Flow flowData) {
    // Log the initial response and extract key details.
    log.info("[{}] initial response: {}", BRE_RESPONSE, gson.toJson(initialResponseMap));
    String loanId = (String) initialResponseMap.get(LOANID);
    String success = (String) initialResponseMap.get("success");
    String action = (String) initialResponseMap.get(ACTION);
    // If the action is 'ELIGIBLE', update the response with loan details.
    if (TRUE.equalsIgnoreCase(success) && ELIGIBLE.equalsIgnoreCase(action)) {
      boolean approvedLoanFlag =
          (boolean) flowData.getConditions().getOrDefault("approvedLoanFlag", false);
      if (approvedLoanFlag) {
        return updateInitialResponseMap(initialResponseMap, flowData);
      } else {
        return loanApplicationService
            .getLoanApplicationByLoanId(loanId)
            .map(this::convertToHashMap)
            .flatMap(
                loanApplicationMap ->
                    updateInitialResponseMap(initialResponseMap, loanApplicationMap))
            .defaultIfEmpty(initialResponseMap);
      }
    } else {
      initialResponseMap.remove(LOANS);
      return Mono.just(initialResponseMap);
    }
  }

  /** Converts a loan application object into a HashMap for processing. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> convertToHashMap(Object loanApplication) {
    if (loanApplication instanceof HashMap) {
      return (Map<String, Object>) loanApplication;
    } else {
      return new HashMap<>();
    }
  }

  /** Updates the initial BRE response with loan details based on the application. */
  @SuppressWarnings("unchecked")
  private Mono<Map<String, Object>> updateInitialResponseMap(
      Map<String, Object> initialResponseMap, Map<String, Object> loanApplicationMap) {
    // Extract requested tenure and loan amount from the loan application.
    Integer tenureRequested = (Integer) loanApplicationMap.get("numberOfRepayments");
    Double loanAmountRequested = (Double) loanApplicationMap.get("loanAmountRequested");

    // If either tenure or loan amount is missing, remove loan details and return the response.
    if (tenureRequested == null || loanAmountRequested == null) {
      initialResponseMap.remove(LOANS);
      return Mono.just(initialResponseMap);
    }
    // Fetch the loans from the initial response.
    List<Map<String, Object>> loans = (List<Map<String, Object>>) initialResponseMap.get(LOANS);
    if (loans == null || loans.isEmpty()) {
      initialResponseMap.remove(LOANS);
      return Mono.just(initialResponseMap);
    }
    // Step 1: Attempt to match a loan based on the requested tenure.
    return Mono.defer(
        () -> {
          Optional<Map<String, Object>> matchedLoan =
              loans.stream().filter(loan -> tenureRequested.equals(loan.get("edi"))).findFirst();
          // Step 2: If a matching loan is found based on requested tenure, update the response with
          // relevant loan details.
          if (matchedLoan.isPresent()) {
            Map<String, Object> loan = matchedLoan.get();
            Integer limit = (Integer) loan.get(FINAL_LIMIT);
            if (limit != null) {
              // Step 3: Compare the requested loan amount with the approved limit.
              if (loanAmountRequested.intValue() <= limit) {
                initialResponseMap.put(LIMIT, loanAmountRequested.intValue());
                initialResponseMap.put(ACTION, APPROVED);
              } else {
                initialResponseMap.put(LIMIT, limit);
                initialResponseMap.put(REASONS, List.of(REQUESTED_AMT_GREATER_THAN_APPROVED_AMT));
              }
              initialResponseMap.put(TENURE, loan.get("edi"));
              initialResponseMap.put(ROI, loan.get(ROI));
            }
            // Remove the loans list from the response and return the updated response.
            initialResponseMap.remove(LOANS);
            log.info("[{}] final response: {}", BRE_RESPONSE, gson.toJson(initialResponseMap));
            return Mono.just(initialResponseMap);
          } else {
            // Step 4: If no matching loan is found, return the response with an ineligible status.
            return Mono.just(handleNonMatchingLoan(initialResponseMap));
          }
        });
  }

  /** Generates a Scienaptic request DTO based on the request and response data. */
  private Object getScienapticRequestWithBankDataAndGstData(
      Object request,
      String productCode,
      BankUnderwritingGstUnderwritingResponseDTO bankDataAndGstData) {
    return BreDataUtil.getScienapticDetailsDTOWithBankDataAndGstData(
        request, productCode, bankDataAndGstData);
  }

  @SuppressWarnings("unchecked")
  private Mono<Map<String, Object>> updateInitialResponseMap(
      Map<String, Object> initialResponseMap, ProductControl.Flow flowData) {
    // Extract the approved loan response keys
    List<String> approvedLoanResponseKeys =
        Arrays.asList(
            flowData.getConditions().get("approvedLoanResponseKeys").toString().split(","));

    // Extract the field renaming mappings from flowData and convert them into a Map
    String fieldRenameString =
        flowData.getConditions().get("fieldRename").toString(); // e.g., "emi=EMI,tenure=TENURE"
    Map<String, String> fieldRenameMap =
        Arrays.stream(fieldRenameString.split(","))
            .map(mapping -> mapping.split(":"))
            .collect(Collectors.toMap(mapping -> mapping[0], mapping -> mapping[1]));

    // Safely process the loans list
    List<Map<String, Object>> loanDetails =
        ((List<Map<String, Object>>) initialResponseMap.get(LOANS))
            .stream()
                .map(
                    loan ->
                        loan.entrySet().stream()
                            .filter(entry -> approvedLoanResponseKeys.contains(entry.getKey()))
                            .collect(
                                Collectors.toMap(
                                    entry ->
                                        fieldRenameMap.getOrDefault(
                                            entry.getKey(),
                                            entry.getKey()), // Rename fields based on map
                                    Map.Entry::getValue)))
                .toList();

    // Update the initialResponseMap with the filtered loan details
    initialResponseMap.put("data", loanDetails);
    initialResponseMap.remove(LOANS);

    return Mono.just(initialResponseMap);
  }

  /** Handles the scenario where no matching loan is found in the initial response. */
  private Map<String, Object> handleNonMatchingLoan(Map<String, Object> initialResponseMap) {
    // Set the action to 'INELIGIBLE' and add reasons for ineligibility.
    initialResponseMap.put(ACTION, INELIGIBLE);
    initialResponseMap.put(REASONS, List.of(TENURE_NOT_FOUNT));
    initialResponseMap.remove(LOANS);
    // Log and return the final response.
    log.info("[{}] final response: {}", BRE_RESPONSE, gson.toJson(initialResponseMap));
    return initialResponseMap;
  }
}
