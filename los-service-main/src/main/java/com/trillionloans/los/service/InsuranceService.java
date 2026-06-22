package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.LOAN_CREATE_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.UPDATE_LOAN_LOG_HEADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.AssurekitApi;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.config.InsuranceConfig;
import com.trillionloans.los.constant.DocumentTag;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.mapper.InsuranceStatus;
import com.trillionloans.los.model.dto.AssurekitCreatePlanRequest;
import com.trillionloans.los.model.dto.AssurekitCreatePlanResponse;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.InsuranceDatatableDTO;
import com.trillionloans.los.model.entity.LoanInsuranceDetailsEntity;
import com.trillionloans.los.model.partner.m2p.M2pBulkDocumentsUploadDTO;
import com.trillionloans.los.model.request.SaveChargeRequest;
import com.trillionloans.los.model.request.UpdateLoanApplication;
import com.trillionloans.los.model.request.m2p.M2pDisbursementCallBackRequest;
import com.trillionloans.los.model.response.m2p.M2pDocumentsUploadResponseDTO;
import com.trillionloans.los.repository.LoanInsuranceDetailsRepository;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.util.DateTimeConverterUtil;
import io.r2dbc.postgresql.codec.Json;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

@Slf4j
@Service
public class InsuranceService {

  private static final double PREMIUM_AMOUNT_MATCH_TOLERANCE = 0.01d;

  private final ObjectMapper objectMapper;
  private final ProductConfigMasterService productConfigMasterService;
  private final LoanInsuranceDetailsRepository loanInsuranceDetailsRepository;
  private final AssurekitApi assurekitApi;
  private final M2PWrapperApi m2PWrapperApi;
  private final String assurekitProgramId;

  public InsuranceService(
      ObjectMapper objectMapper,
      ProductConfigMasterService productConfigMasterService,
      LoanInsuranceDetailsRepository loanInsuranceDetailsRepository,
      AssurekitApi assurekitApi,
      M2PWrapperApi m2PWrapperApi,
      @Value("${assurekit.program-id:CRDUSGIZWZDEMRT}") String assurekitProgramId) {
    this.objectMapper = objectMapper;
    this.productConfigMasterService = productConfigMasterService;
    this.loanInsuranceDetailsRepository = loanInsuranceDetailsRepository;
    this.assurekitApi = assurekitApi;
    this.m2PWrapperApi = m2PWrapperApi;
    this.assurekitProgramId = assurekitProgramId;
  }

  public Mono<InsuranceConfig> getInsuranceConfig(String productCode) {
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);

    return productConfigTuple.flatMap(
        productControlConfigData -> {
          ProductControl.Flow loanCreateFlowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), LOAN_CREATE_CTA_IDENTIFIER);

