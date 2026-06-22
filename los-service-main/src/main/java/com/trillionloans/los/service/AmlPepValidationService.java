package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.AML_PEP_REDIS_KEY;
import static com.trillionloans.los.constant.StringConstants.CANNOT_BE_DONE;
import static com.trillionloans.los.constant.StringConstants.DIGIO_AML_NAME_MATCH_DEFAULT_VALUE;
import static com.trillionloans.los.constant.StringConstants.FAILED;
import static com.trillionloans.los.constant.StringConstants.LOAN_CREATE_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.MANUAL_REVIEW;
import static com.trillionloans.los.constant.StringConstants.REJECT;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.service.validationservice.ValidationFunnelService.buildFullName;

import com.trillionloans.los.api.partner.DigioApi;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.config.AmlPepConfig;
import com.trillionloans.los.model.ClientCacheDTO;
import com.trillionloans.los.model.dto.QcChecksDataTableDTO;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.AmlPepResultsEntity;
import com.trillionloans.los.model.request.digio.AmlPepCheckRequest;
import com.trillionloans.los.repository.AmlPepResultsRepository;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.RedisCacheService;
import com.trillionloans.los.util.EncryptionUtil;
import com.trillionloans.los.util.JsonUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Service
public class AmlPepValidationService {
  private final RedisCacheService redisCacheService;
  private final ProductConfigMasterService productConfigMasterService;
  private final DigioApi digioApi;
  private final AmlPepResultsRepository amlPepResulstRepository;
  private final long digioResponseCacheTtl;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;
  private final EncryptionUtil encryptionUtil;
  private final M2PWrapperApi m2PWrapperApi;

  @Value("${digio.screening.mode:SCREENING}")
  private String digioScreeningMode;

  @Value("${digio.enable-monitoring:false}")
  private boolean enableMonitoring;

  @Value("${digio.screening.type:SCREEN_ONLY_RESPONSE}")
  private String digioScreeningType;

  @Value("${digio.monitoring.frequency:900}")
  private Integer digioMonitoringFrequency;

  @Value("${digio.response.mode:DETAILED}")
  private String digioResponseMode;

  @Value("${digio.cutoff.threshold:80}")
  private Integer digioCutOffThreshold;

  @Value(
      "#{'${digio.rules:RID260202185516982TKZ39OWV7XT4YG,RID260202184358055SUUF2PFL42HDJQ}'.split(',')}")
  private List<String> digioRules;

  @Autowired
  public AmlPepValidationService(
      RedisCacheService redisCacheService,
      ProductConfigMasterService productConfigMasterService,
      DigioApi digioApi,
      AmlPepResultsRepository amlPepResulstRepository,
      @Value("${cache.digio-response.ttl-seconds:86400}") long digioResponseCacheTtl,
      LoanLevelClientDetailsService loanLevelClientDetailsService,
      EncryptionUtil encryptionUtil,
      M2PWrapperApi m2PWrapperApi) {
    this.redisCacheService = redisCacheService;
    this.productConfigMasterService = productConfigMasterService;
    this.digioApi = digioApi;
    this.amlPepResulstRepository = amlPepResulstRepository;
    this.digioResponseCacheTtl = digioResponseCacheTtl;
    this.loanLevelClientDetailsService = loanLevelClientDetailsService;
    this.encryptionUtil = encryptionUtil;
    this.m2PWrapperApi = m2PWrapperApi;
  }

