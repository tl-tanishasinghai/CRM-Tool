package com.trillionloans.los.service.validationservice;

import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.StepName.PAN_VALIDATION;
import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.Vendor.KARZA;
import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.Vendor.NSDL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.NSDLV3API;
import com.trillionloans.los.config.ValidationFunnelConfiguration;
import com.trillionloans.los.config.ValidationFunnelProperties;
import com.trillionloans.los.exception.PanValidationExceptions.PanVerificationException;
import com.trillionloans.los.mapper.OpvResponseCode;
import com.trillionloans.los.mapper.PanStatus;
import com.trillionloans.los.mapper.SeedingStatus;
import com.trillionloans.los.model.NsdlPanEvaluationResult;
import com.trillionloans.los.model.NsdlPanVerificationResponse;
import com.trillionloans.los.model.NsdlPanVerificationResult;
import com.trillionloans.los.model.ValidationStepEntity;
import com.trillionloans.los.model.dto.NsdlRejectionType;
import com.trillionloans.los.model.dto.PanVerificationLog;
import com.trillionloans.los.model.request.ClientValidationFunnelStatus;
import com.trillionloans.los.model.request.NsdlPanVerificationRequest;
import com.trillionloans.los.service.db.ClientValidationFunnelStatusRepository;
import com.trillionloans.los.service.db.ClientValidationFunnelStepsRepository;
import com.trillionloans.los.util.JsonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Service responsible for PAN validation flow: - Fetch cached PAN validation result from Redis (to
 * avoid redundant OPV calls). - If cache miss → call OPV API. - Evaluate OPV response against
 * configured PAN validation rules. - Persist results asynchronously.
 */
