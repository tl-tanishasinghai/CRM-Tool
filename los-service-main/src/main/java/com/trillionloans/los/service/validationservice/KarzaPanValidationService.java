package com.trillionloans.los.service.validationservice;

import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.StepName.PAN_VALIDATION;
import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.Vendor.KARZA;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.KarzaApi;
import com.trillionloans.los.config.ValidationFunnelConfiguration;
import com.trillionloans.los.config.ValidationFunnelProperties;
import com.trillionloans.los.exception.PanValidationExceptions.KarzaPanAuthenticateExceptions.KarzaPanAuthenticateException;
import com.trillionloans.los.mapper.KarzaInternalStatusEnum;
import com.trillionloans.los.model.ValidationStepEntity;
import com.trillionloans.los.model.request.ClientValidationFunnelStatus;
import com.trillionloans.los.model.request.KarzaPanAuthenticateRequest;
import com.trillionloans.los.model.request.KarzaPanAuthenticateResponse;
import com.trillionloans.los.model.request.KarzaPanAuthenticateResult;
import com.trillionloans.los.service.db.ClientValidationFunnelStatusRepository;
import com.trillionloans.los.service.db.ClientValidationFunnelStepsRepository;
import com.trillionloans.los.util.JsonUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
public class KarzaPanValidationService
    implements ValidationService<
        KarzaPanAuthenticateRequest, KarzaPanAuthenticateResult, KarzaPanAuthenticateResponse> {
  private final KarzaApi karzaApi;
  private static final String VENDOR_NAME = "KARZA";
  private final ValidationFunnelProperties properties;
  private final ValidationFunnelServiceUtil validationFunnelServiceUtil;
  private final ValidationFunnelConfigService validationFunnelConfigService;
  private final ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository;
  private final ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public KarzaPanValidationService(
      KarzaApi karzaApi,
      ValidationFunnelProperties properties,
      ValidationFunnelConfigService validationFunnelConfigService,
      ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository,
      ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository,
      ValidationFunnelServiceUtil validationFunnelServiceUtil) {
    this.karzaApi = karzaApi;
    this.properties = properties;
    this.validationFunnelConfigService = validationFunnelConfigService;
    this.clientValidationFunnelStepsRepository = clientValidationFunnelStepsRepository;
    this.clientValidationFunnelStatusRepository = clientValidationFunnelStatusRepository;
    this.validationFunnelServiceUtil = validationFunnelServiceUtil;
  }

  @Override
  public String getVendorName() {
    return VENDOR_NAME;
  }

  @Override
  public Mono<KarzaPanAuthenticateResult> init(
      Mono<KarzaPanAuthenticateRequest> request,
      String productCode,
      String clientId,
      String loanApplicationId) {
    log.info(
        "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][INIT] Starting Karza pan validation."
            + " clientId={}, productCode={}",
        clientId,
        productCode);

    return request
        .flatMap(
            karzaPanAuthenticateRequest -> {
              boolean skipCache = properties.isMasterFlag() && !properties.getCache().isEnabled();

              if (skipCache) {
                return callVendor(
                        karzaPanAuthenticateRequest, clientId, productCode, loanApplicationId)
                    .map(Tuple2::getT1);
              }

              return readThroughCache(Mono.just(karzaPanAuthenticateRequest), productCode, clientId)
                  .switchIfEmpty(
                      callVendor(
                              karzaPanAuthenticateRequest, clientId, productCode, loanApplicationId)
                          .flatMap(
                              tuple -> {
                                KarzaPanAuthenticateResult result = tuple.getT1();
                                KarzaPanAuthenticateResponse response = tuple.getT2();

                                // cache write
                                return writeThroughCache(
                                        Mono.just(karzaPanAuthenticateRequest),
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
                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][ERROR] PAN validation failed."
                      + " clientId={}, productCode={}, error={}",
                  clientId,
                  productCode,
                  e.getMessage());

              return validationFunnelServiceUtil
                  .persistFailure(
                      e, clientId, productCode, PAN_VALIDATION, KARZA, "KARZA_VALIDATION_SERVICE")
                  .then(
                      Mono.error(
                          new KarzaPanAuthenticateException(
                              "Error verifying PAN",
                              e.getMessage(),
                              HttpStatus.INTERNAL_SERVER_ERROR)));
            });
  }

  Mono<Tuple2<KarzaPanAuthenticateResult, KarzaPanAuthenticateResponse>> callVendor(
      KarzaPanAuthenticateRequest karzaReq,
      String clientId,
      String productCode,
      String loanApplicationId) {

    return karzaApi
        .authenticatePan(karzaReq, clientId, loanApplicationId)
        .flatMap(
            karzaResp ->
                evaluateResult(Mono.just(karzaResp), productCode)
                    .flatMap(
                        evalResult ->
                            persist(
                                    Mono.just(karzaReq),
                                    Mono.just(karzaResp),
                                    Mono.just(evalResult),
                                    clientId,
                                    productCode)
                                .thenReturn(Tuples.of(evalResult, karzaResp))));
  }

  @Override
  public Mono<KarzaPanAuthenticateResult> readThroughCache(
      Mono<KarzaPanAuthenticateRequest> request, String productCode, String clientId) {
    log.info(
        "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE] Attempting to"
            + " fetch validation result from Redis cache");

    String cacheKey = validationFunnelServiceUtil.buildCacheKey(productCode, clientId);

    return validationFunnelServiceUtil
        .getObjectFromCache(cacheKey, ClientValidationFunnelStatus.class)
        .flatMap(
            clientValidationFunnelStatus -> {
              if (Objects.isNull(clientValidationFunnelStatus)) {
                return Mono.empty();
              }

              log.info(
                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE] Cache hit.");

              try {
                // Get the KARZA PAN_VALIDATION step
                Optional<ClientValidationFunnelStatus.ValidationStep> karzaPanStepOpt =
                    clientValidationFunnelStatus.getSteps().stream()
                        .filter(
                            step ->
                                step.getStepName()
                                        == ClientValidationFunnelStatus.StepName.PAN_VALIDATION
                                    && step.getVendor()
                                        == ClientValidationFunnelStatus.Vendor.KARZA)
                        .findFirst();

                if (karzaPanStepOpt.isEmpty()) {
                  log.info(
                      "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE] Karza Pan"
                          + " Validation step not found for clientId={}",
                      clientValidationFunnelStatus.getClientId());
                  return Mono.empty();
                }

                ClientValidationFunnelStatus.ValidationStep karzaPanStep = karzaPanStepOpt.get();
                String karzaPanResponse = karzaPanStep.getResponse();

                KarzaPanAuthenticateResponse karzaPanAuthenticateResponse =
                    OBJECT_MAPPER.readValue(karzaPanResponse, KarzaPanAuthenticateResponse.class);

                return this.evaluateResult(Mono.just(karzaPanAuthenticateResponse), productCode)
                    .doOnNext(
                        result ->
                            log.info(
                                "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                                    + " Successfully evaluated cached response."))
                    .onErrorResume(
                        e -> {
                          log.warn(
                              "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                                  + " Failed to evaluate cached response. Error={}",
                              e.getMessage());
                          return Mono.empty();
                        });

              } catch (Exception e) {
                log.warn(
                    "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE] Failed to"
                        + " parse cached response. Error={}",
                    e.getMessage());
                return Mono.empty();
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE] Cache"
                          + " miss.");
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][READ_THROUGH_CACHE][ERROR] Redis"
                      + " operation failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            });
  }

  @Override
  public Mono<?> writeThroughCache(
      Mono<KarzaPanAuthenticateRequest> requestMono,
      Mono<KarzaPanAuthenticateResponse> responseMono,
      Mono<KarzaPanAuthenticateResult> resultMono,
      String clientID,
      String productCode) {

    log.info(
        "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][WRITE_THROUGH_CACHE] Attempting to cache {}"
            + " response async",
        getVendorName());

    String cacheKey = validationFunnelServiceUtil.buildCacheKey(productCode, clientID);

    // Combine all required Monos first
    return Mono.zip(requestMono, responseMono, resultMono)
        .flatMap(
            tuple -> {
              KarzaPanAuthenticateRequest request = tuple.getT1();
              KarzaPanAuthenticateResponse response = tuple.getT2();
              KarzaPanAuthenticateResult result = tuple.getT3();

              return validationFunnelServiceUtil
                  .getObjectFromCache(cacheKey, ClientValidationFunnelStatus.class)
                  .flatMap(
                      clientValidationFunnelStatus -> {
                        ClientValidationFunnelStatus funnelStatus;
                        // in the redis.
                        String statusCode = String.valueOf(response.getStatusCode());

                        if (KarzaInternalStatusEnum.CODE_101.getCode().equals(statusCode)) {
                          funnelStatus =
                              createWriteThroughCacheObject(
                                  clientValidationFunnelStatus, response, result, clientID);
                        } else {
                          log.info(
                              "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][WRITE_THROUGH_CACHE]"
                                  + " Cache failure response since responseCode is NOT SUCCESS.");
                          funnelStatus =
                              createFailureWriteThroughCacheObejct(
                                  clientValidationFunnelStatus, response, result, clientID);
                        }

                        return validationFunnelServiceUtil
                            .cacheObject(cacheKey, funnelStatus)
                            .doOnError(
                                e ->
                                    log.error(
                                        "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][WRITE_THROUGH_CACHE]"
                                            + " Failed to cache {} response. Error={}",
                                        getVendorName(),
                                        e.getMessage()))
                            .thenReturn(funnelStatus);
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            // Handle cache miss
                            ClientValidationFunnelStatus funnelStatus =
                                createWriteThroughCacheObject(
                                    new ClientValidationFunnelStatus(), response, result, clientID);
                            return validationFunnelServiceUtil
                                .cacheObject(cacheKey, funnelStatus)
                                .doOnError(
                                    e ->
                                        log.error(
                                            "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][WRITE_THROUGH_CACHE]"
                                                + " Failed to cache {} response. Error={}",
                                            getVendorName(),
                                            e.getMessage()))
                                .thenReturn(funnelStatus);
                          }));
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][WRITE_THROUGH_CACHE][ERROR] Redis"
                      + " operation failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            });
  }

  private ClientValidationFunnelStatus createWriteThroughCacheObject(
      ClientValidationFunnelStatus funnelStatus,
      KarzaPanAuthenticateResponse response,
      KarzaPanAuthenticateResult result,
      String clientId) {

    if (Objects.isNull(funnelStatus) || Objects.isNull(funnelStatus.getFinalStatus())) {
      assert funnelStatus != null;
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.PENDING);
    }

    // Determine step status (PASS / REJECT)
    ClientValidationFunnelStatus.StepStatus stepStatusResult =
        KarzaInternalStatusEnum.CODE_101.getCode().equals(String.valueOf(response.getStatusCode()))
            ? ClientValidationFunnelStatus.StepStatus.PASS
            : ClientValidationFunnelStatus.StepStatus.REJECT;

    if (stepStatusResult == ClientValidationFunnelStatus.StepStatus.REJECT) {
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.REJECT);
    }

    // Create or update step
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            ClientValidationFunnelStatus.StepName.PAN_VALIDATION,
            ClientValidationFunnelStatus.Vendor.KARZA,
            response.toString(), // store full response JSON
            stepStatusResult,
            ClientValidationFunnelStatus.ServiceStatus.SUCCESS);

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

  private ClientValidationFunnelStatus createFailureWriteThroughCacheObejct(
      ClientValidationFunnelStatus funnelStatus,
      KarzaPanAuthenticateResponse response,
      KarzaPanAuthenticateResult result,
      String clientId) {
    funnelStatus.setClientId(clientId);
    funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW);

    // Create or update step
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            ClientValidationFunnelStatus.StepName.PAN_VALIDATION,
            ClientValidationFunnelStatus.Vendor.KARZA,
            response.toString(), // store full response JSON
            ClientValidationFunnelStatus.StepStatus.REJECT,
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
  public Mono<KarzaPanAuthenticateResult> evaluateResult(
      Mono<KarzaPanAuthenticateResponse> karzaPanAuthenticateResponseMono, String productCode) {
    log.info(
        "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][EVALUATE_KARZA_PAN_RESPONSE] Evaluating"
            + " Karza PAN response");

    return karzaPanAuthenticateResponseMono
        .flatMap(
            response -> {
              if (response == null) {
                log.warn(
                    "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][EVALUATE_KARZA_PAN_RESPONSE]"
                        + " Empty response received. Skipping evaluation.");
                return Mono.just(new KarzaPanAuthenticateResult());
              }

              String statusCode = String.valueOf(response.getStatusCode());
              KarzaInternalStatusEnum internalStatus = KarzaInternalStatusEnum.fromCode(statusCode);

              if (KarzaInternalStatusEnum.CODE_101.getCode().equals(statusCode)) {
                log.info(
                    "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][EVALUATE_KARZA_PAN_RESPONSE]"
                        + " Received SUCCESS Karza PAN Auth response. Proceeding with evaluation.");

                return buildResult(
                    response, internalStatus, KarzaPanAuthenticateResult.Status.APPROVED, null);
              } else {
                log.info(
                    "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][EVALUATE_KARZA_PAN_RESPONSE]"
                        + " Received UN-SUCCESSFUL Karza PAN Auth response.");

                return buildResult(
                    response,
                    internalStatus,
                    KarzaPanAuthenticateResult.Status.REJECTED,
                    "PAN validation rejected");
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][EVALUATE_KARZA_PAN_RESPONSE]"
                          + " Received empty Mono for Karza PAN Authenticate response.");
                  return Mono.just(new KarzaPanAuthenticateResult());
                }))
        .onErrorResume(
            e -> {
              log.error(
                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][EVALUATE_KARZA_PAN_RESPONSE][ERROR]"
                      + " Evaluation failed. Error={}",
                  e.getMessage(),
                  e);
              return Mono.just(new KarzaPanAuthenticateResult());
            });
  }

  private Mono<KarzaPanAuthenticateResult> buildResult(
      KarzaPanAuthenticateResponse response,
      KarzaInternalStatusEnum internalStatus,
      KarzaPanAuthenticateResult.Status status,
      String rejectionReason) {

    return Mono.fromSupplier(
            () -> {
              KarzaPanAuthenticateResult.EvaluationResult evaluationResult =
                  KarzaPanAuthenticateResult.EvaluationResult.builder()
                      .status(status)
                      .rejectionReasons(rejectionReason)
                      .build();

              return KarzaPanAuthenticateResult.builder()
                  .vendorResponse(response)
                  .internalStatusDesc(internalStatus)
                  .evaluationResult(evaluationResult)
                  .build();
            })
        .doOnNext(
            result ->
                log.info(
                    "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][EVALUATE_KARZA_PAN_RESPONSE]"
                        + " Evaluated Karza PAN Result: {}",
                    result.toString()));
  }

  @Override
  public Mono<Void> persist(
      Mono<KarzaPanAuthenticateRequest> requestMono,
      Mono<KarzaPanAuthenticateResponse> responseMono,
      Mono<KarzaPanAuthenticateResult> resultMono,
      String clientId,
      String productCode) {

    log.info(
        "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][PERSIST] Initiating persistence of Karza"
            + " Pan Validation response to DB for clientId={} productCode={}",
        clientId,
        productCode);

    return Mono.zip(requestMono, resultMono, responseMono)
        .flatMap(
            tuple -> {
              KarzaPanAuthenticateRequest request = tuple.getT1();
              KarzaPanAuthenticateResult result = tuple.getT2();
              KarzaPanAuthenticateResponse response = tuple.getT3();

              String requestJson = JsonUtils.serializeResponse(request);
              String responseJson = JsonUtils.serializeResponse(response);

              String statusCode = String.valueOf(result.getVendorResponse().getStatusCode());
              boolean isServiceSuccess =
                  KarzaInternalStatusEnum.CODE_101.getCode().equals(statusCode);

              // Derive step status if service succeeded
              ClientValidationFunnelStatus.StepStatus stepStatusResult = null;
              if (isServiceSuccess) {
                KarzaPanAuthenticateResult.Status status = result.getEvaluationResult().getStatus();
                stepStatusResult =
                    (status == KarzaPanAuthenticateResult.Status.APPROVED)
                        ? ClientValidationFunnelStatus.StepStatus.PASS
                        : ClientValidationFunnelStatus.StepStatus.REJECT;
              }

              // Build the step entity
              ValidationStepEntity stepEntity =
                  ValidationStepEntity.builder()
                      .clientId(clientId)
                      .stepName(PAN_VALIDATION.name())
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
                                    "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][PERSIST][FUNNEL_STATUS][UPDATE][ERROR]"
                                        + " Failed to upsert REJECT status: {} for clientId: {}",
                                    e.getMessage(),
                                    clientId));
              } else {
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
                                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][PERSIST][STEP_STATUS][PERSIST]"
                                      + " clientId={} vendor={} stepStatus={} serviceStatus={} →"
                                      + " Successfully persisted",
                                  stepEntity.getClientId(),
                                  stepEntity.getVendor(),
                                  stepEntity.getStatus(),
                                  stepEntity.getServiceStatus()))
                      .doOnError(
                          e ->
                              log.error(
                                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][PERSIST][STEP_STATUS][PERSIST][ERROR]"
                                      + " clientId={} vendor={} Failed to persist step entity: {}",
                                  stepEntity.getClientId(),
                                  stepEntity.getVendor(),
                                  e.getMessage(),
                                  e))
                      .then()
                      .doFinally(
                          signal ->
                              log.info(
                                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][PERSIST]"
                                      + " Completed persistence for clientId={}",
                                  clientId)));
            })
        .doOnError(
            e ->
                log.error(
                    "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][PERSIST][ERROR] clientId={}"
                        + " productCode={} Unexpected error: {}",
                    clientId,
                    productCode,
                    e.getMessage()));
  }

  public Mono<Void> markRemainingStepsSkipped(String clientId, String productCode) {
    List<ClientValidationFunnelStatus.ValidationStep> markRemainingStepsAsSkipped =
        List.of(
            new ClientValidationFunnelStatus.ValidationStep(
                ClientValidationFunnelStatus.StepName.NAME_SIMILARITY,
                KARZA,
                null,
                ClientValidationFunnelStatus.StepStatus.SKIPPED,
                ClientValidationFunnelStatus.ServiceStatus.SKIPPED),
            new ClientValidationFunnelStatus.ValidationStep(
                ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL,
                ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
                null,
                ClientValidationFunnelStatus.StepStatus.SKIPPED,
                ClientValidationFunnelStatus.ServiceStatus.SKIPPED));

    return validationFunnelServiceUtil.markRemainingStepsSkipped(
        clientId, productCode, markRemainingStepsAsSkipped);
  }

  public Mono<ValidationFunnelConfiguration.KarzaPanValidationConfig> getKarzaPanValidationConfig(
      String productCode) {
    return validationFunnelConfigService
        .getValidationFunnelConfig(productCode)
        .flatMap(
            funnelConfig -> {
              return Mono.just(funnelConfig.getKarzaPanValidationConfig());
            });
  }
}
