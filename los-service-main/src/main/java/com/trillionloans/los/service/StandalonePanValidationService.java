package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.KYC_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.LOAN_CREATE_CTA_IDENTIFIER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.NSDLV3API;
import com.trillionloans.los.config.PanValidationConfig;
import com.trillionloans.los.exception.PanValidationExceptions.PanVerificationException;
import com.trillionloans.los.mapper.OpvResponseCode;
import com.trillionloans.los.mapper.PanStatus;
import com.trillionloans.los.mapper.SeedingStatus;
import com.trillionloans.los.model.NsdlPanVerificationResponse;
import com.trillionloans.los.model.PanEvaluationResult;
import com.trillionloans.los.model.PanVerificationResult;
import com.trillionloans.los.model.dto.NsdlRejectionType;
import com.trillionloans.los.model.dto.PanVerificationLog;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.request.NsdlPanVerificationRequest;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.RedisCacheService;
import com.trillionloans.los.util.PanValidationUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * Service responsible for PAN validation flow: - Fetch cached PAN validation result from Redis (to
 * avoid redundant OPV calls). - If cache miss → call OPV API. - Evaluate OPV response against
 * configured PAN validation rules. - Persist results asynchronously.
 */
@Slf4j
@Service
public class StandalonePanValidationService {
  private final NSDLV3API nsdlV3API;
  private final long opvResponseCacheTtl;
  private final M2PWrapperApi m2PWrapperApi;
  private final RedisCacheService redisCacheService;
  private final ProductConfigMasterService productConfigMasterService;

  private static final String NSDL_RESPONSE_REDIS_KEY_PREFIX = "pan_verification_response";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired
  public StandalonePanValidationService(
      NSDLV3API nsdlV3API,
      M2PWrapperApi m2PWrapperApi,
      RedisCacheService redisCacheService,
      @Value("${cache.opv-response.ttl-seconds:86400}") long opvResponseCacheTtl,
      ProductConfigMasterService productConfigMasterService) {
    this.nsdlV3API = nsdlV3API;
    this.m2PWrapperApi = m2PWrapperApi;
    this.redisCacheService = redisCacheService;
    this.opvResponseCacheTtl = opvResponseCacheTtl;
    this.productConfigMasterService = productConfigMasterService;
  }