@Slf4j
@Service
public class NsdlPanValidationService
    implements ValidationService<
        NsdlPanVerificationRequest, NsdlPanVerificationResult, NsdlPanVerificationResponse> {
  private final NSDLV3API nsdlV3API;
  private final M2PWrapperApi m2PWrapperApi;
  private final ValidationFunnelProperties properties;
  private final ValidationFunnelConfigService validationFunnelConfigService;
  private final ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository;
  private final ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository;
  private final ValidationFunnelServiceUtil validationFunnelServiceUtil;

  private static final String VENDOR_NAME = "NSDL";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired
  public NsdlPanValidationService(
      NSDLV3API nsdlV3API,
      M2PWrapperApi m2PWrapperApi,
      ValidationFunnelConfigService validationFunnelConfigService,
      ValidationFunnelProperties properties,
      ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository,
      ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository,
      ValidationFunnelServiceUtil validationFunnelServiceUtil) {
    this.nsdlV3API = nsdlV3API;
    this.m2PWrapperApi = m2PWrapperApi;
    this.validationFunnelConfigService = validationFunnelConfigService;
    this.properties = properties;
    this.clientValidationFunnelStepsRepository = clientValidationFunnelStepsRepository;
    this.clientValidationFunnelStatusRepository = clientValidationFunnelStatusRepository;
    this.validationFunnelServiceUtil = validationFunnelServiceUtil;
  }

  @Override
  public String getVendorName() {
    return VENDOR_NAME;
  }

  @Override
  public Mono<NsdlPanVerificationResult> init(
      Mono<NsdlPanVerificationRequest> request,
      String productCode,
      String clientId,
      String loanApplicationId) {
    log.info(
        "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][INIT] Starting NSDL PAN validation."
            + " clientId={}, productCode={}",
        clientId,
        productCode);

    return request
        .flatMap(
            panRequest -> {
              boolean skipCache = properties.isMasterFlag() && !properties.getCache().isEnabled();

              if (skipCache) {
                return callVendor(panRequest, clientId, productCode, loanApplicationId)
                    .map(Tuple2::getT1);
              }

              return readThroughCache(Mono.just(panRequest), productCode, clientId)
                  .switchIfEmpty(
                      callVendor(panRequest, clientId, productCode, loanApplicationId)
                          .flatMap(
                              tuple -> {
                                NsdlPanVerificationResult result = tuple.getT1();
                                NsdlPanVerificationResponse opvResponse = tuple.getT2();

                                // cache write
                                return writeThroughCache(
                                        Mono.just(panRequest),
                                        Mono.just(opvResponse),
                                        Mono.just(result),
                                        clientId,
                                        productCode)
                                    .thenReturn(result);
                              }));
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][ERROR] PAN validation failed."
                      + " clientId={}, productCode={}, error={}",
                  clientId,
                  productCode,
                  e.getMessage());

              return validationFunnelServiceUtil
                  .persistFailure(
                      e, clientId, productCode, PAN_VALIDATION, NSDL, "NSDL_VALIDATION_SERVICE")
                  .then(
                      Mono.error(
                          new PanVerificationException(
                              "Error verifying PAN",
                              e.getMessage(),
                              HttpStatus.INTERNAL_SERVER_ERROR)));
            });
  }

  Mono<Tuple2<NsdlPanVerificationResult, NsdlPanVerificationResponse>> callVendor(
      NsdlPanVerificationRequest panReq,
      String clientId,
      String productCode,
      String loanApplicationId) {
    return nsdlV3API
        .verify(List.of(panReq), clientId, loanApplicationId)
        .flatMap(
            opvResponse ->
                evaluateResult(Mono.just(opvResponse), productCode)
                    .flatMap(
                        result ->
                            persist(
                                    Mono.just(panReq),
                                    Mono.just(opvResponse),
                                    Mono.just(result),
                                    clientId,
                                    productCode)
                                .thenReturn(Tuples.of(result, opvResponse))));
  }

  @Override
  public Mono<NsdlPanVerificationResult> readThroughCache(
      Mono<NsdlPanVerificationRequest> panRequest, String productCode, String clientId) {

    log.info(
        "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][READ_THROUGH_CACHE] Attempting to fetch"
            + " validation result from Redis cache.");

    String cacheKey = validationFunnelServiceUtil.buildCacheKey(productCode, clientId);

    return validationFunnelServiceUtil
        .getObjectFromCache(cacheKey, ClientValidationFunnelStatus.class)
        .flatMap(
            clientValidationFunnelStatus -> {
              if (Objects.isNull(clientValidationFunnelStatus)) {
                return Mono.empty();
              }

              log.info(
                  "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][READ_THROUGH_CACHE] Cache hit.");

              try {
                Optional<ClientValidationFunnelStatus.ValidationStep> nsdlPanStepOpt =
                    clientValidationFunnelStatus.getSteps().stream()
                        .filter(
                            step ->
                                step.getStepName()
                                        == ClientValidationFunnelStatus.StepName.PAN_VALIDATION
                                    && step.getVendor() == NSDL)
                        .findFirst();

                if (nsdlPanStepOpt.isEmpty()) {
                  log.info(
                      "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][READ_THROUGH_CACHE] NSDL pan"
                          + " validation step not found for clientID= {}.",
                      clientId);
                  return Mono.empty();
                }

                ClientValidationFunnelStatus.ValidationStep nsdlPanStep = nsdlPanStepOpt.get();
                String nsdlPanResponse = nsdlPanStep.getResponse();

                NsdlPanVerificationResponse opvResponse =
                    OBJECT_MAPPER.readValue(nsdlPanResponse, NsdlPanVerificationResponse.class);
                return this.evaluateResult(Mono.just(opvResponse), productCode)
                    .onErrorResume(
                        e -> {
                          log.warn(
                              "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][READ_THROUGH_CACHE]"
                                  + " Failed to evaluate cached response for clientId= {}."
                                  + " Error={}",
                              clientId,
                              e.getMessage());
                          return Mono.empty();
                        });
              } catch (Exception e) {
                log.warn(
                    "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][READ_THROUGH_CACHE] Failed to"
                        + " parse cached response for clientId= {}. Error={}",
                    clientId,
                    e.getMessage());
                return Mono.empty();
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][READ_THROUGH_CACHE] Cache"
                          + " miss.");
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][READ_THROUGH_CACHE][ERROR] Redis"
                      + " operation failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            });
  }

  @Override
  public Mono<ClientValidationFunnelStatus> writeThroughCache(
      Mono<NsdlPanVerificationRequest> requestMono,
      Mono<NsdlPanVerificationResponse> responseMono,
      Mono<NsdlPanVerificationResult> resultMono,
      String clientID,
      String productCode) {
    log.info(
        "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][WRITE_THROUGH_CACHE] Attempting to cache"
            + " NSDL Pan Validation response.");

    String cacheKey = validationFunnelServiceUtil.buildCacheKey(productCode, clientID);

    return Mono.zip(requestMono, responseMono, resultMono)
        .flatMap(
            tuple -> {
              NsdlPanVerificationRequest request = tuple.getT1();
              NsdlPanVerificationResponse response = tuple.getT2();
              NsdlPanVerificationResult result = tuple.getT3();

              ClientValidationFunnelStatus funnelStatus;
              if (!OpvResponseCode.SUCCESS.getCode().equals(response.getResponseCode())) {
                log.info(
                    "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][WRITE_THROUGH_CACHE] Skipping"
                        + " cache since NSDL Pan Validation responseCode is NOT SUCCESS.");
                funnelStatus =
                    createServiceFailureRedisPojo(request, result, clientID, productCode, response);
              } else {
                funnelStatus =
                    createWriteThroughCacheRedisPojo(
                        request, result, clientID, productCode, response);
              }

              return validationFunnelServiceUtil
                  .cacheObject(cacheKey, funnelStatus)
                  .thenReturn(funnelStatus);
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][WRITE_THROUGH_CACHE][ERROR] Redis"
                      + " operation failed. ClientId= {} productCode= {} Error={}",
                  clientID,
                  productCode,
                  e.getMessage());
              return Mono.empty();
            });
  }

  private ClientValidationFunnelStatus createServiceFailureRedisPojo(
      NsdlPanVerificationRequest request,
      NsdlPanVerificationResult result,
      String clientId,
      String productCode,
      NsdlPanVerificationResponse opvApiResponse) {
    ClientValidationFunnelStatus funnelStatus = new ClientValidationFunnelStatus();
    funnelStatus.setClientId(clientId);
    funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW);

    // Create the step to upsert
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            PAN_VALIDATION,
            NSDL,
            result.toString(),
            ClientValidationFunnelStatus.StepStatus.REJECT,
            ClientValidationFunnelStatus.ServiceStatus.FAILURE);

    // Check for existing step (same vendor + stepName)
    List<ClientValidationFunnelStatus.ValidationStep> steps = funnelStatus.getSteps();
    boolean updated = false;

    for (int i = 0; i < steps.size(); i++) {
      ClientValidationFunnelStatus.ValidationStep step = steps.get(i);
      if (step.getStepName().equals(newStep.getStepName())
          && step.getVendor().equals(newStep.getVendor())) {
        // Update existing step
        steps.set(i, newStep);
        updated = true;
        break;
      }
    }

    // If not found, add it
    if (!updated) {
      steps.add(newStep);
    }

    return funnelStatus;
  }

  private ClientValidationFunnelStatus createWriteThroughCacheRedisPojo(
      NsdlPanVerificationRequest request,
      NsdlPanVerificationResult result,
      String clientId,
      String productCode,
      NsdlPanVerificationResponse opvApiResponse) {

    ClientValidationFunnelStatus funnelStatus = new ClientValidationFunnelStatus();
    funnelStatus.setClientId(clientId);
    funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.PENDING);

    // Determine step status (PASS / REJECT)
    ClientValidationFunnelStatus.StepStatus stepStatusResult = determineStepStatusResult(result);

    if (stepStatusResult == ClientValidationFunnelStatus.StepStatus.REJECT) {
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.REJECT);
    }

    if (stepStatusResult == ClientValidationFunnelStatus.StepStatus.PASS) {
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.PASS);
    }

    // Create or update step
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            PAN_VALIDATION,
            NSDL,
            result.toString(), // store full response JSON
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

  @Override
  public Mono<NsdlPanVerificationResult> evaluateResult(
      Mono<NsdlPanVerificationResponse> opvApiResponseMono, String productCode) {
    log.info(
        "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][EVALUATE_NSDL_RESPONSE]"
            + " Evaluating NSDL Pan Validation response.");

    return opvApiResponseMono
        .flatMap(
            opvApiResponse -> {
              if (opvApiResponse == null) {
                log.warn(
                    "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][EVALUATE_NSDL_RESPONSE] Empty"
                        + " NSDL Pan Validation response received. Skipping evaluating record.");
                return Mono.just(new NsdlPanVerificationResult());
              }

              if (OpvResponseCode.SUCCESS.getCode().equals(opvApiResponse.getResponseCode())) {

                return Flux.fromIterable(opvApiResponse.getOutputData())
                    .flatMap(
                        response -> {
                          Mono<NsdlPanEvaluationResult> validationOutcome =
                              isValidPanRecord(response, productCode);

                          // Build vendor response record
                          NsdlPanVerificationResult.Record panResultRecord =
                              NsdlPanVerificationResult.Record.builder()
                                  .pan(response.getPan())
                                  .panStatus(response.getPanStatus())
                                  .panStatusDesc(
                                      PanStatus.fromCode(response.getPanStatus()).getDescription())
                                  .nameMatch(response.getName())
                                  .dobMatch(response.getDob())
                                  .fathersNameMatch(response.getFatherName())
                                  .seedingStatus(response.getSeedingStatus())
                                  .seedingStatusDesc(
                                      SeedingStatus.fromCode(response.getSeedingStatus())
                                          .getDescription())
                                  .build();

                          // Build evaluation result reactively
                          return validationOutcome.map(
                              validation ->
                                  NsdlPanVerificationResult.VerificationResult.builder()
                                      .evaluationResult(
                                          NsdlPanVerificationResult.EvaluationResult.builder()
                                              .status(determineStatus(validation))
                                              .rejectionReasons(getAllRejectionReasons(validation))
                                              .build())
                                      .vendorResponse(panResultRecord)
                                      .build());
                        })
                    .collectList()
                    .map(
                        results ->
                            NsdlPanVerificationResult.builder()
                                .responseCode(opvApiResponse.getResponseCode())
                                .responseCodeDesc(
                                    OpvResponseCode.fromCode(opvApiResponse.getResponseCode())
                                        .getDescription())
                                .panVerificationResults(results)
                                .build())
                    .doOnSuccess(
                        result -> {
                          log.info(
                              "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][EVALUATE_NSDL_RESPONSE]"
                                  + " Completed evaluation.");
                          log.info(
                              "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][EVALUATE_NSDL_RESPONSE]"
                                  + " Final result: {}",
                              result.toMaskedLogString());
                        });

              } else {
                log.warn(
                    "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][EVALUATE_NSDL_RESPONSE]"
                        + " Non-success NSDL responseCode={} description={}",
                    opvApiResponse.getResponseCode(),
                    OpvResponseCode.fromCode(opvApiResponse.getResponseCode()).getDescription());

                return Mono.just(
                    NsdlPanVerificationResult.builder()
                        .responseCode(opvApiResponse.getResponseCode())
                        .responseCodeDesc(
                            OpvResponseCode.fromCode(opvApiResponse.getResponseCode())
                                .getDescription())
                        .panVerificationResults(Collections.emptyList())
                        .build());
              }
            })
        .switchIfEmpty(Mono.just(new NsdlPanVerificationResult()));
  }

  private NsdlPanVerificationResult.Status determineStatus(NsdlPanEvaluationResult validation) {
    if (validation.isHardReject()) {
      return NsdlPanVerificationResult.Status.REJECTED;
    }
    if (validation.isSoftReject()) {
      return NsdlPanVerificationResult.Status.SOFT_REJECTED;
    }
    // if neither reject flag is true, it is approved.
    return NsdlPanVerificationResult.Status.APPROVED;
  }

  private List<String> getAllRejectionReasons(NsdlPanEvaluationResult validation) {
    List<String> combinedRejections = new ArrayList<>();

    // Add all hard rejections
    if (Objects.nonNull(validation.getHardRejections())) {
      combinedRejections.addAll(validation.getHardRejections());
    }

    // Add all soft rejections
    if (Objects.nonNull(validation.getSoftRejections())) {
      combinedRejections.addAll(validation.getSoftRejections());
    }

    return combinedRejections;
  }

  private ClientValidationFunnelStatus.StepStatus determineStepStatusResult(
      NsdlPanVerificationResult validation) {
    NsdlPanVerificationResult.Status result =
        validation.getPanVerificationResults().get(0).getEvaluationResult().getStatus();

    if (result == NsdlPanVerificationResult.Status.APPROVED) {
      return ClientValidationFunnelStatus.StepStatus.PASS;

    } else if (result == NsdlPanVerificationResult.Status.REJECTED) {
      return ClientValidationFunnelStatus.StepStatus.REJECT;

    } else if (result == NsdlPanVerificationResult.Status.SOFT_REJECTED) {
      return ClientValidationFunnelStatus.StepStatus.SOFT_REJECT;

    } else {
      return ClientValidationFunnelStatus.StepStatus.REJECT;
    }
  }

  @Override
  public Mono<Void> persist(
      Mono<NsdlPanVerificationRequest> requestMono,
      Mono<NsdlPanVerificationResponse> responseMono,
      Mono<NsdlPanVerificationResult> resultMono,
      String clientId,
      String productCode) {
    log.info(
        "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][PERSIST] Initiating persistence of NSDL Pan"
            + " Validation response to DB for clientId={} productCode={}",
        clientId,
        productCode);

    return Mono.zip(requestMono, resultMono, responseMono)
        .flatMap(
            tuple -> {
              NsdlPanVerificationRequest request = tuple.getT1();
              NsdlPanVerificationResult result = tuple.getT2();
              NsdlPanVerificationResponse response = tuple.getT3();

              String requestJson = JsonUtils.serializeResponse(request);
              String responseJson = JsonUtils.serializeResponse(response);

              boolean isServiceSuccess =
                  OpvResponseCode.SUCCESS.getCode().equals(result.getResponseCode());

              String serviceStatus =
                  isServiceSuccess
                      ? ClientValidationFunnelStatus.ServiceStatus.SUCCESS.name()
                      : ClientValidationFunnelStatus.ServiceStatus.FAILURE.name();

              ClientValidationFunnelStatus.StepStatus stepStatusResult =
                  isServiceSuccess
                      ? determineStepStatusResult(result)
                      : ClientValidationFunnelStatus.StepStatus.REJECT;

              ValidationStepEntity stepEntity =
                  ValidationStepEntity.builder()
                      .clientId(clientId)
                      .stepName(PAN_VALIDATION.name())
                      .vendor(NSDL.name())
                      .status(stepStatusResult.name())
                      .serviceStatus(serviceStatus)
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
              } else if (stepStatusResult == ClientValidationFunnelStatus.StepStatus.PASS) {
                // Service succeeded, and the stepStatus result was all PASS.
                finalStatus = ClientValidationFunnelStatus.FinalStatus.PASS;
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
                                    "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][PERSIST][FUNNEL_STATUS][UPDATE][ERROR]"
                                        + " Failed to upsert REJECT status: {} for clientId: {}",
                                    e.getMessage(),
                                    clientId));
              } else {
                // If no final decision was reached (service success and not REJECT),
                // this Mono returns an empty signal to continue the flow.
                funnelUpdate = Mono.empty();
              }

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
                                  "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][PERSIST][STEP_STATUS][PERSIST]"
                                      + " clientId={} vendor={} stepStatus={} serviceStatus={} →"
                                      + " Successfully persisted",
                                  stepEntity.getClientId(),
                                  stepEntity.getVendor(),
                                  stepEntity.getStatus(),
                                  stepEntity.getServiceStatus()))
                      .doOnError(
                          e ->
                              log.error(
                                  "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][PERSIST][STEP_STATUS][PERSIST][ERROR]"
                                      + " clientId={} vendor={} Failed to persist step entity: {}",
                                  stepEntity.getClientId(),
                                  stepEntity.getVendor(),
                                  e.getMessage(),
                                  e))
                      .then()
                      .doFinally(
                          signal ->
                              log.info(
                                  "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][PERSIST] Completed"
                                      + " persistence for clientId={}",
                                  clientId)));
            })
        .doOnError(
            e ->
                log.error(
                    "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][PERSIST][ERROR] clientId={}"
                        + " productCode={} Unexpected error: {}",
                    clientId,
                    productCode,
                    e.getMessage()));
  }

  public Mono<Void> markRemainingStepsSkipped(String clientId, String productCode) {
    List<ClientValidationFunnelStatus.ValidationStep> markRemainingStepsAsSkipped =
        List.of(
            new ClientValidationFunnelStatus.ValidationStep(
                PAN_VALIDATION,
                KARZA,
                null,
                ClientValidationFunnelStatus.StepStatus.SKIPPED,
                ClientValidationFunnelStatus.ServiceStatus.SKIPPED),
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

  private void checkMismatch(
      String field,
      boolean enabled,
      String expected,
      String actual,
      boolean isCritical,
      List<String> hardRejections,
      List<String> softRejections) {

    if (enabled && !expected.equalsIgnoreCase(actual)) {
      String message =
          String.format("%s mismatch (expected: %s, found: %s)", field, expected, actual);

      if (isCritical) {
        hardRejections.add(message);
      } else {
        softRejections.add(message);
      }
    }
  }

  /** Apply configured validation rules against vendor PAN record. */
  public Mono<NsdlPanEvaluationResult> isValidPanRecord(
      NsdlPanVerificationResponse.OutputData record, String productCode) {
    if (Objects.isNull(record)) {
      log.warn(
          "[VALIDATION_SERVICE][NSDL_VALIDATION_SERVICE][VALIDATE_RECORD] Received null record.");
      return Mono.just(
          new NsdlPanEvaluationResult(
              false, false, List.of(StringUtils.EMPTY), List.of(StringUtils.EMPTY)));
    }

    return this.getNsdlPanValidationConfig(productCode)
        .map(
            config -> {
              List<String> hardRejections = new ArrayList<>();
              List<String> softRejections = new ArrayList<>();

              checkMismatch(
                  NsdlRejectionType.PAN_STATUS.getFieldName(),
                  config.isPanStatusCheckEnabled(),
                  config.getPanStatusExpected(),
                  record.getPanStatus(),
                  config.isPanStatusIsCritical(),
                  hardRejections,
                  softRejections);

              checkMismatch(
                  NsdlRejectionType.SEEDING_STATUS.getFieldName(),
                  config.isSeedingStatusCheckEnabled(),
                  config.getSeedingStatusExpectedValue(),
                  record.getSeedingStatus(),
                  config.isSeedingMatchIsCritical(),
                  hardRejections,
                  softRejections);

              checkMismatch(
                  NsdlRejectionType.NAME_STATUS.getFieldName(),
                  config.isNameMatchCheckEnabled(),
                  config.getNameMatchStatusExpected(),
                  record.getName(),
                  config.isNameMatchIsCritical(),
                  hardRejections,
                  softRejections);

              checkMismatch(
                  NsdlRejectionType.DOB_STATUS.getFieldName(),
                  config.isDobMatchCheckEnabled(),
                  config.getDobMatchStatusExpected(),
                  record.getDob(),
                  config.isDobMatchIsCritical(),
                  hardRejections,
                  softRejections);

              boolean hardReject = !hardRejections.isEmpty();
              boolean softReject = !softRejections.isEmpty();

              return new NsdlPanEvaluationResult(
                  hardReject, softReject, hardRejections, softRejections);
            });
  }

  public Mono<ValidationFunnelConfiguration.NsdlPanValidationConfig> getNsdlPanValidationConfig(
      String productCode) {
    return validationFunnelConfigService
        .getValidationFunnelConfig(productCode)
        .flatMap(
            funnelConfig -> {
              return Mono.just(funnelConfig.getNsdlPanValidationConfig());
            });
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
}
