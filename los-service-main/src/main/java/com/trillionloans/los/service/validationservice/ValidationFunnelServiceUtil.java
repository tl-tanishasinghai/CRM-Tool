package com.trillionloans.los.service.validationservice;

import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.StepName.*;
import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.Vendor.KARZA;
import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.Vendor.NSDL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.config.ValidationFunnelProperties;
import com.trillionloans.los.model.ClientValidationFunnelStatusEntity;
import com.trillionloans.los.model.ValidationStepEntity;
import com.trillionloans.los.model.request.ClientValidationFunnelStatus;
import com.trillionloans.los.model.request.ClientValidationServiceKYCCallbackResponse;
import com.trillionloans.los.model.request.KarzaNameSimilarityResponse;
import com.trillionloans.los.service.db.ClientValidationFunnelStatusRepository;
import com.trillionloans.los.service.db.ClientValidationFunnelStepsRepository;
import com.trillionloans.los.service.db.RedisCacheService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ValidationFunnelServiceUtil {

  private final ValidationFunnelProperties properties;
  private final RedisCacheService redisCacheService;
  private final ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository;
  private final ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository;

  public final String VALIDATION_FUNNEL_REDIS_KEY_PREFIX = "VALIDATION_FUNNEL";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public ValidationFunnelServiceUtil(
      ValidationFunnelProperties properties,
      RedisCacheService redisCacheService,
      ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository,
      ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository) {
    this.properties = properties;
    this.redisCacheService = redisCacheService;
    this.clientValidationFunnelStepsRepository = clientValidationFunnelStepsRepository;
    this.clientValidationFunnelStatusRepository = clientValidationFunnelStatusRepository;
  }

  public String buildCacheKey(String productCode, String clientId) {
    return VALIDATION_FUNNEL_REDIS_KEY_PREFIX + ":" + productCode + ":" + clientId;
  }

  public <T> Mono<T> getObjectFromCache(String key, Class<T> clazz) {
    if (!properties.getCache().isEnabled()) {
      log.info("[VALIDATION_SERVICE] Caching is OFF.");
      return Mono.empty(); // skip cache lookup when disabled
    }

    return redisCacheService.getObjectSilently(
        key, clazz, properties.getCache().isEncryptionEnabled());
  }

  public <T> Mono<Void> cacheObject(String key, T object) {
    if (!properties.getCache().isEnabled()) {
      log.info("[VALIDATION_SERVICE] Caching is OFF.");
      return Mono.empty(); // do not write to cache when disabled
    }

    return redisCacheService.cacheObjectSilently(
        key, object, properties.getCache().getTtl(), properties.getCache().isEncryptionEnabled());
  }

  public Mono<Void> persistFailure(
      Throwable exception,
      String clientId,
      String productCode,
      ClientValidationFunnelStatus.StepName stepName,
      ClientValidationFunnelStatus.Vendor vendor,
      String logHeader) {

    log.error(
        "{}[PERSIST_ON_FAILURE] Persisting failure for clientId={} productCode={} step={}"
            + " vendor={}",
        logHeader,
        clientId,
        productCode,
        stepName,
        vendor,
        exception);

    String errorResponse =
        exception.getMessage() != null ? exception.getMessage() : "Unexpected error";

    ValidationStepEntity failureStep =
        ValidationStepEntity.builder()
            .clientId(clientId)
            .productCode(productCode)
            .stepName(stepName.name())
            .vendor(vendor.name())
            .status(ClientValidationFunnelStatus.StepStatus.REJECT.name())
            .serviceStatus(ClientValidationFunnelStatus.ServiceStatus.FAILURE.name())
            .request(null)
            .response(errorResponse)
            .build();

    Mono<Void> updateFunnelFinalStatus =
        clientValidationFunnelStatusRepository
            .upsertFunnelStatus(
                clientId,
                productCode,
                ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW.name())
            .doOnSuccess(
                e ->
                    log.info(
                        "{}[PERSIST_ON_FAILURE][FUNNEL_STATUS] Marked clientId={} productCode={} as"
                            + " REJECT",
                        logHeader,
                        clientId,
                        productCode))
            .doOnError(
                e ->
                    log.error(
                        "{}[PERSIST_ON_FAILURE][FUNNEL_STATUS][ERROR] Failed to update status for"
                            + " clientId={} error={}",
                        logHeader,
                        clientId,
                        e.getMessage()))
            .then();

    Mono<Void> logStepFailure =
        clientValidationFunnelStepsRepository
            .upsertStep(
                failureStep.getClientId(),
                failureStep.getProductCode(),
                failureStep.getStepName(),
                failureStep.getVendor(),
                failureStep.getStatus(),
                failureStep.getServiceStatus(),
                failureStep.getRequest(),
                failureStep.getResponse())
            .doOnSuccess(
                v ->
                    log.info(
                        "{}[PERSIST_ON_FAILURE][STEP_STATUS] Persisted failed step for clientId={}"
                            + " step={} vendor={}",
                        logHeader,
                        clientId,
                        stepName,
                        vendor))
            .doOnError(
                e ->
                    log.error(
                        "{}[PERSIST_ON_FAILURE][STEP_STATUS][ERROR] Unable to persist failed step"
                            + " for clientId={} step={} error={}",
                        logHeader,
                        clientId,
                        stepName,
                        e.getMessage()))
            .then();

    return updateFunnelFinalStatus
        .then(logStepFailure)
        .then(
            markRemainingStepsSkipped(clientId, productCode, stepName.name(), vendor.name())
                .then(writeThroughCacheOnFailure(clientId, productCode, stepName, vendor)))
        .then();
  }

  public Mono<Void> markRemainingStepsSkipped(
      String clientId, String productCode, String stepName, String vendor) {

    ClientValidationFunnelStatus.StepName currentStep =
        ClientValidationFunnelStatus.StepName.valueOf(stepName);

    ClientValidationFunnelStatus.Vendor currentVendor =
        ClientValidationFunnelStatus.Vendor.valueOf(vendor);

    List<ClientValidationFunnelStatus.ValidationStep> skippedSteps = new ArrayList<>();

    if (currentStep == PAN_VALIDATION && currentVendor == NSDL) {

      skippedSteps.add(
          new ClientValidationFunnelStatus.ValidationStep(
              PAN_VALIDATION,
              KARZA,
              null,
              ClientValidationFunnelStatus.StepStatus.SKIPPED,
              ClientValidationFunnelStatus.ServiceStatus.SKIPPED));

      skippedSteps.add(
          new ClientValidationFunnelStatus.ValidationStep(
              NAME_SIMILARITY,
              KARZA,
              null,
              ClientValidationFunnelStatus.StepStatus.SKIPPED,
              ClientValidationFunnelStatus.ServiceStatus.SKIPPED));

      skippedSteps.add(
          new ClientValidationFunnelStatus.ValidationStep(
              IN_HOUSE_DOB_WATERFALL,
              ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
              null,
              ClientValidationFunnelStatus.StepStatus.SKIPPED,
              ClientValidationFunnelStatus.ServiceStatus.SKIPPED));

    } else if (currentStep == PAN_VALIDATION && currentVendor == KARZA) {

      skippedSteps.add(
          new ClientValidationFunnelStatus.ValidationStep(
              NAME_SIMILARITY,
              KARZA,
              null,
              ClientValidationFunnelStatus.StepStatus.SKIPPED,
              ClientValidationFunnelStatus.ServiceStatus.SKIPPED));

      skippedSteps.add(
          new ClientValidationFunnelStatus.ValidationStep(
              IN_HOUSE_DOB_WATERFALL,
              ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
              null,
              ClientValidationFunnelStatus.StepStatus.SKIPPED,
              ClientValidationFunnelStatus.ServiceStatus.SKIPPED));

    } else if (currentStep == NAME_SIMILARITY && currentVendor == KARZA) {

      skippedSteps.add(
          new ClientValidationFunnelStatus.ValidationStep(
              IN_HOUSE_DOB_WATERFALL,
              ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
              null,
              ClientValidationFunnelStatus.StepStatus.SKIPPED,
              ClientValidationFunnelStatus.ServiceStatus.SKIPPED));
    }

    if (skippedSteps.isEmpty()) {
      log.warn(
          "[VALIDATION_FALLBACK][SKIP_STEPS] No steps to skip for clientId={} productCode={}"
              + " step={} vendor={}",
          clientId,
          productCode,
          stepName,
          vendor);
      return Mono.empty();
    }

    log.info(
        "[VALIDATION_FALLBACK][SKIP_STEPS] Marking {} steps skipped for clientId={} productCode={}"
            + " after step={} vendor={}",
        skippedSteps.size(),
        clientId,
        productCode,
        stepName,
        vendor);

    return markRemainingStepsSkipped(clientId, productCode, skippedSteps);
  }

  public Mono<ClientValidationFunnelStatus> writeThroughCacheOnFailure(
      String clientId,
      String productCode,
      ClientValidationFunnelStatus.StepName stepName,
      ClientValidationFunnelStatus.Vendor vendor) {

    String cacheKey = buildCacheKey(productCode, clientId);

    return redisCacheService
        .getKey(cacheKey)
        .flatMap(
            cachedResponse -> {
              ClientValidationFunnelStatus funnelStatus;

              if (cachedResponse == null || cachedResponse.isEmpty()) {
                funnelStatus = new ClientValidationFunnelStatus();
              } else {
                try {
                  funnelStatus =
                      OBJECT_MAPPER.readValue(cachedResponse, ClientValidationFunnelStatus.class);
                } catch (Exception e) {
                  log.warn(
                      "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][WRITE_THROUGH_CACHE]"
                          + " Failed to parse cached response: {}",
                      e.getMessage());
                  funnelStatus = new ClientValidationFunnelStatus();
                }
              }

              funnelStatus =
                  createFailureWriteThroughCacheObejct(funnelStatus, clientId, stepName, vendor);

              return cacheObject(cacheKey, funnelStatus).thenReturn(funnelStatus);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  ClientValidationFunnelStatus funnelStatus =
                      createFailureWriteThroughCacheObejct(
                          new ClientValidationFunnelStatus(), clientId, stepName, vendor);

                  return cacheObject(cacheKey, funnelStatus).thenReturn(funnelStatus);
                }))
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][KARZA_VALIDATION_SERVICE][WRITE_THROUGH_CACHE][ERROR] Redis"
                      + " operation failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            });
  }

  private ClientValidationFunnelStatus createFailureWriteThroughCacheObejct(
      ClientValidationFunnelStatus funnelStatus,
      String clientId,
      ClientValidationFunnelStatus.StepName stepName,
      ClientValidationFunnelStatus.Vendor vendor) {
    funnelStatus.setClientId(clientId);
    funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW);

    // Create or update step
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            stepName,
            vendor,
            null, // store full response JSON
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

  public Mono<ClientValidationServiceKYCCallbackResponse> readValidationServiceFinalStatus(
      String productCode, String clientId) {
    // Fetch the main status entity for that clientId and productCode
    Mono<ClientValidationFunnelStatusEntity> statusEntityMono =
        clientValidationFunnelStatusRepository.findByClientIdAndProductCode(clientId, productCode);

    // Fetch all step entities for that clientId and productCode
    Mono<List<ValidationStepEntity>> stepsListMono =
        clientValidationFunnelStepsRepository
            .findByClientIdAndProductCode(clientId, productCode)
            .collectList();

    return Mono.zip(statusEntityMono, stepsListMono)
        .flatMap(
            tuple -> {
              ClientValidationFunnelStatusEntity statusEntity = tuple.getT1();
              List<ValidationStepEntity> stepEntities = tuple.getT2();

              // Extract KARZA NAME_SIMILARITY response safely
              KarzaNameSimilarityResponse karzaNameSimilarityResponse =
                  stepEntities.stream()
                      .filter(
                          step ->
                              KARZA.name().equalsIgnoreCase(step.getVendor())
                                  && NAME_SIMILARITY.name().equalsIgnoreCase(step.getStepName()))
                      .map(ValidationStepEntity::getResponse)
                      .filter(resp -> resp != null && !resp.isBlank())
                      .findFirst()
                      .map(
                          resp -> {
                            try {
                              return OBJECT_MAPPER.readValue(
                                  resp, KarzaNameSimilarityResponse.class);
                            } catch (Exception e) {
                              log.error(
                                  "[VALIDATION_SERVICE][KYC_CALLBACK] Failed to parse KARZA"
                                      + " NAME_SIMILARITY response",
                                  e);
                              return null;
                            }
                          })
                      .orElse(null);

              String nameFuzzyMatchPercentage =
                  Optional.ofNullable(karzaNameSimilarityResponse)
                      .map(KarzaNameSimilarityResponse::getResult)
                      .map(KarzaNameSimilarityResponse.Result::getScore)
                      .map(s -> Math.round(s * 100.0) / 100.0)
                      .map(String::valueOf)
                      .orElse(StringUtils.EMPTY);

              ClientValidationFunnelStatus statusDto =
                  mapEntitiesToFunnelStatus(statusEntity, stepEntities);

              log.info(
                  "[VALIDATION_SERVICE][KYC_CALLBACK][READ_THROUGH_DB] Successfully"
                      + " loaded status and {} steps from DB.",
                  stepEntities.size());

              return mapFunnelStatusToValidationServiceKYCCallbackResponse(
                      statusDto, nameFuzzyMatchPercentage)
                  .doOnSuccess(
                      res -> {
                        log.info("[VALIDATION_SERVICE] Validation funnel status: {}", statusDto);
                      });
            })
        // Handles case where statusEntityMono or stepsListMono returns nothing
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[VALIDATION_SERVICE][KYC_CALLBACK][READ_THROUGH_DB] No"
                          + " validation data found in DB for client: {}",
                      clientId);
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.error(
                  "[VALIDATION_SERVICE][KYC_CALLBACK][READ_THROUGH_DB][FATAL_ERROR]"
                      + " Operation failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            });
  }

  private Mono<ClientValidationServiceKYCCallbackResponse>
      mapFunnelStatusToValidationServiceKYCCallbackResponse(
          ClientValidationFunnelStatus clientValidationFunnelStatus,
          String nameFuzzyMatchPercentage) {
    log.debug("[VALIDATION_SERVICE] Mapping FunnelStatus to KYC Callback Response.");

    final ClientValidationFunnelStatus.StepStatus nsdlPanStatus =
        extractStepStatus(
            clientValidationFunnelStatus,
            ClientValidationFunnelStatus.StepName.PAN_VALIDATION,
            ClientValidationFunnelStatus.Vendor.NSDL);

    final ClientValidationFunnelStatus.StepStatus karzaPanStatus =
        extractStepStatus(
            clientValidationFunnelStatus,
            ClientValidationFunnelStatus.StepName.PAN_VALIDATION,
            ClientValidationFunnelStatus.Vendor.KARZA);

    final ClientValidationFunnelStatus.StepStatus karzaNameStatus =
        extractStepStatus(
            clientValidationFunnelStatus,
            ClientValidationFunnelStatus.StepName.NAME_SIMILARITY,
            ClientValidationFunnelStatus.Vendor.KARZA);

    final ClientValidationFunnelStatus.StepStatus dobWaterfallStatus =
        extractStepStatus(
            clientValidationFunnelStatus,
            ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL,
            ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL);

    // Build the response DTO
    ClientValidationServiceKYCCallbackResponse response =
        ClientValidationServiceKYCCallbackResponse.builder()
            .finalStatus(clientValidationFunnelStatus.getFinalStatus())
            .nsdlPanValidationStatus(nsdlPanStatus)
            .karzaPanValidationStatus(karzaPanStatus)
            .karzaNameSimilarityStatus(karzaNameStatus)
            .dobWaterFallStatus(dobWaterfallStatus)
            .nameFuzzyMatchPercentage(nameFuzzyMatchPercentage)
            .build();

    return Mono.just(response);
  }

  private ClientValidationFunnelStatus mapEntitiesToFunnelStatus(
      ClientValidationFunnelStatusEntity statusEntity, List<ValidationStepEntity> stepEntities) {

    ClientValidationFunnelStatus statusDto = new ClientValidationFunnelStatus();

    statusDto.setClientId(statusEntity.getClientId());
    statusDto.setFinalStatus(
        safeValueOf(
            ClientValidationFunnelStatus.FinalStatus.class,
            statusEntity.getFinalStatus(),
            ClientValidationFunnelStatus.FinalStatus.INIT));

    List<ClientValidationFunnelStatus.ValidationStep> stepDtos =
        stepEntities.stream()
            .map(
                entity -> {
                  ClientValidationFunnelStatus.ValidationStep step =
                      new ClientValidationFunnelStatus.ValidationStep();

                  step.setStepName(
                      safeValueOf(
                          ClientValidationFunnelStatus.StepName.class,
                          entity.getStepName(),
                          ClientValidationFunnelStatus.StepName
                              .NULL) // Use a common step as placeholder
                      );
                  step.setVendor(
                      safeValueOf(
                          ClientValidationFunnelStatus.Vendor.class,
                          entity.getVendor(),
                          ClientValidationFunnelStatus.Vendor
                              .NULL) // Use a common vendor as placeholder
                      );
                  step.setStatus(
                      safeValueOf(
                          ClientValidationFunnelStatus.StepStatus.class,
                          entity.getStatus(),
                          ClientValidationFunnelStatus.StepStatus.NULL));
                  step.setServiceStatus(
                      safeValueOf(
                          ClientValidationFunnelStatus.ServiceStatus.class,
                          entity.getServiceStatus(),
                          ClientValidationFunnelStatus.ServiceStatus.NULL));

                  step.setResponse(entity.getResponse());
                  return step;
                })
            .toList();
    statusDto.setSteps(stepDtos);

    return statusDto;
  }

  private ClientValidationFunnelStatus.StepStatus extractStepStatus(
      ClientValidationFunnelStatus funnelStatus,
      ClientValidationFunnelStatus.StepName name,
      ClientValidationFunnelStatus.Vendor vendor) {

    return funnelStatus.getSteps().stream()
        .filter(step -> step.getStepName() == name && step.getVendor() == vendor)
        .findFirst()
        .map(ClientValidationFunnelStatus.ValidationStep::getStatus)
        .orElseGet(ValidationFunnelServiceUtil::get);
  }

  private static ClientValidationFunnelStatus.StepStatus get() {
    return ClientValidationFunnelStatus.StepStatus.NULL;
  }

  private <T extends Enum<T>> T safeValueOf(Class<T> enumType, String value, T defaultValue) {

    if (value == null || value.trim().isEmpty()) {
      log.warn(
          "Missing value for enum type {}. Defaulting to {}.",
          enumType.getSimpleName(),
          defaultValue);
      return defaultValue;
    }

    try {
      return Enum.valueOf(enumType, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn(
          "Invalid enum value '{}' for type {}. Defaulting to {}",
          value,
          enumType.getSimpleName(),
          defaultValue);
      return defaultValue;
    }
  }

  public Mono<Void> markRemainingStepsSkipped(
      String clientId,
      String productCode,
      List<ClientValidationFunnelStatus.ValidationStep> definedSteps) {
    return Flux.fromIterable(definedSteps)
        .flatMap(
            step ->
                clientValidationFunnelStepsRepository
                    .upsertStep(
                        clientId,
                        productCode,
                        step.getStepName().name(),
                        step.getVendor().name(),
                        ClientValidationFunnelStatus.StepStatus.SKIPPED.name(),
                        ClientValidationFunnelStatus.ServiceStatus.SKIPPED.name(),
                        null,
                        null)
                    .doOnSuccess(
                        entity ->
                            log.info(
                                "[FUNNEL][STEP][SKIPPED] Upserted/Updated step={} vendor={} for"
                                    + " clientId={}",
                                step.getStepName(),
                                step.getVendor(),
                                clientId))
                    .doOnError(
                        e ->
                            log.error(
                                "[FUNNEL][STEP][SKIPPED][ERROR] Failed to upsert step={} vendor={}"
                                    + " for clientId={} err={}",
                                step.getStepName(),
                                step.getVendor(),
                                clientId,
                                e.getMessage())))
        .then();
  }
}
