package com.trillionloans.los.service.validationservice;

import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.StepName.NAME_SIMILARITY;
import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.Vendor.KARZA;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.KarzaApi;
import com.trillionloans.los.config.ValidationFunnelConfiguration;
import com.trillionloans.los.config.ValidationFunnelProperties;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.exception.PanValidationExceptions.KarzaPanAuthenticateExceptions.KarzaNameSimilarityValidationException;
import com.trillionloans.los.mapper.KarzaInternalStatusEnum;
import com.trillionloans.los.model.KarzaNameSimilarityRequest;
import com.trillionloans.los.model.ValidationStepEntity;
import com.trillionloans.los.model.dto.KarzaThresholdEvaluationResult;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.request.ClientValidationFunnelStatus;
import com.trillionloans.los.model.request.KarzaNameSimilarityResponse;
import com.trillionloans.los.model.request.KarzaNameSimilarityResult;
import com.trillionloans.los.service.db.ClientValidationFunnelStatusRepository;
import com.trillionloans.los.service.db.ClientValidationFunnelStepsRepository;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.util.JsonUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
public class KarzaNameSimilarityService
    implements ValidationService<
        KarzaNameSimilarityRequest, KarzaNameSimilarityResult, KarzaNameSimilarityResponse> {
  private final KarzaApi karzaApi;
  private static final String VENDOR_NAME = "KARZA";
  private final ValidationFunnelProperties properties;
  private final ValidationFunnelServiceUtil validationFunnelServiceUtil;
  private final ValidationFunnelConfigService validationFunnelConfigService;
  private final ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository;
  private final ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository;
  private final KafkaEventProducerService eventProducerService;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public KarzaNameSimilarityService(
      KarzaApi karzaApi,
      ValidationFunnelProperties properties,
      ValidationFunnelConfigService validationFunnelConfigService,
      ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository,
      ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository,
      ValidationFunnelServiceUtil validationFunnelServiceUtil,
      KafkaEventProducerService kafkaEventProducerService) {
    this.karzaApi = karzaApi;
    this.properties = properties;
    this.validationFunnelConfigService = validationFunnelConfigService;
    this.clientValidationFunnelStepsRepository = clientValidationFunnelStepsRepository;
    this.clientValidationFunnelStatusRepository = clientValidationFunnelStatusRepository;
    this.validationFunnelServiceUtil = validationFunnelServiceUtil;
    this.eventProducerService = kafkaEventProducerService;
  }

  @Override
  public String getVendorName() {
    return VENDOR_NAME;
  }

  @Override
  public Mono<KarzaNameSimilarityResult> init(
      Mono<KarzaNameSimilarityRequest> requestMono,
      String productCode,
      String clientId,
      String loanApplicationId) {
    log.info(
        "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][INIT] Starting Karza Name "
            + "Similarity validation. ClientId={}, productCode={}",
        clientId,
        productCode);

    return requestMono
        .flatMap(
            karzaNameSimilarityRequest -> {
              boolean skipCache = properties.isMasterFlag() && !properties.getCache().isEnabled();

              if (skipCache) {
                return callVendor(
                        karzaNameSimilarityRequest, clientId, productCode, loanApplicationId)
                    .map(Tuple2::getT1);
              }

              return readThroughCache(Mono.just(karzaNameSimilarityRequest), productCode, clientId)
                  .switchIfEmpty(
                      callVendor(
                              karzaNameSimilarityRequest, clientId, productCode, loanApplicationId)
                          .flatMap(
                              tuple -> {
                                KarzaNameSimilarityResult result = tuple.getT1();
                                KarzaNameSimilarityResponse response = tuple.getT2();
                                Event event =
                                    result.getEvaluationResult().getEvaluationSatus()
                                            == KarzaNameSimilarityResult.Status.APPROVED
                                        ? Event.KARZA_NAME_SIMILARITY_APPROVED
                                        : Event.KARZA_NAME_SIMILARITY_REJECTED;
                                publishEventKafkaAsync(
                                    () ->
                                        eventProducerService.publishEvent(
                                            new EventContext(event, loanApplicationId, clientId),
                                            null,
                                            null));
                                // cache write
                                return writeThroughCache(
                                        Mono.just(karzaNameSimilarityRequest),
                                        Mono.just(response),
                                        Mono.just(result),
                                        clientId,
                                        productCode)
                                    .thenReturn(result);
                              }));
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][ERROR] karza Name"
                      + " Similarity failed. ClientId={}, productCode={}, error={}",
                  clientId,
                  productCode,
                  e.getMessage());

              return validationFunnelServiceUtil
                  .persistFailure(
                      e, clientId, productCode, NAME_SIMILARITY, KARZA, "KARZA_VALIDATION_SERVICE")
                  .then(
                      Mono.error(
                          new KarzaNameSimilarityValidationException(
                              "Error performing karza Name Similarity",
                              e.getMessage(),
                              HttpStatus.INTERNAL_SERVER_ERROR)));
            });
  }

  Mono<Tuple2<KarzaNameSimilarityResult, KarzaNameSimilarityResponse>> callVendor(
      KarzaNameSimilarityRequest req,
      String clientId,
      String productCode,
      String loanApplicationId) {
    return karzaApi
        .checkNameSimilarity(req, clientId, loanApplicationId, "create_loan", null)
        .flatMap(
            resp ->
                this.evaluateResult(Mono.just(resp), productCode)
                    .flatMap(
                        evalResult ->
                            this.persist(
                                    Mono.just(req),
                                    Mono.just(resp),
                                    Mono.just(evalResult),
                                    clientId,
                                    productCode)
                                .thenReturn(Tuples.of(evalResult, resp))));
  }

  @Override
  public Mono<KarzaNameSimilarityResult> readThroughCache(
      Mono<KarzaNameSimilarityRequest> requestMono, String productCode, String clientId) {
    log.info(
        "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
            + " Attempting to fetch validation result from Redis cache. ClientId= {} productCode="
            + " {}",
        clientId,
        productCode);

    String cacheKey = validationFunnelServiceUtil.buildCacheKey(productCode, clientId);

    return validationFunnelServiceUtil
        .getObjectFromCache(cacheKey, ClientValidationFunnelStatus.class)
        .flatMap(
            clientValidationFunnelStatus -> {
              if (Objects.isNull(clientValidationFunnelStatus)) {
                log.info(
                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                        + " Cache miss.");
                return Mono.empty();
              }

              log.info(
                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                      + " Cache hit.");

              try {
                // Get the KARZA Name Similarity step
                Optional<ClientValidationFunnelStatus.ValidationStep> karzaNameSimiStepOpt =
                    clientValidationFunnelStatus.getSteps().stream()
                        .filter(
                            step ->
                                step.getStepName() == NAME_SIMILARITY && step.getVendor() == KARZA)
                        .findFirst();

                if (karzaNameSimiStepOpt.isEmpty()) {
                  log.info(
                      "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                          + " Karza Name Similarity step not found for clientID= {} productCode="
                          + " {}.",
                      clientId,
                      productCode);
                  return Mono.empty();
                }

                ClientValidationFunnelStatus.ValidationStep karzaNameSimilarityStep =
                    karzaNameSimiStepOpt.get();
                String karzaNameSimilarityStepResponseStr = karzaNameSimilarityStep.getResponse();

                KarzaNameSimilarityResponse karzaNameSimilarityResponse =
                    OBJECT_MAPPER.readValue(
                        karzaNameSimilarityStepResponseStr, KarzaNameSimilarityResponse.class);

                return this.evaluateResult(Mono.just(karzaNameSimilarityResponse), productCode)
                    .onErrorResume(
                        e -> {
                          log.warn(
                              "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                                  + " Failed to evaluate cached response. ClientId= {} productCode"
                                  + " ={} error={}",
                              clientId,
                              productCode,
                              e.getMessage());
                          return Mono.empty();
                        });
              } catch (Exception e) {
                log.warn(
                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                        + " Failed to parse cached response. ClientId= {} productCode= {} error={}",
                    clientId,
                    productCode,
                    e.getMessage());
                return Mono.empty();
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                          + " Cache miss. ClientId= {} productCode= {}.",
                      clientId,
                      productCode);
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][READ_THROUGH_CACHE][ERROR]"
                      + " Redis operation failed. ClientId= {} productCode= {} error={}",
                  clientId,
                  productCode,
                  e.getMessage());
              return Mono.empty();
            });
  }

  @Override
  public Mono<?> writeThroughCache(
      Mono<KarzaNameSimilarityRequest> requestMono,
      Mono<KarzaNameSimilarityResponse> responseMono,
      Mono<KarzaNameSimilarityResult> resultMono,
      String clientId,
      String productCode) {

    log.info(
        "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][WRITE_THROUGH_CACHE]"
            + " Attempting to cache karza Name Similarity response async. ClientId= {} productCode="
            + " {}",
        clientId,
        productCode);

    String key = validationFunnelServiceUtil.buildCacheKey(productCode, clientId);

    return Mono.zip(requestMono, responseMono, resultMono)
        .flatMap(
            tuple -> {
              KarzaNameSimilarityResponse response = tuple.getT2();
              KarzaNameSimilarityResult result = tuple.getT3();

              return validationFunnelServiceUtil
                  .getObjectFromCache(key, ClientValidationFunnelStatus.class)
                  .defaultIfEmpty(new ClientValidationFunnelStatus())
                  .flatMap(
                      funnelStatus -> {
                        boolean isSuccess =
                            KarzaInternalStatusEnum.CODE_101
                                .getCode()
                                .equals(String.valueOf(response.getStatusCode()));

                        Mono<ClientValidationFunnelStatus> updatedStatusMono;

                        if (!isSuccess) {
                          log.info(
                              "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][WRITE_THROUGH_CACHE]"
                                  + " Skipping cache since responseCode is NOT SUCCESS. ClientId="
                                  + " {} productCode= {}.",
                              clientId,
                              productCode);
                          updatedStatusMono =
                              Mono.just(
                                  createFailureWriteThroughCacheObejct(
                                      funnelStatus, response, result, clientId));
                        } else {
                          updatedStatusMono =
                              createWriteThroughCacheObject(
                                  funnelStatus, response, result, productCode);
                        }

                        return updatedStatusMono.flatMap(
                            status ->
                                validationFunnelServiceUtil
                                    .cacheObject(key, status)
                                    .doOnError(
                                        e ->
                                            log.error(
                                                "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][WRITE_THROUGH_CACHE]"
                                                    + " Failed to cache karza Name Similarity"
                                                    + " response. ClientId= {} productCode= {}"
                                                    + " error={}",
                                                clientId,
                                                productCode,
                                                e.getMessage()))
                                    .thenReturn(status));
                      });
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][WRITE_THROUGH_CACHE][ERROR]"
                      + " Redis operation failed. ClientId= {} productCode= {} error={}",
                  clientId,
                  productCode,
                  e.getMessage());
              return Mono.empty();
            });
  }

  private Mono<ClientValidationFunnelStatus> createWriteThroughCacheObject(
      ClientValidationFunnelStatus funnelStatus,
      KarzaNameSimilarityResponse response,
      KarzaNameSimilarityResult result,
      String productCode) {

    return validationFunnelConfigService
        .isDobWaterfallFunnelActiveMono(productCode)
        .map(
            isDobFunnelActive -> {
              KarzaNameSimilarityResult.Status evaluationStatus =
                  result.getEvaluationResult().getEvaluationSatus();

              // ---------- Final Status ----------
              if (evaluationStatus == KarzaNameSimilarityResult.Status.APPROVED) {

                funnelStatus.setFinalStatus(
                    isDobFunnelActive
                        ? ClientValidationFunnelStatus.FinalStatus.PENDING
                        : ClientValidationFunnelStatus.FinalStatus.PASS);

              } else if (evaluationStatus == KarzaNameSimilarityResult.Status.REJECTED) {

                funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.REJECT);

              } else if (evaluationStatus == KarzaNameSimilarityResult.Status.SERVICE_UNAVAILABLE) {

                funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW);

              } else {
                funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.PENDING);
              }

              // ---------- Step Status ----------
              ClientValidationFunnelStatus.StepStatus stepStatus =
                  evaluationStatus == KarzaNameSimilarityResult.Status.APPROVED
                      ? ClientValidationFunnelStatus.StepStatus.PASS
                      : ClientValidationFunnelStatus.StepStatus.REJECT;

              ClientValidationFunnelStatus.ValidationStep newStep =
                  new ClientValidationFunnelStatus.ValidationStep(
                      ClientValidationFunnelStatus.StepName.NAME_SIMILARITY,
                      ClientValidationFunnelStatus.Vendor.KARZA,
                      response.toString(),
                      stepStatus,
                      ClientValidationFunnelStatus.ServiceStatus.SUCCESS);

              List<ClientValidationFunnelStatus.ValidationStep> steps = funnelStatus.getSteps();
              boolean updated = false;

              for (int i = 0; i < steps.size(); i++) {
                ClientValidationFunnelStatus.ValidationStep existing = steps.get(i);
                if (existing.getStepName().equals(newStep.getStepName())
                    && existing.getVendor().equals(newStep.getVendor())) {
                  steps.set(i, newStep);
                  updated = true;
                  break;
                }
              }

              if (!updated) {
                steps.add(newStep);
              }

              return funnelStatus;
            });
  }

  private ClientValidationFunnelStatus createFailureWriteThroughCacheObejct(
      ClientValidationFunnelStatus funnelStatus,
      KarzaNameSimilarityResponse response,
      KarzaNameSimilarityResult result,
      String clientId) {
    funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW);

    // Create or update step
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            ClientValidationFunnelStatus.StepName.NAME_SIMILARITY,
            ClientValidationFunnelStatus.Vendor.KARZA,
            response.toString(), // store full response JSON
            null,
            ClientValidationFunnelStatus.ServiceStatus.FAILURE);

    List<ClientValidationFunnelStatus.ValidationStep> steps = funnelStatus.getSteps();
    boolean updated = false;

    for (int i = 0; i < steps.size(); i++) {
      ClientValidationFunnelStatus.ValidationStep existingStep = steps.get(i);
      if (existingStep.getStepName().equals(newStep.getStepName())
          && existingStep.getVendor().equals(newStep.getVendor())) {
        steps.set(i, newStep); // update existing step
        updated = true;
        break;
      }
    }

    if (!updated) {
      steps.add(newStep); // add new step if not found
    }

    return funnelStatus;
  }

  @Override
  public Mono<KarzaNameSimilarityResult> evaluateResult(
      Mono<KarzaNameSimilarityResponse> karzaNameSimilarityResponseMono, String productCode) {
    log.info(
        "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][EVALUATE_KARZA_NAME_SIMILARITY_RESPONSE]"
            + " Evaluating karza Name Similarity response");

    return karzaNameSimilarityResponseMono
        .flatMap(
            karzaNameSimilarityResponse -> {
              if (karzaNameSimilarityResponse == null) {
                log.warn(
                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][EVALUATE_KARZA_NAME_SIMILARITY_RESPONSE]"
                        + " Empty response received. Skipping evaluating record.");
                return Mono.just(new KarzaNameSimilarityResult());
              }

              String statusCode = String.valueOf(karzaNameSimilarityResponse.getStatusCode());

              if (KarzaInternalStatusEnum.CODE_101.getCode().equals(statusCode)) {
                log.info(
                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][EVALUATE_KARZA_NAME_SIMILARITY_RESPONSE]"
                        + " Received Success karza Name Similarity response. Proceeding to evaluate"
                        + " record");

                double actualScore = karzaNameSimilarityResponse.getResult().getScore();

                return checkThresholdValue(actualScore, productCode)
                    .map(
                        evaluation -> {
                          // Build evaluation result
                          KarzaNameSimilarityResult.EvaluationResult evaluationResult =
                              KarzaNameSimilarityResult.EvaluationResult.builder()
                                  .evaluationSatus(
                                      evaluation.isAccepted()
                                          ? KarzaNameSimilarityResult.Status.APPROVED
                                          : KarzaNameSimilarityResult.Status.REJECTED)
                                  .rejectionReason(
                                      evaluation.isAccepted()
                                          ? null
                                          : "actualScore="
                                              + actualScore
                                              + ", thresholdScore="
                                              + evaluation.getThresholdScore())
                                  .build();

                          // Build full result
                          KarzaNameSimilarityResult result =
                              KarzaNameSimilarityResult.builder()
                                  .status(statusCode)
                                  .statusDesc(KarzaInternalStatusEnum.fromCode(statusCode))
                                  .vendorResponse(karzaNameSimilarityResponse)
                                  .evaluationResult(evaluationResult)
                                  .build();

                          log.info("Karza Name Similarity Result: {}", result);
                          return result;
                        });

              } else {
                log.info(
                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][EVALUATE_KARZA_NAME_SIMILARITY_RESPONSE]"
                        + " Received un-successful Karza Name Similarity response.");

                KarzaNameSimilarityResult result =
                    KarzaNameSimilarityResult.builder()
                        .status(statusCode)
                        .statusDesc(KarzaInternalStatusEnum.fromCode(statusCode))
                        .vendorResponse(karzaNameSimilarityResponse)
                        .evaluationResult(null)
                        .build();

                log.info(
                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][EVALUATE_KARZA_NAME_SIMILARITY_RESPONSE]"
                        + " Karza Name Similarity Result: {}",
                    result);
                return Mono.just(result);
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][EVALUATE_KARZA_NAME_SIMILARITY_RESPONSE]"
                          + " Received empty Mono for karza Name Similarity Response.");
                  return Mono.just(new KarzaNameSimilarityResult());
                }))
        .onErrorResume(
            e -> {
              log.error(
                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][EVALUATE_KARZA_NAME_SIMILARITY_RESPONSE][ERROR]"
                      + " Failed to evaluate response: {}",
                  e.getMessage(),
                  e);
              return Mono.just(new KarzaNameSimilarityResult());
            });
  }

  private Mono<KarzaThresholdEvaluationResult> checkThresholdValue(
      Double actualScore, String productCode) {
    if (Objects.isNull(actualScore) || Objects.isNull(productCode) || productCode.isEmpty()) {
      return Mono.just(
          KarzaThresholdEvaluationResult.builder()
              .actualScore(actualScore)
              .thresholdScore(null)
              .accepted(false)
              .build());
    }

    return getKarzaNameSimilarityConfig(productCode)
        .map(
            config -> {
              Double thresholdScore = extractThresholdScore(config);
              boolean isAccepted = Objects.nonNull(thresholdScore) && actualScore >= thresholdScore;

              return KarzaThresholdEvaluationResult.builder()
                  .actualScore(actualScore)
                  .thresholdScore(thresholdScore)
                  .accepted(isAccepted)
                  .build();
            });
  }

  private Double extractThresholdScore(
      ValidationFunnelConfiguration.KarzaNameSimilarityConfig config) {
    if (Objects.isNull(config) || Objects.isNull(config.getScore())) {
      return null; // return null if config or score is missing
    }
    return config.getScore();
  }

  public Mono<ValidationFunnelConfiguration.KarzaNameSimilarityConfig> getKarzaNameSimilarityConfig(
      String productCode) {
    return validationFunnelConfigService
        .getValidationFunnelConfig(productCode)
        .flatMap(
            funnelConfig -> {
              return Mono.just(funnelConfig.getKarzaNameSimilarityConfig());
            });
  }

  @Override
  public Mono<Void> persist(
      Mono<KarzaNameSimilarityRequest> requestMono,
      Mono<KarzaNameSimilarityResponse> responseMono,
      Mono<KarzaNameSimilarityResult> resultMono,
      String clientId,
      String productCode) {

    log.info(
        "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][PERSIST] Initiating"
            + " persistence of Karza Name Similarity response to DB for clientId={} productCode={}",
        clientId,
        productCode);

    return Mono.zip(requestMono, resultMono, responseMono)
        .flatMap(
            tuple -> {
              KarzaNameSimilarityRequest request = tuple.getT1();
              KarzaNameSimilarityResult result = tuple.getT2();
              KarzaNameSimilarityResponse response = tuple.getT3();

              String requestJson = JsonUtils.serializeResponse(request);
              String responseJson = JsonUtils.serializeResponse(response);

              String statusCode = String.valueOf(result.getVendorResponse().getStatusCode());
              boolean isServiceSuccess =
                  KarzaInternalStatusEnum.CODE_101.getCode().equals(statusCode);

              // Derive step status if service succeeded
              ClientValidationFunnelStatus.StepStatus stepStatusResult = null;
              if (isServiceSuccess) {
                KarzaNameSimilarityResult.Status panStatus =
                    result.getEvaluationResult().getEvaluationSatus();
                stepStatusResult =
                    (panStatus == KarzaNameSimilarityResult.Status.APPROVED)
                        ? ClientValidationFunnelStatus.StepStatus.PASS
                        : ClientValidationFunnelStatus.StepStatus.REJECT;
              }

              // Build the step entity
              ValidationStepEntity stepEntity =
                  ValidationStepEntity.builder()
                      .clientId(clientId)
                      .stepName(NAME_SIMILARITY.name())
                      .vendor(KARZA.name())
                      .status(
                          isServiceSuccess
                              ? stepStatusResult.name()
                              : ClientValidationFunnelStatus.StepStatus.REJECT.name())
                      .serviceStatus(
                          isServiceSuccess
                              ? ClientValidationFunnelStatus.ServiceStatus.SUCCESS.name()
                              : ClientValidationFunnelStatus.ServiceStatus.FAILURE.name())
                      .request(requestJson)
                      .response(responseJson)
                      .build();

              ClientValidationFunnelStatus.FinalStatus finalStatus;

              if (!isServiceSuccess) {
                // Service failure leads to manual intervention.
                finalStatus = ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW;
              } else if (stepStatusResult == ClientValidationFunnelStatus.StepStatus.REJECT) {
                // Service succeeded, but the step result was an outright rejection.
                finalStatus = ClientValidationFunnelStatus.FinalStatus.REJECT;
              } else {
                finalStatus = null;
              }

              Mono<Void> funnelUpdate;

              if (finalStatus != null) {
                // If a final decision (REJECT or MANUAL_REVIEW) has been reached, upsert the
                // status.
                funnelUpdate =
                    clientValidationFunnelStatusRepository
                        .upsertFunnelStatus(clientId, productCode, finalStatus.name())
                        .then(markRemainingStepsSkipped(clientId, productCode))
                        .doOnError(
                            e ->
                                log.error(
                                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][PERSIST][FUNNEL_STATUS][UPDATE][ERROR]"
                                        + " Failed to upsert REJECT status: {} for clientId: {}",
                                    e.getMessage(),
                                    clientId));
              } else {
                // If no final decision was reached (service success and not REJECT),
                // this Mono returns an empty signal to continue the flow.
                funnelUpdate = Mono.empty();
              }

              // Chain funnel + step updates
              return funnelUpdate.then(
                  clientValidationFunnelStepsRepository
                      .upsertStep(
                          stepEntity.getClientId(),
                          productCode,
                          stepEntity.getStepName(),
                          stepEntity.getVendor(),
                          stepEntity.getStatus(),
                          stepEntity.getServiceStatus(),
                          stepEntity.getRequest(),
                          stepEntity.getResponse())
                      .doOnSuccess(
                          v ->
                              log.info(
                                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][PERSIST][STEP_STATUS][PERSIST]"
                                      + " clientId={} vendor={} stepStatus={} serviceStatus={} →"
                                      + " Successfully persisted",
                                  stepEntity.getClientId(),
                                  stepEntity.getVendor(),
                                  stepEntity.getStatus(),
                                  stepEntity.getServiceStatus()))
                      .doOnError(
                          e ->
                              log.error(
                                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][PERSIST][STEP_STATUS][PERSIST][ERROR]"
                                      + " clientId={} vendor={} Failed to persist step entity: {}",
                                  stepEntity.getClientId(),
                                  stepEntity.getVendor(),
                                  e.getMessage(),
                                  e))
                      .then()
                      .doFinally(
                          signal ->
                              log.info(
                                  "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][PERSIST]"
                                      + " Completed persistence for clientId={}",
                                  clientId)));
            })
        .doOnError(
            e ->
                log.error(
                    "[VALIDATION_SERVICE][KARZA_NAME_SIMILARITY_VALIDATION_SERVICE][PERSIST][ERROR]"
                        + " clientId={} productCode={} Unexpected error: {}",
                    clientId,
                    productCode,
                    e.getMessage()));
  }

  public Mono<Void> markRemainingStepsSkipped(String clientId, String productCode) {
    List<ClientValidationFunnelStatus.ValidationStep> markRemainingStepsAsSkipped =
        List.of(
            new ClientValidationFunnelStatus.ValidationStep(
                ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL,
                ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
                null,
                ClientValidationFunnelStatus.StepStatus.SKIPPED,
                ClientValidationFunnelStatus.ServiceStatus.SKIPPED));

    return validationFunnelServiceUtil.markRemainingStepsSkipped(
        clientId, productCode, markRemainingStepsAsSkipped);
  }

  private void publishEventKafkaAsync(Runnable eventTask) {
    Mono.fromRunnable(eventTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error while publishing the event", error));
  }
}