  /**
   * Validate PAN for a client: 1. Try Redis cache. 2. If not cached → call OPV API, evaluate
   * response and async cache result.
   *
   * @return a {@link Mono} emitting {@link PanVerificationResult} containing the PAN validation
   *     outcome
   */
  public Mono<PanVerificationResult> validatePan(
      Mono<NsdlPanVerificationRequest> panRequestMono,
      String productCode,
      String clientId,
      String loanApplicationId) {

    log.info(
        "[PAN_VERIFY][PAN_VALIDATION] Starting PAN validation. clientId={}, productCode={}",
        clientId,
        productCode);

    return panRequestMono
        .flatMap(
            panRequest -> {
              return fetchFromCache(panRequest, productCode, clientId)
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.info(
                                "[PAN_VERIFY][PAN_VALIDATION] Proceeding with OPV verification."
                                    + " clientId={}",
                                clientId);

                            return nsdlV3API
                                .verify(List.of(panRequest), clientId, loanApplicationId)
                                .flatMap(
                                    opvResponse ->
                                        Mono.deferContextual(
                                            ctx -> {
                                              asyncCachePanValidationResponseSilently(
                                                      Mono.just(panRequest),
                                                      opvResponse,
                                                      clientId,
                                                      productCode)
                                                  .contextWrite(ctx)
                                                  .subscribe();

                                              return evaluateOpvResult(opvResponse, productCode);
                                            }));
                          }));
            })
        .onErrorMap(
            e -> {
              log.error(
                  "[PAN_VERIFY][PAN_VALIDATION][ERROR] PAN validation failed. clientId={},"
                      + " productCode={}, error={}",
                  clientId,
                  productCode,
                  e.getMessage());

              return new PanVerificationException(
                  "Error verifying PAN", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            });
  }

  /** Try fetching PAN verification response from Redis cache. */
  private Mono<PanVerificationResult> fetchFromCache(
      NsdlPanVerificationRequest panRequest, String productCode, String clientId) {

    log.info(
        "[PAN_VERIFY][PAN_RESULT_CACHE] Attempting to fetch PAN validation result from Redis"
            + " cache");

    String cacheKey = buildCacheKey(productCode, clientId, panRequest.getPan());

    return redisCacheService
        .getKey(cacheKey)
        .flatMap(
            cachedResponse -> {
              if (cachedResponse == null || cachedResponse.isEmpty()) {
                log.info("[PAN_VERIFY][PAN_RESULT_CACHE] Cache miss.");
                return Mono.empty();
              }

              log.info("[PAN_VERIFY][PAN_RESULT_CACHE] Cache hit.");

              try {
                NsdlPanVerificationResponse opvResponse =
                    OBJECT_MAPPER.readValue(cachedResponse, NsdlPanVerificationResponse.class);
                return evaluateOpvResult(opvResponse, productCode)
                    .doOnNext(
                        result ->
                            log.info(
                                "[PAN_VERIFY][PAN_RESULT_CACHE] Successfully cached evaluated"
                                    + " response."))
                    .onErrorResume(
                        e -> {
                          log.warn(
                              "[PAN_VERIFY][PAN_RESULT_CACHE] Failed to evaluate cached response."
                                  + " Error={}",
                              e.getMessage());
                          return Mono.empty(); // fallback to OPV call
                        });
              } catch (Exception e) {
                log.warn(
                    "[PAN_VERIFY][PAN_RESULT_CACHE] Failed to parse cached response. Error={}",
                    e.getMessage());
                return Mono.empty();
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("[PAN_VERIFY][PAN_RESULT_CACHE] Cache miss.");
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.warn(
                  "[PAN_VERIFY][PAN_RESULT_CACHE][ERROR] Redis operation failed. Error={}",
                  e.getMessage());
              return Mono.empty(); // fallback to OPV call
            });
  }

  /** Cache OPV PAN verification response asynchronously (only if successful). */
  private Mono<Void> asyncCachePanValidationResponseSilently(
      Mono<NsdlPanVerificationRequest> panRequestMono,
      NsdlPanVerificationResponse response,
      String clientID,
      String productCode) {
    log.info("[PAN_VERIFY][OPV_RESPONSE_CACHE] Attempting to cache OPV response async");

    // Only cache successful responses
    if (!OpvResponseCode.SUCCESS.getCode().equals(response.getResponseCode())) {
      log.info(
          "[PAN_VERIFY][OPV_RESPONSE_CACHE] Skipping cache since OPV responseCode is NOT SUCCESS.");
      return Mono.empty();
    }

    return panRequestMono
        .map(NsdlPanVerificationRequest::getPan)
        .flatMap(
            pan -> {
              String key = buildCacheKey(productCode, clientID, pan);
              log.info("[PAN_VERIFY][OPV_RESPONSE_CACHE] Caching OPV response.");

              return redisCacheService
                  .cacheObjectSilently(key, response, opvResponseCacheTtl)
                  .doOnSuccess(
                      v ->
                          log.info(
                              "[PAN_VERIFY][OPV_RESPONSE_CACHE] Successfully cached OPV response."))
                  .doOnError(
                      e ->
                          log.error(
                              "[PAN_VERIFY][OPV_RESPONSE_CACHE] Failed to cache OPV response."
                                  + " Error={}",
                              e.getMessage()));
            });
  }

  /** Transform and evaluate OPV response into internal PAN validation result. */
  private Mono<PanVerificationResult> evaluateOpvResult(
      NsdlPanVerificationResponse opvApiResponse, String productCode) {
    log.info("[PAN_VERIFY][EVALUATE_OPV_RESPONSE] Evaluating OPV response");

    if (Objects.isNull(opvApiResponse)) {
      log.warn(
          "[PAN_VERIFY][EVALUATE_OPV_RESPONSE] Empty OPV response received. Skipping evaluating"
              + " record.");
      return Mono.just(new PanVerificationResult());
    }

    if (OpvResponseCode.SUCCESS.getCode().equals(opvApiResponse.getResponseCode())) {
      return Flux.fromIterable(opvApiResponse.getOutputData())
          .flatMap(
              response -> {
                log.info(
                    "[PAN_VERIFY][EVALUATE_OPV_RESPONSE] Received Success OPV response. Proceeding"
                        + " to evaluate the record");

                // Evaluate PAN record as per the config set
                Mono<PanEvaluationResult> validationOutcome =
                    isValidPanRecord(response, productCode);

                // Build vendor response record
                PanVerificationResult.Record panResultRecord =
                    PanVerificationResult.Record.builder()
                        .pan(response.getPan())
                        .panStatus(response.getPanStatus())
                        .panStatusDesc(PanStatus.fromCode(response.getPanStatus()).getDescription())
                        .nameMatch(response.getName())
                        .dobMatch(response.getDob())
                        .fathersNameMatch(response.getFatherName())
                        .seedingStatus(response.getSeedingStatus())
                        .seedingStatusDesc(
                            SeedingStatus.fromCode(response.getSeedingStatus()).getDescription())
                        .build();

                // Build evaluation result reactively
                return validationOutcome.map(
                    validation ->
                        PanVerificationResult.VerificationResult.builder()
                            .evaluationResult(
                                PanVerificationResult.EvaluationResult.builder()
                                    .status(
                                        validation.isValid()
                                            ? PanVerificationResult.Status.APPROVED
                                            : PanVerificationResult.Status.REJECTED)
                                    .rejectionReasons(validation.getRejectionReasons())
                                    .rejectionMessage(
                                        PanValidationUtil.getRejectionMessage(
                                            validation.getRejectionReasons()))
                                    .build())
                            .vendorResponse(panResultRecord)
                            .build());
              })
          .collectList()
          .map(
              results ->
                  PanVerificationResult.builder()
                      .responseCode(opvApiResponse.getResponseCode())
                      .responseCodeDesc(
                          OpvResponseCode.fromCode(opvApiResponse.getResponseCode())
                              .getDescription())
                      .panVerificationResults(results)
                      .build())
          .doOnSuccess(
              result -> {
                log.info("[PAN_VERIFY][EVALUATE_OPV_RESPONSE] Completed evaluation.");
                log.info(
                    "[PAN_VERIFY][EVALUATE_OPV_RESPONSE] Final result: {}",
                    result.toMaskedLogString());
              });
    } else {
      log.warn(
          "[PAN_VERIFY][EVALUATE_OPV_RESPONSE] Non-success OPV responseCode={} description={}",
          opvApiResponse.getResponseCode(),
          OpvResponseCode.fromCode(opvApiResponse.getResponseCode()).getDescription());

      return Mono.just(
          PanVerificationResult.builder()
              .responseCode(opvApiResponse.getResponseCode())
              .responseCodeDesc(
                  OpvResponseCode.fromCode(opvApiResponse.getResponseCode()).getDescription())
              .panVerificationResults(Collections.emptyList())
              .build());
    }
  }

  /** Apply configured validation rules against vendor PAN record. */
  public Mono<PanEvaluationResult> isValidPanRecord(
      NsdlPanVerificationResponse.OutputData record, String productCode) {

    if (Objects.isNull(record)) {
      log.warn("[PAN_VERIFY][VALIDATE_RECORD] Received null record.");
      return Mono.just(new PanEvaluationResult(false, List.of(StringUtils.EMPTY)));
    }

    return getStandalonePanValidationConfig(productCode)
        .flatMap(
            config ->
                isAadhaarPanLinkEnforced(productCode)
                    .map(
                        isLinkCheckEnabled -> {
                          List<String> reasons = new ArrayList<>();

                          checkMismatch(
                              NsdlRejectionType.PAN_STATUS.getFieldName(),
                              config.isPanStatusCheckEnabled(),
                              config.getPanStatusExpected(),
                              record.getPanStatus(),
                              reasons);

                          if (isLinkCheckEnabled) {
                            checkMismatch(
                                NsdlRejectionType.SEEDING_STATUS.getFieldName(),
                                config.isSeedingStatusCheckEnabled(),
                                config.getSeedingStatusExpectedValue(),
                                record.getSeedingStatus(),
                                reasons);
                          }

                          checkMismatch(
                              NsdlRejectionType.NAME_STATUS.getFieldName(),
                              config.isNameMatchCheckEnabled(),
                              config.getNameMatchStatusExpected(),
                              record.getName(),
                              reasons);

                          checkMismatch(
                              NsdlRejectionType.DOB_STATUS.getFieldName(),
                              config.isDobMatchCheckEnabled(),
                              config.getDobMatchStatusExpected(),
                              record.getDob(),
                              reasons);

                          boolean isValid = reasons.isEmpty();
                          return new PanEvaluationResult(isValid, reasons);
                        }));
  }

  private void checkMismatch(
      String field, boolean enabled, String expected, String actual, List<String> reasons) {

    if (enabled && !expected.equalsIgnoreCase(actual)) {
      reasons.add(String.format("%s mismatch (expected: %s, found: %s)", field, expected, actual));
    }
  }

  public boolean isPanValid(PanVerificationResult results) {
    return OpvResponseCode.SUCCESS.getCode().equals(results.getResponseCode())
        && Objects.nonNull(results.getPanVerificationResults())
        && !results.getPanVerificationResults().isEmpty()
        && results.getPanVerificationResults().get(0).getEvaluationResult().getStatus()
            == PanVerificationResult.Status.APPROVED;
  }

  public Mono<Void> logInDb(PanVerificationLog panVerificationLog, String clientId) {
    if (Objects.isNull(clientId)) {
      log.error(
          "[PAN_VERIFY][STORE_PAN_VERIFICATION_RESULT] Skipped PAN result persistence because"
              + " clientId is null");
      return Mono.empty();
    }

    return m2PWrapperApi
        .persistPanVerificationResults(panVerificationLog, clientId)
        .doOnSuccess(
            result -> {
              if ("SKIPPED".equals(result)) {
                log.info(
                    "[PAN_VERIFY][STORE_PAN_VERIFICATION_RESULT] Skipped PAN result persistence due"
                        + " to missing config. clientId={}",
                    clientId);
              } else {
                log.info(
                    "[PAN_VERIFY][STORE_PAN_VERIFICATION_RESULT] Successfully persisted PAN"
                        + " validation result. clientId={}",
                    clientId);
              }
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[PAN_VERIFY][STORE_PAN_VERIFICATION_RESULT] Failed to store PAN verification"
                      + " result for clientId={}, error={}",
                  clientId,
                  e.getMessage());
              return Mono.empty();
            })
        .then();
  }

  public Mono<PanValidationConfig> getStandalonePanValidationConfig(String productCode) {

    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);

    return productConfigTuple.flatMap(
        productControlConfigData -> {
          ProductControl.Flow loanCreateFlowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), LOAN_CREATE_CTA_IDENTIFIER);

          if (Objects.isNull(loanCreateFlowData)
              || Objects.isNull(loanCreateFlowData.getPanValidationConfig())) {
            log.info(
                "[PAN_VERIFY] No flow data found for PAN validation. Returning default disabled"
                    + " config.");

            // Return default PanValidationConfig with all flags disabled
            PanValidationConfig defaultConfig =
                PanValidationConfig.builder()
                    .panValidationFeatureFlag(false)
                    .panStatusCheckEnabled(false)
                    .nameMatchCheckEnabled(false)
                    .dobMatchCheckEnabled(false)
                    .panStatusExpected(null)
                    .nameMatchStatusExpected(null)
                    .dobMatchStatusExpected(null)
                    .seedingStatusCheckEnabled(false)
                    .seedingStatusExpectedValue("NA")
                    .build();

            return Mono.just(defaultConfig);
          }

          return Mono.just(loanCreateFlowData.getPanValidationConfig());
        });
  }

  public Mono<Boolean> isAadhaarPanLinkEnforced(String productCode) {

    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);

    return productConfigTuple
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow kycCallbackFlowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), KYC_CALLBACK_IDENTIFIER);

              if (Objects.isNull(kycCallbackFlowData)) {
                log.info("[PAN_VERIFY] No flow data found for KYC callback. Defaulting to false.");

                return Mono.just(false);
              }

              return Mono.just(kycCallbackFlowData.isAadhaarPanLinkEnforce());
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[PAN_VERIFY] Error while checking Aadhaar-PAN link status for productCode: {}."
                      + " Defaulting to false.",
                  productCode,
                  error);
              return Mono.just(false);
            });
  }

  private String buildCacheKey(String productCode, String clientId, String pan) {
    return NSDL_RESPONSE_REDIS_KEY_PREFIX + ":" + productCode + ":" + clientId + ":" + pan;
  }
}