          if (Objects.isNull(loanCreateFlowData)
              || Objects.isNull(loanCreateFlowData.getInsuranceConfig())) {
            log.info(
                "[INSURANCE][CONFIG] No flow data found for Insurance. Returning default"
                    + "config.");
            InsuranceConfig defaultConfig =
                InsuranceConfig.builder().insuranceFeatureFlag(false).build();
            return Mono.just(defaultConfig);
          }
          return Mono.just(loanCreateFlowData.getInsuranceConfig());
        });
  }

  public Mono<LoanInsuranceDetailsEntity> savePremiumGridToTable(
      String loanApplicationId,
      String clientId,
      List<InsuranceConfig.InsuranceGridRow> insuranceGridRows,
      String productId) {
    String premiumGridJson = serializePremiumGrid(insuranceGridRows);
    LoanInsuranceDetailsEntity entity =
        LoanInsuranceDetailsEntity.builder()
            .loanApplicationId(Integer.parseInt(loanApplicationId))
            .clientId(Integer.parseInt(clientId))
            .premiumGrid(Json.of(premiumGridJson))
            .productId(productId)
            .build();
    return loanInsuranceDetailsRepository
        .save(entity)
        .doOnSuccess(
            savedEntity ->
                log.info(
                    "[INSURANCE][STORING][DB] Successfully saved client loan and premium grid"
                        + " details: entityId={}, loanApplicationId={}",
                    savedEntity.getId(),
                    savedEntity.getLoanApplicationId()))
        .doOnError(
            error ->
                log.error(
                    "[INSURANCE][STORING][DB] Failed to save premium details for"
                        + " loanApplicationId={}: {}",
                    loanApplicationId,
                    error.getMessage(),
                    error));
  }

  private String serializePremiumGrid(List<InsuranceConfig.InsuranceGridRow> insuranceGridRows) {
    try {
      return objectMapper.writeValueAsString(
          insuranceGridRows == null ? List.of() : insuranceGridRows);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize insurance premium grid.", ex);
    }
  }

  public Mono<LoanInsuranceDetailsEntity> saveChargeInfo(
      String loanApplicationId,
      Boolean insuranceOpted,
      Boolean success,
      Double premiumAmount,
      Double loanAmount) {
    return loanInsuranceDetailsRepository
        .findFirstByLoanApplicationIdOrderByIdDesc(Integer.parseInt(loanApplicationId))
        .flatMap(
            existingEntity -> {
              existingEntity.setIsChargeAdded(success);
              existingEntity.setIsOpted(insuranceOpted);
              existingEntity.setUpdatedAt(LocalDateTime.now());
              existingEntity.setPremiumAmount(premiumAmount);
              existingEntity.setSumInsured(
                  resolveSumInsuredFromGrid(
                      existingEntity.getPremiumGrid(), loanAmount, loanApplicationId));
              return loanInsuranceDetailsRepository
                  .save(existingEntity)
                  .doOnSuccess(
                      savedEntity ->
                          log.info(
                              "[INSURANCE][ADD_CHARGE][STORING][DB] Updated insurance record saved"
                                  + " for loanApplicationId {} with status {}",
                              loanApplicationId,
                              savedEntity.getIsChargeAdded()));
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[INSURANCE][ADD_CHARGE][STORING][DB] No existing insurance record found for"
                          + " loanApplicationId {}. Skipping save.",
                      loanApplicationId);
                  return Mono.empty();
                }));
  }

  public Mono<SaveChargeRequest> createInsuranceChargeRequest(
      String loanId, ProductControl.Flow flow, Double loanAmount) {

    return loanInsuranceDetailsRepository
        .findFirstByLoanApplicationIdOrderByIdDesc(Integer.parseInt(loanId))
        .flatMap(
            entity -> {
              if (loanAmount == null) {
                log.warn(
                    "[INSURANCE][ADD_CHARGE][GET_PREMIUM_GRID] Loan amount is null for"
                        + " loanApplicationId {}",
                    loanId);
                return Mono.error(
                    new IllegalStateException("Loan amount is not set for loan: " + loanId));
              }

              Double premiumAmount =
                  resolvePremiumFromGrid(entity.getPremiumGrid(), loanAmount, loanId);
              log.info(
                  "[INSURANCE][ADD_CHARGE][GET_PREMIUM_PERCENTAGE] Premium amount {} fetched"
                      + " for loanApplicationId {}",
                  premiumAmount,
                  loanId);
              int insuranceChargeId = flow.getInsuranceConfig().getInsuranceChargeId();
              SaveChargeRequest saveChargeRequest =
                  SaveChargeRequest.builder()
                      .chargeId(insuranceChargeId)
                      .amount(premiumAmount)
                      .isAmountNonEditable(Boolean.TRUE)
                      .isMandatory(Boolean.FALSE)
                      .build();
              return Mono.just(saveChargeRequest);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "[{}] No insurance details entity found for loanApplicationId {}. Cannot"
                          + " build insurance charge request.",
                      UPDATE_LOAN_LOG_HEADER,
                      loanId);
                  return Mono.error(
                      new IllegalStateException(
                          "No insurance details entity found for loan: " + loanId));
                }));
  }

  private Double resolvePremiumFromGrid(Json premiumGridJson, Double loanAmount, String loanId) {
    return resolveValueFromGrid(premiumGridJson, loanAmount, loanId, "premiumWithGst");
  }

  private Double resolveSumInsuredFromGrid(Json premiumGridJson, Double loanAmount, String loanId) {
    return resolveValueFromGrid(premiumGridJson, loanAmount, loanId, "sumInsured");
  }

  private Double resolveValueFromGrid(
      Json premiumGridJson, Double loanAmount, String loanId, String targetField) {
    JsonNode matchedRow = findMatchingGridRow(premiumGridJson, loanAmount, loanId);
    if (!matchedRow.hasNonNull(targetField)) {
      throw new IllegalStateException(
          "No "
              + targetField
              + " configured for loan amount "
              + loanAmount
              + " and loan: "
              + loanId);
    }
    return matchedRow.get(targetField).asDouble();
  }

  private String resolveAssurekitPlanNameFromGrid(
      Json premiumGridJson, Double loanAmount, String loanId) {
    JsonNode matchedRow = findMatchingGridRow(premiumGridJson, loanAmount, loanId);
    if (matchedRow.hasNonNull("assureKitPlanName")) {
      return matchedRow.get("assureKitPlanName").asText();
    }
    if (matchedRow.hasNonNull("assurekitPlanName")) {
      return matchedRow.get("assurekitPlanName").asText();
    }
    throw new IllegalStateException(
        "No assureKitPlanName configured in premium grid for loan amount "
            + loanAmount
            + " and loan: "
            + loanId);
  }

  private JsonNode findMatchingGridRow(Json premiumGridJson, Double loanAmount, String loanId) {
    if (loanAmount == null) {
      throw new IllegalStateException("Loan amount is not set for loan: " + loanId);
    }
    if (premiumGridJson == null
        || premiumGridJson.asString() == null
        || premiumGridJson.asString().isBlank()) {
      throw new IllegalStateException("Premium grid is not set for loan: " + loanId);
    }
    try {
      JsonNode gridNode = objectMapper.readTree(premiumGridJson.asString());
      if (!gridNode.isArray()) {
        throw new IllegalStateException("Premium grid format is invalid for loan: " + loanId);
      }
      for (JsonNode row : gridNode) {
        if (!row.hasNonNull("minAmount")) {
          continue;
        }
        double minAmount = row.get("minAmount").asDouble();
        boolean isMaxConfigured = row.hasNonNull("maxAmount");
        double maxAmount = isMaxConfigured ? row.get("maxAmount").asDouble() : Double.MAX_VALUE;
        if (loanAmount >= minAmount && loanAmount <= maxAmount) {
          return row;
        }
      }
      throw new IllegalStateException(
          "No premium grid row configured for loan amount " + loanAmount + " and loan: " + loanId);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to parse premium grid for loan: " + loanId, ex);
    }
  }

  public Mono<M2pDisbursementCallBackRequest> enrichWithAssurekitInsuranceDetailsIfVerified(
      M2pDisbursementCallBackRequest requestBody, ProductControl.Flow flowData) {
    final String traceId = MDC.get(TRACE_ID);

    if (!isInsuranceFeatureEnabled(flowData)) {
      log.info(
          "[INSURANCE][CONFIG] loanId={} reason=FeatureDisabled",
          requestBody.getLoanApplicationId());
      return Mono.just(requestBody);
    }

    if (!"Disbursed".equalsIgnoreCase(requestBody.getStatus())) {
      log.info(
          "[INSURANCE][CONFIG] loanId={} status={} reason=LoanNotInDisbursedState",
          requestBody.getLoanApplicationId(),
          requestBody.getStatus());
      return Mono.just(requestBody);
    }

    log.info(
        "[INSURANCE][CONFIG] Insurance Enrichment Feature Enabled for loanId={}",
        requestBody.getLoanApplicationId());

    return getVerifiedInsuranceEntity(requestBody.getLoanApplicationId().toString())
        .flatMap(
            insuranceEntity -> {
              if (!isPremiumAmountMatchingWithGrid(insuranceEntity, requestBody)) {
                return Mono.just(requestBody);
              }
              insuranceEntity.setDisbursedApprovedAmount(requestBody.getApprovedAmount());
              return buildAssurekitRequest(insuranceEntity, requestBody)
                  .doOnNext(
                      req ->
                          log.info(
                              "[INSURANCE][ASSUREKIT_REQUEST_BUILT] loanId={} planName={}"
                                  + " tenure={} months",
                              req.getLoanId(),
                              req.getPlanName(),
                              req.getTenureOfLoan()))
                  .flatMap(this::callAssurekitCreatePlan)
                  .flatMap(
                      assurekitResponse -> {

                        // Case 1: Assurekit API failed

                        if (assurekitResponse == null
                            || !Boolean.TRUE.equals(assurekitResponse.getStatus())
                            || assurekitResponse.getResult() == null) {

                          log.error(
                              "[INSURANCE][CREATE_PLAN_FAILED] loanId={} status={} message={}",
                              requestBody.getLoanApplicationId(),
                              assurekitResponse != null ? assurekitResponse.getStatus() : null,
                              assurekitResponse != null
                                  ? assurekitResponse.getMessage()
                                  : "null response");

                          insuranceEntity.setStatus(InsuranceStatus.FAILED.name());
                          insuranceEntity.setPolicyNumber(null);
                          insuranceEntity.setM2pDocId(null);
                          insuranceEntity.setDocUrl(null);

                          requestBody.setInsuranceDetails(
                              M2pDisbursementCallBackRequest.InsuranceDetails.builder()
                                  .insuranceStatus(InsuranceStatus.FAILED.name())
                                  .build());

                          return loanInsuranceDetailsRepository
                              .save(insuranceEntity)
                              .doOnSuccess(
                                  saved -> {
                                    log.info(
                                        "[INSURANCE] Saved FAILED insurance status for loanId={}",
                                        saved.getLoanApplicationId());
                                    // Async push to M2P Data Table
                                    m2PWrapperApi
                                        .saveInsuranceResult(
                                            buildInsuranceDto(saved), saved.getLoanApplicationId())
                                        .contextWrite(
                                            ctx ->
                                                traceId != null ? ctx.put(TRACE_ID, traceId) : ctx)
                                        .subscribe(
                                            success ->
                                                log.info(
                                                    "[INSURANCE] Pushed FAILED insurance data to"
                                                        + " M2P for loanId={}",
                                                    saved.getLoanApplicationId()),
                                            err ->
                                                log.error(
                                                    "[INSURANCE] Error pushing FAILED insurance"
                                                        + " data to M2P for loanId={} : {}",
                                                    saved.getLoanApplicationId(),
                                                    err.getMessage()));
                                  })
                              .thenReturn(requestBody);
                        }

                        // Case 2: AssureKit success — Try M2P upload

                        return uploadInsuranceDocumentToM2p(assurekitResponse, insuranceEntity)
                            .flatMap(
                                uploadResponse -> {
                                  // Both succeeded
                                  log.info(
                                      "[INSURANCE][M2P_UPLOAD_SUCCESS] loanId={} docId={}",
                                      requestBody.getLoanApplicationId(),
                                      uploadResponse.getDocuments().get(0).getDocumentId());
                                  M2pDisbursementCallBackRequest enrichedRequest =
                                      enrichRequestWithAssurekitResponse(
                                          requestBody, assurekitResponse, uploadResponse);

                                  return saveInsuranceDetailsToDBandM2p(
                                          insuranceEntity,
                                          assurekitResponse,
                                          uploadResponse,
                                          requestBody.getApprovedAmount().doubleValue())
                                      .doOnSuccess(
                                          v ->
                                              log.info(
                                                  "[INSURANCE][STORE] Create Plan And Upload Loan"
                                                      + " SUCCESS loanId={} policyId={}",
                                                  requestBody.getLoanApplicationId(),
                                                  assurekitResponse
                                                      .getResult()
                                                      .getProtectionPlanId()))
                                      .thenReturn(enrichedRequest);
                                })
                            .onErrorResume(
                                uploadEx -> {

                                  // Case 3: AssureKit success, but M2P upload failed

                                  log.error(
                                      "[INSURANCE][UPLOAD_INSURANCE_DOC] Upload to M2P failed for"
                                          + " loanId={} : {}",
                                      insuranceEntity.getLoanApplicationId(),
                                      uploadEx.getMessage());

                                  // Keep AssureKit policy details, but mark overall
                                  // insuranceStatus as FAILED
                                  insuranceEntity.setStatus(InsuranceStatus.SUCCESS.name());
                                  insuranceEntity.setPolicyNumber(
                                      assurekitResponse.getResult().getProgramId());
                                  insuranceEntity.setM2pDocId(null);
                                  insuranceEntity.setDocUrl(
                                      assurekitResponse
                                          .getResult()
                                          .getDownloadProtectionPlanLink());

                                  requestBody.setInsuranceDetails(
                                      M2pDisbursementCallBackRequest.InsuranceDetails.builder()
                                          .insuranceStatus(InsuranceStatus.SUCCESS.name())
                                          .insurancePolicyNumber(
                                              assurekitResponse.getResult().getProgramId())
                                          .insuranceDocId(null)
                                          .insuranceDocURL(
                                              assurekitResponse
                                                  .getResult()
                                                  .getDownloadProtectionPlanLink())
                                          .build());

                                  return loanInsuranceDetailsRepository
                                      .save(insuranceEntity)
                                      .doOnSuccess(
                                          saved ->
                                              log.info(
                                                  "[INSURANCE][STORE] Partial insurance saved"
                                                      + " (AssureKit ok, M2P fail) for loanId={}",
                                                  saved.getLoanApplicationId()))
                                      .thenReturn(requestBody);
                                });
                      });
            })
        .onErrorResume(
            ex -> {
              log.error(
                  "[INSURANCE][DEFAULT][ERROR] Unexpected error enriching AssureKit details for"
                      + " loanId={} : {}",
                  requestBody.getLoanApplicationId(),
                  ex.getMessage());
              return Mono.just(requestBody);
            })
        .switchIfEmpty(Mono.just(requestBody))
        .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx);
  }

  private Mono<LoanInsuranceDetailsEntity> getVerifiedInsuranceEntity(String loanApplicationId) {
    return loanInsuranceDetailsRepository
        .findFirstByLoanApplicationIdOrderByIdDesc(Integer.parseInt(loanApplicationId))
        .filter(entity -> InsuranceStatus.PENDING.name().equalsIgnoreCase(entity.getStatus()));
  }

  private boolean isPremiumAmountMatchingWithGrid(
      LoanInsuranceDetailsEntity insuranceEntity, M2pDisbursementCallBackRequest requestBody) {
    Integer loanId = insuranceEntity.getLoanApplicationId();
    if (requestBody.getApprovedAmount() == null) {
      log.warn(
          "[INSURANCE][PREMIUM_VALIDATION] loanId={} reason=ApprovedAmountMissing",
          requestBody.getLoanApplicationId());
      return false;
    }
    if (insuranceEntity.getPremiumAmount() == null) {
      log.warn(
          "[INSURANCE][PREMIUM_VALIDATION] loanId={} reason=PremiumAmountMissingInDB",
          requestBody.getLoanApplicationId());
      return false;
    }
    try {
      Double premiumAmountFromGrid =
          resolvePremiumFromGrid(
              insuranceEntity.getPremiumGrid(),
              requestBody.getApprovedAmount().doubleValue(),
              loanId.toString());
      boolean isMatching =
          Math.abs(premiumAmountFromGrid - insuranceEntity.getPremiumAmount())
              < PREMIUM_AMOUNT_MATCH_TOLERANCE;
      if (!isMatching) {
        log.warn(
            "[INSURANCE][PREMIUM_VALIDATION] loanId={} reason=PremiumMismatch"
                + " premiumAmountFromGrid={} premiumAmountInTable={}",
            requestBody.getLoanApplicationId(),
            premiumAmountFromGrid,
            insuranceEntity.getPremiumAmount());
      }
      return isMatching;
    } catch (RuntimeException ex) {
      log.error(
          "[INSURANCE][PREMIUM_VALIDATION] loanId={} reason=PremiumResolutionFailed error={}",
          requestBody.getLoanApplicationId(),
          ex.getMessage());
      return false;
    }
  }

  private Mono<M2pDocumentsUploadResponseDTO> uploadInsuranceDocumentToM2p(
      AssurekitCreatePlanResponse assurekitResponse, LoanInsuranceDetailsEntity insuranceEntity) {
    final String traceId = MDC.get(TRACE_ID);

    if (assurekitResponse == null
        || !Boolean.TRUE.equals(assurekitResponse.getStatus())
        || assurekitResponse.getResult() == null) {
      log.warn(
          "[INSURANCE][UPLOAD_INSURANCE_DOC] Skipping M2P document upload due to invalid AssureKit"
              + " response for loanId={}",
          insuranceEntity.getLoanApplicationId());
      return Mono.empty();
    }

    String loanId = insuranceEntity.getLoanApplicationId().toString();

    // Build document payload
    M2pBulkDocumentsUploadDTO.DocumentDetailsDTO insuranceDoc =
        M2pBulkDocumentsUploadDTO.DocumentDetailsDTO.builder()
            .tag(DocumentTag.INSURANCE_DOCUMENT) // DocumentTag.INSURANCE_DOC
            .document(
                M2pBulkDocumentsUploadDTO.DocumentInfoDTO.builder()
                    .fileName(
                        "insurance_" + assurekitResponse.getResult().getProtectionPlanId() + ".pdf")
                    .filePath(assurekitResponse.getResult().getDownloadProtectionPlanLink())
                    .fileType("application/pdf")
                    .storageType("URL")
                    .build())
            .build();

    M2pBulkDocumentsUploadDTO uploadRequest =
        M2pBulkDocumentsUploadDTO.builder().documents(List.of(insuranceDoc)).build();

    return m2PWrapperApi
        .uploadDocumentsAgainstLoan(loanId, uploadRequest)
        .cast(M2pDocumentsUploadResponseDTO.class)
        .flatMap(
            uploadResponse -> {
              if (uploadResponse.getDocuments() != null
                  && !uploadResponse.getDocuments().isEmpty()) {
                M2pDocumentsUploadResponseDTO.Doc firstDoc = uploadResponse.getDocuments().get(0);
                Integer docId = firstDoc.getDocumentId();

                insuranceEntity.setM2pDocId(docId);
                insuranceEntity.setDocUrl(
                    assurekitResponse.getResult().getDownloadProtectionPlanLink());
                insuranceEntity.setPolicyNumber(
                    assurekitResponse.getResult().getProtectionPlanId());
                insuranceEntity.setStatus(InsuranceStatus.SUCCESS.name());

                log.info(
                    "[INSURANCE][UPLOAD_INSURANCE_DOC] Uploaded insurance document for loanId={} →"
                        + " docId={}",
                    loanId,
                    docId);
                return Mono.just(uploadResponse);
              } else {
                log.warn("[INSURANCE] Empty upload response from M2P for loanId={}", loanId);
                return Mono.error(new RuntimeException("Empty upload response from M2P"));
              }
            })
        .retryWhen(
            Retry.backoff(3, Duration.ofSeconds(2))
                .filter(this::isRetryableM2pUploadFailure)
                .doBeforeRetry(
                    retrySignal ->
                        log.warn(
                            "[INSURANCE][UPLOAD_INSURANCE_DOC] Retrying upload for loanId={} "
                                + "attempt={} reason={}",
                            loanId,
                            retrySignal.totalRetries() + 1,
                            retrySignal.failure().getMessage())))
        // If anything inside this flow fails (like product config, validation, M2P API)
        .onErrorResume(
            ex -> {
              log.error(
                  "[INSURANCE][UPLOAD_INSURANCE_DOC] Error uploading document to M2P for loanId={}"
                      + " : {}",
                  loanId,
                  ex.getMessage(),
                  ex);
              // Propagate a descriptive error for outer handler
              return Mono.error(new RuntimeException("M2P_UPLOAD_FAILED", ex));
            })
        .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx);
  }

  private boolean isRetryableM2pUploadFailure(Throwable ex) {
    if (ex instanceof ClientSideException clientEx) {
      return clientEx.getHttpStatusCode() != null
          && clientEx.getHttpStatusCode().value() == HttpStatus.FORBIDDEN.value();
    }
    return false;
  }

  private Mono<Void> saveInsuranceDetailsToDBandM2p(
      LoanInsuranceDetailsEntity insuranceEntity,
      AssurekitCreatePlanResponse assurekitResponse,
      M2pDocumentsUploadResponseDTO uploadResponse,
      Double insurancePremiumAmount) {
    final String traceId = MDC.get(TRACE_ID);

    if (assurekitResponse != null && assurekitResponse.getResult() != null) {
      insuranceEntity.setPolicyNumber(assurekitResponse.getResult().getProtectionPlanId());
      insuranceEntity.setDocUrl(assurekitResponse.getResult().getDownloadProtectionPlanLink());
      insuranceEntity.setStatus(InsuranceStatus.SUCCESS.name());
      insuranceEntity.setPremiumAmount(
          resolvePremiumFromGrid(
              insuranceEntity.getPremiumGrid(),
              insurancePremiumAmount,
              insuranceEntity.getLoanApplicationId().toString()));
      insuranceEntity.setSumInsured(
          resolveSumInsuredFromGrid(
              insuranceEntity.getPremiumGrid(),
              insurancePremiumAmount,
              insuranceEntity.getLoanApplicationId().toString()));
    }

    if (uploadResponse != null
        && uploadResponse.getDocuments() != null
        && !uploadResponse.getDocuments().isEmpty()) {
      insuranceEntity.setM2pDocId(uploadResponse.getDocuments().get(0).getDocumentId());
    }

    return loanInsuranceDetailsRepository
        .save(insuranceEntity)
        .doOnSuccess(
            saved -> {
              log.info(
                  "[INSURANCE][STORING][DB] Saved successful insurance details for loanId={}"
                      + " policy={}",
                  saved.getLoanApplicationId(),
                  saved.getPolicyNumber());

              // Push final insurance record to M2P asynchronously
              m2PWrapperApi
                  .saveInsuranceResult(buildInsuranceDto(saved), saved.getLoanApplicationId())
                  .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx)
                  .subscribe(
                      success ->
                          log.info(
                              "[INSURANCE][STORING][M2P]  Successfully pushed insurance details to"
                                  + " M2P for loanId={}",
                              saved.getLoanApplicationId()),
                      err ->
                          log.error(
                              "[INSURANCE][STORING][M2P]  Failed to push insurance details to M2P"
                                  + " for loanId={} : {}",
                              saved.getLoanApplicationId(),
                              err.getMessage()));
            })
        .then()
        .contextWrite(ctx -> traceId != null ? ctx.put(TRACE_ID, traceId) : ctx);
  }

  private InsuranceDatatableDTO buildInsuranceDto(LoanInsuranceDetailsEntity entity) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");

    return InsuranceDatatableDTO.builder()
        .status(entity.getStatus())
        .policyNo(entity.getPolicyNumber())
        .m2pDocId(entity.getM2pDocId())
        .docUrl(entity.getDocUrl())
        .loanApplicationId(entity.getLoanApplicationId())
        .clientId(entity.getClientId())
        .isChargeAdded(entity.getIsChargeAdded())
        .isOpted(entity.getIsOpted())
        .premiumAmount(entity.getPremiumAmount())
        .createdAt(LocalDateTime.now().format(formatter))
        .locale("en")
        .dateFormat("dd MMMM yyyy HH:mm:ss")
        .build();
  }

  private Mono<AssurekitCreatePlanRequest> buildAssurekitRequest(
      LoanInsuranceDetailsEntity insuranceEntity, M2pDisbursementCallBackRequest requestBody) {

    // Extract identifiers for M2P API call
    Integer loanId = insuranceEntity.getLoanApplicationId();

    return m2PWrapperApi
        .getLoanInsuranceData(loanId.toString())
        .flatMap(
            loanInsuranceDto -> {

              // Convert date format safely
              String formattedStartDate =
                  DateTimeConverterUtil.convertToGivenDateFormat(
                      requestBody.getDisbursementDate(), "MMM d, yyyy", "yyyy-MM-dd");
              Double approvedAmount = requestBody.getApprovedAmount().doubleValue();
              String resolvedPlanName =
                  resolveAssurekitPlanNameFromGrid(
                      insuranceEntity.getPremiumGrid(), approvedAmount, loanId.toString());

              // Build Assurekit request directly from DTO
              AssurekitCreatePlanRequest request =
                  AssurekitCreatePlanRequest.builder()
                      .programId(assurekitProgramId)
                      .name(
                          Optional.ofNullable(loanInsuranceDto.getFullName())
                              .orElse(loanInsuranceDto.getName()))
                      .phone(loanInsuranceDto.getPhone())
                      .loanAmount(approvedAmount)
                      .loanStartTime(formattedStartDate)
                      .tenureOfLoan(12)
                      .loanId(String.valueOf(requestBody.getLanID()))
                      .planName(resolvedPlanName)
                      .build();

              return Mono.just(request);
            });
  }

  public Mono<AssurekitCreatePlanResponse> callAssurekitCreatePlan(
      AssurekitCreatePlanRequest request) {

    return assurekitApi
        .createPlan(request)
        .doOnNext(
            response ->
                log.info(
                    "[INSURANCE][CREATE_PLAN] AssureKit createPlan called for loanId={} →"
                        + " status={}, message={}",
                    request.getLoanId(),
                    response.getStatus(),
                    response.getMessage()))
        .onErrorResume(
            ex -> {
              log.error(
                  "[INSURANCE][CREATE_PLAN] Error calling AssureKit createPlan for loanId={} : {}",
                  request.getLoanId(),
                  ex.getMessage());
              AssurekitCreatePlanResponse failedResponse = new AssurekitCreatePlanResponse();
              failedResponse.setStatus(false);
              failedResponse.setMessage("Assurekit call failed: " + ex.getMessage());
              failedResponse.setResult(null);
              return Mono.just(failedResponse);
            });
  }

  private M2pDisbursementCallBackRequest enrichRequestWithAssurekitResponse(
      M2pDisbursementCallBackRequest requestBody,
      AssurekitCreatePlanResponse assurekitResponse,
      M2pDocumentsUploadResponseDTO uploadResponse) {

    if (assurekitResponse == null
        || !Boolean.TRUE.equals(assurekitResponse.getStatus())
        || assurekitResponse.getResult() == null) {
      log.warn(
          "[INSURANCE][ENRICHMENT] Skipping AssureKit enrichment for loanId={} due to invalid"
              + " AssureKit response",
          requestBody.getLoanApplicationId());
      return requestBody;
    }

    // Extract docId from upload response (if present)
    Integer uploadedDocId = null;
    if (uploadResponse != null
        && uploadResponse.getDocuments() != null
        && !uploadResponse.getDocuments().isEmpty()) {
      uploadedDocId = uploadResponse.getDocuments().get(0).getDocumentId();
    }

    // Enrich directly from Assurekit + upload
    requestBody.setInsuranceDetails(
        M2pDisbursementCallBackRequest.InsuranceDetails.builder()
            .insurancePolicyNumber(assurekitResponse.getResult().getProtectionPlanId())
            .insuranceStatus(InsuranceStatus.SUCCESS.name())
            .insuranceDocURL(assurekitResponse.getResult().getDownloadProtectionPlanLink())
            .insuranceDocId(uploadedDocId != null ? uploadedDocId : 0)
            .build());
    return requestBody;
  }

  public boolean isInsuranceEnabled(UpdateLoanApplication loanData, ProductControl.Flow flow) {

    if (loanData == null || flow == null || flow.getInsuranceConfig() == null) {
      return false;
    }
    Boolean isOpted = loanData.getIsInsuranceOpted();
    Boolean isFeatureFlagOn = flow.getInsuranceConfig().getInsuranceFeatureFlag();

    return Boolean.TRUE.equals(isOpted) && Boolean.TRUE.equals(isFeatureFlagOn);
  }

  public boolean isInsuranceFeatureEnabled(ProductControl.Flow flowData) {

    if (flowData == null || flowData.getInsuranceConfig() == null) {
      return false;
    }

    Boolean featureFlag = flowData.getInsuranceConfig().getInsuranceFeatureFlag();

    return Boolean.TRUE.equals(featureFlag);
  }
}