  public Mono<AmlPepResultsEntity> launchAmlPepValidationIfEnabled(
      String productCode, String loanApplicationId, LoanLevelClientDetailsCacheDTO clientDetails) {

    return getAmlPepValidationConfig(productCode)
        .flatMap(
            isFeatureEnabled -> {
              if (Boolean.FALSE.equals(isFeatureEnabled.getAmlPepFeatureFlag())) {
                log.info(
                    "[AML_PEP_VERIFY] Feature flag is OFF. Skipping AML/PEP validation for"
                        + " productCode={}",
                    productCode);
                return Mono.empty();
              }

              return performAmlPepValidationAndStore(
                      clientDetails, loanApplicationId, isFeatureEnabled)
                  .onErrorResume(
                      e -> {
                        log.error(
                            "[AML_PEP_VERIFY][VALIDATION] AML/PEP validation failed for"
                                + " clientId={}, error={}",
                            clientDetails.getClientId(),
                            e.getMessage());

                        AmlPepProcessor.AmlPepMatchDetails fallback =
                            new AmlPepProcessor.AmlPepMatchDetails();
                        fallback.setPepMatch(CANNOT_BE_DONE);
                        fallback.setAmlStatus(CANNOT_BE_DONE);
                        fallback.setAmlFuzzyMatchScore(DIGIO_AML_NAME_MATCH_DEFAULT_VALUE);
                        fallback.setServiceStatus(FAILED);

                        return storeAmlPepResultWithDigioError(
                                clientDetails,
                                fallback,
                                e.getMessage(),
                                loanApplicationId,
                                isFeatureEnabled)
                            .onErrorResume(
                                storeError -> {
                                  log.error(
                                      "[AML_PEP_VERIFY][STORE_ERROR] Failed to store"
                                          + " Digio fallback for clientId={}, error={}",
                                      clientDetails.getClientId(),
                                      storeError.getMessage());
                                  return Mono.empty();
                                });
                      });
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private static final AmlPepConfig DEFAULT_AML_PEP_CONFIG;

  static {
    DEFAULT_AML_PEP_CONFIG = new AmlPepConfig();
    DEFAULT_AML_PEP_CONFIG.setAmlPepFeatureFlag(false);
    DEFAULT_AML_PEP_CONFIG.setAmlCheckEnabled(true);
    DEFAULT_AML_PEP_CONFIG.setPepCheckEnabled(true);
    DEFAULT_AML_PEP_CONFIG.setAmlRejectionThreshold(90.0);
    DEFAULT_AML_PEP_CONFIG.setAmlManualVerificationThreshold(60.0);
  }

  private Mono<AmlPepConfig> getAmlPepValidationConfig(String productCode) {

    log.info(
        "[AML_PEP_VERIFY][CONFIG] Fetching AML/PEP validation config for productCode={}",
        productCode);

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productConfigTuple -> {
              ProductControl.Flow flow =
                  productConfigMasterService.getFlowFromProductConfig(
                      productConfigTuple.getT2(), LOAN_CREATE_CTA_IDENTIFIER);

              if (flow == null || flow.getAmlPepConfig() == null) {
                log.warn(
                    "[AML_PEP_VERIFY][CONFIG] AML/PEP config missing for productCode={}. Using"
                        + " default",
                    productCode);
                return Mono.just(DEFAULT_AML_PEP_CONFIG);
              }

              AmlPepConfig config = flow.getAmlPepConfig();

              if (config.getAmlPepFeatureFlag() == null) {
                config.setAmlPepFeatureFlag(false);
              }

              log.info(
                  "[AML_PEP_VERIFY][CONFIG] Loaded AML/PEP config for productCode={} => {}",
                  productCode,
                  config);

              return Mono.just(config);
            })
        .switchIfEmpty(Mono.just(DEFAULT_AML_PEP_CONFIG))
        .onErrorResume(
            ex -> {
              log.error(
                  "[AML_PEP_VERIFY][CONFIG] Error fetching AML/PEP config for productCode={}. Using"
                      + " default",
                  productCode,
                  ex);
              return Mono.just(DEFAULT_AML_PEP_CONFIG);
            });
  }

  private AmlPepCheckRequest buildAmlPepRequest(LoanLevelClientDetailsCacheDTO clientCache) {
    log.debug(
        "[AML_PEP_REQUEST] Building AML/PEP request for clientId={}, PAN={}",
        clientCache.getClientId(),
        clientCache.getPanNumber());
    String fullName =
        buildFullName(
            clientCache.getFirstName(), clientCache.getMiddleName(), clientCache.getLastName());

    AmlPepCheckRequest request =
        AmlPepCheckRequest.builder()
            .userDetails(
                AmlPepCheckRequest.UserDetails.builder()
                    .name(fullName)
                    .pan(clientCache.getPanNumber())
                    .build())
            .screeningMode(digioScreeningMode)
            .screeningSettings(
                AmlPepCheckRequest.ScreeningSettings.builder()
                    .enableMonitoring(enableMonitoring)
                    .screeningType(digioScreeningType)
                    .monitoringFrequency(digioMonitoringFrequency)
                    .responseMode(digioResponseMode)
                    .cutOffThreshold(digioCutOffThreshold)
                    .build())
            .rules(digioRules)
            .build();

    log.debug(
        "[AML_PEP_VERIFY][REQUEST_CREATION] Request prepared for clientId={}, PAN={}",
        clientCache.getClientId(),
        clientCache.getPanNumber());

    return request;
  }

  private Mono<AmlPepResultsEntity> performAmlPepValidationAndStore(
      LoanLevelClientDetailsCacheDTO clientCache,
      String loanApplicationId,
      AmlPepConfig amlPepConfig) {
    log.info(
        "[AML_PEP_VERIFY][VALIDATION] Starting AML/PEP validation for clientId={}, PAN={}",
        clientCache.getClientId(),
        clientCache.getPanNumber());

    return validateAmlPep(clientCache)
        .flatMap(
            details -> storeAmlPepResult(clientCache, details, loanApplicationId, amlPepConfig))
        .doOnSuccess(
            entity ->
                log.info(
                    "[AML_PEP_VERIFY][VALIDATION] AML/PEP validation completed and stored for"
                        + " clientId={}, recordId={}",
                    clientCache.getClientId(),
                    entity.getId()))
        .doOnError(
            error ->
                log.error(
                    "[[AML_PEP_VERIFY][VALIDATION] Error during AML/PEP validation for clientId={},"
                        + " error={}",
                    clientCache.getClientId(),
                    error.getMessage(),
                    error));
  }

  private Mono<AmlPepProcessor.AmlPepMatchDetails> validateAmlPep(
      LoanLevelClientDetailsCacheDTO clientCache) {
    AmlPepCheckRequest request = buildAmlPepRequest(clientCache);

    return digioApi
        .performAmlPepScreening(request)
        .doOnSubscribe(
            sub ->
                log.info(
                    "[AML_PEP_VERIFY][SCREENING] Sending AML/PEP request to Digio for clientId={}",
                    clientCache.getClientId()))
        .doOnSuccess(
            resp ->
                log.debug(
                    "[AML_PEP_VERIFY][SCREENING] Received AML/PEP response for clientId={},"
                        + " response={}",
                    clientCache.getClientId(),
                    resp))
        .map(
            response -> {
              log.info(
                  "[AML_PEP_VERIFY][PROCESS_RESPONSE] Processing AML/PEP response for clientId={}",
                  clientCache.getClientId());
              return AmlPepProcessor.processDetailedResponse(response, request);
            });
  }

  private Mono<AmlPepResultsEntity> storeAmlPepResult(
      LoanLevelClientDetailsCacheDTO clientCache,
      AmlPepProcessor.AmlPepMatchDetails details,
      String loanApplicationId,
      AmlPepConfig amlPepConfig) {

    log.info(
        "[AML_PEP_VERIFY][STORING] Persisting AML/PEP → M2P QC → Async Cache clientId={}",
        clientCache.getClientId());

    return persistAmlPepResult(clientCache, details, loanApplicationId, amlPepConfig)

        // -------- REQUIRED STEP : M2P QC UPDATE --------
        .flatMap(
            savedEntity -> {
              if (Boolean.TRUE.equals(amlPepConfig.getDecoupleFlag())) {
                // Skip QC update
                log.info(
                    "[AML_PEP_VERIFY][AML_PEP_QC_UPDATE] Decoupled mode enabled. Skipping M2P QC"
                        + " update.");
                return Mono.just(savedEntity);
              }

              // Execute QC update
              return updateAmlPepQcInM2P(details, loanApplicationId, amlPepConfig)
                  .thenReturn(savedEntity);
            })

        // -------- NON BLOCKING SIDE EFFECT : REDIS CACHE --------
        .doOnSuccess(
            savedEntity ->
                cacheAmlPepResult(clientCache, details, amlPepConfig)
                    .doOnError(
                        e ->
                            log.error(
                                "[AML_PEP_CACHE][ERROR] Cache failed clientId={}, error={}",
                                clientCache.getClientId(),
                                e.getMessage(),
                                e))
                    .subscribe());
  }

  private Mono<Void> cacheAmlPepResult(
      LoanLevelClientDetailsCacheDTO clientCache,
      AmlPepProcessor.AmlPepMatchDetails details,
      AmlPepConfig amlPepConfig) {

    String redisKey = AML_PEP_REDIS_KEY + clientCache.getClientId();

    log.info(
        "[AML_PEP_VERIFY][STORING][CACHE] Caching AML/PEP result for clientId={}",
        clientCache.getClientId());

    AmlPepProcessor.AmlPepMatchDetails cacheObject =
        AmlPepProcessor.AmlPepMatchDetails.builder()
            .serviceStatus(details.getServiceStatus())
            .pepMatch(details.getPepMatch())
            .amlFuzzyMatchScore(details.getAmlFuzzyMatchScore())
            .amlStatus(mapAmlStatus(details, amlPepConfig))
            .build();

    return redisCacheService
        .cacheEncryptedObjectSilently(redisKey, cacheObject, digioResponseCacheTtl)
        .timeout(Duration.ofSeconds(2))
        .doOnSuccess(
            v ->
                log.info(
                    "[AML_PEP_VERIFY][STORING][CACHE] Successfully cached result for clientId={}",
                    clientCache.getClientId()))
        .doOnError(
            e ->
                log.warn(
                    "[AML_PEP_VERIFY][STORING][CACHE] Failed to cache result for clientId={},"
                        + " error={}",
                    clientCache.getClientId(),
                    e.getMessage()))
        .onErrorResume(e -> Mono.empty())
        .then();
  }

  private Mono<AmlPepResultsEntity> persistAmlPepResult(
      LoanLevelClientDetailsCacheDTO clientCache,
      AmlPepProcessor.AmlPepMatchDetails details,
      String loanApplicationId,
      AmlPepConfig amlPepConfig) {

    return amlPepResulstRepository
        .findByLeadId(loanApplicationId)
        .flatMap(
            existing -> {
              log.info("[AML_PEP_DB] Existing record found. Updating leadId={}", loanApplicationId);

              existing.setRequest(
                  encryptionUtil.encrypt(JsonUtils.serializeResponse(details.getRequest())));
              existing.setResponse(
                  encryptionUtil.encrypt(JsonUtils.serializeResponse(details.getResponse())));
              existing.setPepDecision(details.getPepMatch());
              existing.setAmlDecision(mapAmlStatus(details, amlPepConfig));
              existing.setProductId(clientCache.getProductCode());
              existing.setAmlFuzzyMatchScore(details.getAmlFuzzyMatchScore());
              existing.setServiceStatus(details.getServiceStatus());
              existing.setUpdatedAt(java.time.LocalDateTime.now());

              return amlPepResulstRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[AML_PEP_DB] No existing record. Creating new leadId={}", loanApplicationId);

                  AmlPepResultsEntity entity =
                      AmlPepResultsEntity.builder()
                          .clientId(clientCache.getClientId())
                          .productId(clientCache.getProductCode())
                          .leadId(loanApplicationId)
                          .request(
                              encryptionUtil.encrypt(
                                  JsonUtils.serializeResponse(details.getRequest())))
                          .response(
                              encryptionUtil.encrypt(
                                  JsonUtils.serializeResponse(details.getResponse())))
                          .pepDecision(details.getPepMatch())
                          .amlDecision(mapAmlStatus(details, amlPepConfig))
                          .amlFuzzyMatchScore(details.getAmlFuzzyMatchScore())
                          .serviceStatus(SUCCESS)
                          .createdAt(java.time.LocalDateTime.now())
                          .build();

                  return amlPepResulstRepository.save(entity);
                }))
        .doOnSuccess(
            saved ->
                log.info(
                    "[AML_PEP_DB] Persist success clientId={}, recordId={}",
                    clientCache.getClientId(),
                    saved.getId()))
        .doOnError(
            e ->
                log.error(
                    "[AML_PEP_DB] Persist failed clientId={}, error={}",
                    clientCache.getClientId(),
                    e.getMessage(),
                    e));
  }

  public Mono<AmlPepProcessor.AmlPepMatchDetails> fetchAmlPepResult(
      ClientCacheDTO clientCache, String loanApplicationId, AmlPepConfig amlPepConfig) {

    String redisKey = AML_PEP_REDIS_KEY + clientCache.getClientId();

    return redisCacheService
        .getEncryptedObjectSilently(redisKey, AmlPepProcessor.AmlPepMatchDetails.class)

        //  REDIS HIT
        .doOnNext(
            details ->
                log.info(
                    "[AML_PEP_VERIFY][FETCH][REDIS] Redis hit for clientId={}",
                    clientCache.getClientId()))

        // REDIS MISS → DB CHECK
        .switchIfEmpty(
            fetchAmlPepResultFromDB(clientCache.getClientId(), loanApplicationId)
                .doOnNext(
                    details ->
                        log.info(
                            "[AML_PEP_VERIFY][FETCH][DB] DB hit for clientId={}",
                            clientCache.getClientId()))

                //  DB MISS → DIGIO CALL
                .switchIfEmpty(
                    Mono.defer(
                        () -> {
                          log.info(
                              "[AML_PEP_VERIFY][FETCH] Cache + DB miss. Calling Digio for"
                                  + " clientId={}",
                              clientCache.getClientId());

                          return loanLevelClientDetailsService
                              .fetchLoanLevelClientDetails(
                                  clientCache.getClientId().toString(),
                                  loanApplicationId,
                                  clientCache.getProductCode())
                              .flatMap(
                                  freshClientCache ->
                                      validateAmlPep(freshClientCache)
                                          .flatMap(
                                              details ->
                                                  storeAmlPepResult(
                                                          freshClientCache,
                                                          details,
                                                          loanApplicationId,
                                                          amlPepConfig)
                                                      .thenReturn(details))
                                          .onErrorResume(
                                              e -> {
                                                log.error(
                                                    "[AML_PEP_VERIFY][FETCH][DIGIO_API_FAILED]"
                                                        + " clientId={}, error={}",
                                                    freshClientCache.getClientId(),
                                                    e.getMessage(),
                                                    e);

                                                AmlPepProcessor.AmlPepMatchDetails fallback =
                                                    new AmlPepProcessor.AmlPepMatchDetails();

                                                fallback.setPepMatch(CANNOT_BE_DONE);
                                                fallback.setServiceStatus(FAILED);

                                                return storeAmlPepResultWithDigioError(
                                                        freshClientCache,
                                                        fallback,
                                                        e.getMessage(),
                                                        loanApplicationId,
                                                        amlPepConfig)
                                                    .thenReturn(fallback);
                                              }));
                        })));
  }

  private Mono<AmlPepProcessor.AmlPepMatchDetails> fetchAmlPepResultFromDB(
      Integer clientId, String leadId) {
    return amlPepResulstRepository
        .findFirstByClientIdAndLeadId(clientId.toString(), leadId)
        .map(
            entity -> {
              AmlPepProcessor.AmlPepMatchDetails details = new AmlPepProcessor.AmlPepMatchDetails();
              details.setServiceStatus(entity.getServiceStatus());
              details.setPepMatch(entity.getPepDecision());
              details.setAmlFuzzyMatchScore(entity.getAmlFuzzyMatchScore());
              details.setAmlStatus(entity.getAmlDecision());
              return details;
            });
  }

  private Mono<AmlPepResultsEntity> storeAmlPepResultWithDigioError(
      LoanLevelClientDetailsCacheDTO clientCache,
      AmlPepProcessor.AmlPepMatchDetails details,
      String errorMsg,
      String loanApplicationId,
      AmlPepConfig amlPepConfig) {

    AmlPepResultsEntity entity =
        AmlPepResultsEntity.builder()
            .clientId(clientCache.getClientId())
            .pepDecision(CANNOT_BE_DONE)
            .productId(clientCache.getProductCode())
            .amlFuzzyMatchScore(details.getAmlFuzzyMatchScore())
            .amlDecision(CANNOT_BE_DONE)
            .reasonDescription(errorMsg)
            .createdAt(LocalDateTime.now())
            .serviceStatus(FAILED)
            .leadId(loanApplicationId)
            .build();

    String redisKey = AML_PEP_REDIS_KEY + clientCache.getClientId();

    Mono<Void> cacheMono =
        redisCacheService
            .cacheEncryptedObjectSilently(redisKey, details, digioResponseCacheTtl)
            .doOnError(
                e ->
                    log.error(
                        "[AML_PEP_CACHE][ERROR] Cache failed clientId={}, error={}",
                        clientCache.getClientId(),
                        e.getMessage(),
                        e))
            .then();

    return amlPepResulstRepository
        .save(entity)

        // -------- REQUIRED STEP : QC UPDATE EVEN ON ERROR --------
        .flatMap(
            savedEntity -> {
              if (Boolean.TRUE.equals(amlPepConfig.getDecoupleFlag())) {
                log.info(
                    "AML_PEP_VERIFY][AML_PEP_QC_UPDATE] Decoupled mode enabled. Skipping M2P QC"
                        + " update.");
                return Mono.just(savedEntity);
              }

              return updateAmlPepQcInM2P(details, loanApplicationId, amlPepConfig)
                  .thenReturn(savedEntity);
            })

        // -------- NON BLOCKING CACHE --------
        .doOnSuccess(savedEntity -> cacheMono.subscribe());
  }

  private Mono<Void> updateAmlPepQcInM2P(
      AmlPepProcessor.AmlPepMatchDetails details, String loanId, AmlPepConfig amlPepConfig) {

    // --------- Prepare PEP ---------
    String pepStatus = mapPepStatus(details.getPepMatch());

    // Vendor Service Status
    String serviceStatus =
        JsonUtils.serializeResponse(Map.of("serviceStatus", details.getServiceStatus()));

    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put("serviceStatus", details.getServiceStatus());
    dataMap.put("bestMatchScore", details.getAmlFuzzyMatchScore());

    String amlserviceStatus = JsonUtils.serializeResponse(dataMap);

    // --------- Prepare AML ---------
    String amlStatus = mapAmlStatus(details, amlPepConfig);
    String amlScore = JsonUtils.serializeResponse(details.getAmlFuzzyMatchScore());

    log.info(
        "[KYC_QC][AML_PEP] Updating AML & PEP QC in M2P loanId={}, pepStatus={}, amlStatus={}",
        loanId,
        pepStatus,
        amlStatus);

    return Mono.whenDelayError(
            updateQcCheckInM2P("pep-check", pepStatus, serviceStatus, null, loanId),
            updateQcCheckInM2P("aml", amlStatus, amlserviceStatus, amlScore, loanId))
        .timeout(Duration.ofSeconds(4))
        .doOnSuccess(
            v -> log.info("[KYC_QC][AML_PEP] AML & PEP QC update success loanId={}", loanId))
        .doOnError(
            e ->
                log.error(
                    "[KYC_QC][AML_PEP][ERROR] AML/PEP QC update failed loanId={}, error={}",
                    loanId,
                    e.getMessage(),
                    e))
        .then();
  }

  private String mapPepStatus(String pepMatch) {

    if (pepMatch == null) {
      return "UNKNOWN";
    }

    return switch (pepMatch) {
      case "PASS", "VERIFIED" -> "VERIFIED";
      case "FAIL", "REJECTED" -> "REJECTED";
      case "CAN_NOT_BE_DONE" -> "CAN_NOT_BE_DONE";
      default -> "UNKNOWN";
    };
  }

  private String mapAmlStatus(
      AmlPepProcessor.AmlPepMatchDetails details, AmlPepConfig amlPepConfig) {

    // -------- BASIC VALIDATION ----------
    if (details == null || amlPepConfig == null) {
      return "CAN_NOT_BE_DONE";
    }

    // -------- DIRECT STATUS OVERRIDE ----------
    String amlStatus = details.getAmlStatus();
    if (amlStatus != null && !amlStatus.isBlank()) {
      return amlStatus;
    }

    Double amlScore = details.getAmlFuzzyMatchScore();

    if (amlScore == null
        || amlScore.isNaN()
        || amlScore.isInfinite()
        || amlScore < 0
        || amlScore > 100) {

      return "CAN_NOT_BE_DONE";
    }

    Double rejectionThreshold = amlPepConfig.getAmlRejectionThreshold();
    Double manualThreshold = amlPepConfig.getAmlManualVerificationThreshold();

    // -------- REJECT ----------
    if (rejectionThreshold != null && amlScore >= rejectionThreshold) {
      return REJECT;
    }

    // -------- MANUAL REVIEW ----------
    if (manualThreshold != null && amlScore >= manualThreshold) {
      return MANUAL_REVIEW;
    }

    // -------- PASS ----------
    return "VERIFIED";
  }

  private Mono<Void> updateQcCheckInM2P(
      String checkName, String status, String data, String score, String loanId) {

    QcChecksDataTableDTO qcDto =
        QcChecksDataTableDTO.builder()
            .checkName(checkName)
            .status(status)
            .score(score)
            .data(data)
            .locale("en")
            .dateFormat("dd MMMM yyyy")
            .build();

    log.info(
        "[KYC_QC] Updating QC check in M2P loanId={}, checkName={}, status={}",
        loanId,
        checkName,
        status);

    return m2PWrapperApi
        .updateQcChecksDataTable(qcDto, loanId)

        // ---- resilience ----
        .timeout(Duration.ofSeconds(4))
        .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))

        // ---- logging ----
        .doOnSuccess(
            r ->
                log.info(
                    "[KYC_QC] QC check update success loanId={}, checkName={}", loanId, checkName))
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC][ERROR] QC update failed loanId={}, checkName={}, error={}",
                    loanId,
                    checkName,
                    error.getMessage(),
                    error))
        .then();
  }
}
