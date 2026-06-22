package com.trillionloans.los.service.validationservice;

import static com.trillionloans.los.model.request.ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL;
import static com.trillionloans.los.util.LeadDataUtil.extractXmlFromBase64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.exception.PanValidationExceptions.DOBWaterfallValidationException;
import com.trillionloans.los.model.ClientValidationFunnelStatusEntity;
import com.trillionloans.los.model.DOBWaterfallRequest;
import com.trillionloans.los.model.DOBWaterfallResult;
import com.trillionloans.los.model.ValidationStepEntity;
import com.trillionloans.los.model.dto.AadhaarXmlDetailsDTO;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.request.ClientValidationFunnelStatus;
import com.trillionloans.los.model.response.DOBWaterfallResponse;
import com.trillionloans.los.repository.DobWaterfallResultRepository;
import com.trillionloans.los.service.LoanLevelClientDetailsService;
import com.trillionloans.los.service.ckyc.AadhaarXmlServiceImpl;
import com.trillionloans.los.service.db.ClientValidationFunnelStatusRepository;
import com.trillionloans.los.service.db.ClientValidationFunnelStepsRepository;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.util.DOBDateParserUtil;
import com.trillionloans.los.util.JsonUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
public class DOBWaterfallValidationService
    implements ValidationService<DOBWaterfallRequest, DOBWaterfallResult, DOBWaterfallResponse> {

  private final ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository;
  private final ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository;

  private final ValidationFunnelServiceUtil validationFunnelServiceUtil;
  private final ValidationFunnelConfigService validationFunnelConfigService;
  private final KafkaEventProducerService eventProducerService;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String VENDOR_NAME = "IN_HOUSE_DOB_WATERFALL";

  private final AadhaarXmlServiceImpl aadhaarXmlService;
  private final DobWaterfallResultRepository repository;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;
  private final M2PWrapperApi m2PWrapperApi;

  private static final String TIME_ZONE_KOLKATA = "Asia/Kolkata";
  private static final String PERSIST_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

  public DOBWaterfallValidationService(
      LoanLevelClientDetailsService loanLevelClientDetailsService,
      AadhaarXmlServiceImpl aadhaarXmlService,
      DobWaterfallResultRepository repository,
      ValidationFunnelServiceUtil validationFunnelServiceUtil,
      ClientValidationFunnelStepsRepository clientValidationFunnelStepsRepository,
      ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository,
      ValidationFunnelConfigService validationFunnelConfigService,
      KafkaEventProducerService eventProducerService,
      M2PWrapperApi m2PWrapperApi) {

    this.repository = repository;
    this.aadhaarXmlService = aadhaarXmlService;
    this.loanLevelClientDetailsService = loanLevelClientDetailsService;
    this.validationFunnelServiceUtil = validationFunnelServiceUtil;
    this.clientValidationFunnelStepsRepository = clientValidationFunnelStepsRepository;
    this.clientValidationFunnelStatusRepository = clientValidationFunnelStatusRepository;
    this.validationFunnelConfigService = validationFunnelConfigService;
    this.eventProducerService = eventProducerService;
    this.m2PWrapperApi = m2PWrapperApi;
  }

  @Override
  public String getVendorName() {
    return VENDOR_NAME;
  }

  public Mono<Void> triggerDOBWaterfallValidationService(
      String base64Xml, String clientId, String loanApplicationId, String productCode) {
    // 1. Fetch Pan DOB
    Mono<String> panDobMono = fetchPanDob(clientId, loanApplicationId, productCode);

    // 2. Extract Aadhaar DOB from XML
    Mono<String> aadharDobMono = extractAadhaarDobFromXml(base64Xml, clientId);

    return Mono.zip(panDobMono, aadharDobMono)
        .flatMap(
            tuple -> {
              String panDob = tuple.getT1();
              String aadharDob = tuple.getT2();

              DOBWaterfallRequest dobWaterfallRequest = new DOBWaterfallRequest(panDob, aadharDob);
              return init(Mono.just(dobWaterfallRequest), productCode, clientId, loanApplicationId);
            })
        .then();
  }

  public Mono<Void> triggerStandaloneDOBWaterfallValidationService(
      String base64Xml, String clientId, String loanApplicationId, String productCode) {

    // 1. Fetch Pan DOB
    Mono<String> panDobMono = fetchPanDob(clientId, loanApplicationId, productCode);

    // 2. Extract Aadhaar DOB from XML
    Mono<String> aadharDobMono = extractAadhaarDobFromXml(base64Xml, clientId);

    return Mono.zip(panDobMono, aadharDobMono)
        .flatMap(
            tuple -> {
              String panDob = tuple.getT1();
              String aadharDob = tuple.getT2();

              DOBWaterfallRequest dobWaterfallRequest = new DOBWaterfallRequest(panDob, aadharDob);
              return initStandaloneDOBwaterfall(
                  Mono.just(dobWaterfallRequest), productCode, clientId);
            })
        .doOnError(
            e ->
                log.error(
                    "[DOB_FUNNEL][ERROR] Final exception in DOB funnel execution for clientId={},"
                        + " error={}",
                    clientId,
                    e.getMessage()))
        .then();
  }

  @Override
  public Mono<DOBWaterfallResult> init(
      Mono<DOBWaterfallRequest> requestMono,
      String productCode,
      String clientId,
      String loanApplicationId) {
    log.info(
        "[VALIDATION_SERVICE][DOB_WATERFALL][INIT] Starting DOB waterfall"
            + " validation. clientId={}, productCode={}",
        clientId,
        productCode);

    return requestMono
        .flatMap(
            request -> {
              // we will not read from cache for dob waterfall, but will write through the cache.
              return initializeWaterfall(request, clientId, productCode)
                  .flatMap(
                      tuple -> {
                        DOBWaterfallResult result = tuple.getT1();
                        DOBWaterfallResponse response = tuple.getT2();

                        Event event =
                            result.getEvaluationResult().getEvaluationSatus()
                                    == DOBWaterfallResult.Status.APPROVED
                                ? Event.DOB_WATERFALL_APPROVED
                                : Event.DOB_WATERFALL_REJECTED;
                        publishEventKafkaAsync(
                            () ->
                                eventProducerService.publishEvent(
                                    new EventContext(event, loanApplicationId, clientId),
                                    null,
                                    null));

                        // cache write
                        return writeThroughCache(
                                Mono.just(request),
                                Mono.just(response),
                                Mono.just(result),
                                clientId,
                                productCode)
                            .thenReturn(result);
                      });
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[VALIDATION_SERVICE][DOB_WATERFALL][ERROR] DOB waterfall"
                      + " validation failed. clientId={}, productCode={}, error={}",
                  clientId,
                  productCode,
                  e.getMessage());

              return validationFunnelServiceUtil
                  .persistFailure(
                      e,
                      clientId,
                      productCode,
                      ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL,
                      IN_HOUSE_DOB_WATERFALL,
                      "DOB_WATERFALL")
                  .then(
                      Mono.error(
                          new DOBWaterfallValidationException(
                              "Error while executing DOB waterfall validation",
                              e.getMessage(),
                              HttpStatus.INTERNAL_SERVER_ERROR)));
            });
  }

  public Mono<DOBWaterfallResponse> initStandaloneDOBwaterfall(
      Mono<DOBWaterfallRequest> requestMono, String productCode, String clientId) {
    log.info(
        "[STANDALONE_DOB_WATERFALL][INIT] Starting DOB waterfall validation. clientId={},"
            + " productCode={}",
        clientId,
        productCode);

    return requestMono
        .flatMap(
            request ->
                runDOBWaterFall(requestMono)
                    .flatMap(
                        response ->
                            logIntoDBForStandAloneDOBWaterfallResult(
                                    Mono.just(request), Mono.just(response), clientId, productCode)
                                .thenReturn(response)))
        .onErrorMap(
            e -> {
              log.error(
                  "[STANDALONE_DOB_WATERFALL][ERROR] DOB waterfall validation failed. clientId={},"
                      + " productCode={}, error={}",
                  clientId,
                  productCode,
                  e.getMessage());

              return new DOBWaterfallValidationException(
                  "Error while executing DOB waterfall validation",
                  e.getMessage(),
                  HttpStatus.INTERNAL_SERVER_ERROR);
            });
  }

  public Mono<Integer> logIntoDBForStandAloneDOBWaterfallResult(
      Mono<DOBWaterfallRequest> requestMono,
      Mono<DOBWaterfallResponse> responseMono,
      String clientId,
      String productCode) {

    return Mono.zip(requestMono, responseMono)
        .flatMap(tuple -> this.prepareAndUpsertResult(tuple, clientId, productCode))
        .doOnError(
            e -> log.error("Failed to persist DOB waterfall result. Error: {}", e.getMessage()));
  }

  Mono<Tuple2<DOBWaterfallResult, DOBWaterfallResponse>> initializeWaterfall(
      DOBWaterfallRequest request, String clientId, String productCode) {
    return runDOBWaterFall(Mono.just(request))
        .flatMap(
            response ->
                evaluateResult(Mono.just(response), productCode)
                    .flatMap(
                        result ->
                            persist(
                                    Mono.just(request),
                                    Mono.just(response),
                                    Mono.just(result),
                                    clientId,
                                    productCode)
                                .thenReturn(Tuples.of(result, response))));
  }

  public Mono<DOBWaterfallResponse> runDOBWaterFall(Mono<DOBWaterfallRequest> requestMono) {
    return requestMono
        .flatMap(
            request -> {
              LocalDate panDOB = null;
              LocalDate aadhaarDOB = null;
              List<String> rejectionReasons = new ArrayList<>();

              // --- 1. Basic Input Validation ---
              if (request == null
                  || StringUtils.isEmpty(request.getPanDOB())
                  || StringUtils.isEmpty(request.getAadhaarDOB())) {
                String missingField =
                    (request == null || StringUtils.isEmpty(request.getPanDOB()))
                        ? "PAN DOB"
                        : "Aadhaar DOB";
                log.info("[DOB_WATERFALL] {} Date is null/empty. Hard failing.", missingField);
                rejectionReasons.add(missingField + " is null/empty.");
                return Mono.just(createHardFailResponse(rejectionReasons));
              }

              // --- 2. Attempt to Parse Dates ---

              // 2a. Parse PAN DOB
              try {
                panDOB = DOBDateParserUtil.parseDate(request.getPanDOB());
              } catch (DateTimeParseException e) {
                log.info(
                    "[DOB_WATERFALL][ERROR] PAN DOB Date parsing failed for: {}, error: {}",
                    request.getPanDOB(),
                    e.getMessage());
                rejectionReasons.add("PAN DOB parsing failed");
              }

              // 2b. Parse Aadhaar DOB
              try {
                aadhaarDOB = DOBDateParserUtil.safeParseAadhaarDate(request.getAadhaarDOB());
              } catch (DateTimeParseException e) {
                log.info(
                    "[DOB_WATERFALL][ERROR] Aadhaar DOB Date parsing failed for: {}, error: {}",
                    request.getAadhaarDOB(),
                    e.getMessage());
                rejectionReasons.add("Aadhaar DOB parsing failed");
              }

              // --- 3. Hard Fail Check (If either date failed to parse) ---
              if (panDOB == null || aadhaarDOB == null) {
                return Mono.just(createHardFailResponse(rejectionReasons));
              }

              // --- 4. Execute Waterfall Logic ---
              return executeWaterfallLogic(panDOB, aadhaarDOB, rejectionReasons);
            })
        .onErrorResume(Exception.class, this::handleUnexpectedError);
  }

  private Mono<DOBWaterfallResponse> executeWaterfallLogic(
      LocalDate panDOB, LocalDate aadhaarDOB, List<String> rejectionReasons) {

    boolean finalStatus = false;
    boolean ruleOneTriggered = true; // Always triggered at this stage
    boolean ruleOnePass = false;
    boolean ruleTwoTriggered = false;
    boolean ruleTwoPass = false;

    // --- 1. Logic 1 (Rule 1): Exact DOB Match (DD/MM/YYYY) ---
    if (panDOB.isEqual(aadhaarDOB)) {
      ruleOnePass = true;
      finalStatus = true;
      log.info(
          "[DOB_WATERFALL][RULE_1_PASSED] Exact DOB Match found: {} == {}", panDOB, aadhaarDOB);
    } else {
      log.info(
          "[DOB_WATERFALL][RULE_1_FAILED] DOB mismatch ({} vs {}). Proceeding to Logic 2.",
          panDOB,
          aadhaarDOB);
    }

    // --- 2. Logic 2 (Rule 2): Year Match with 01/01 Default ---
    if (!finalStatus) {
      ruleTwoTriggered = true; // Rule 2 is now triggered

      boolean isYearMatch = panDOB.getYear() == aadhaarDOB.getYear();

      // Rule: Year(PAN_DOB) == Year(Aadhaar_DOB)
      if (isYearMatch) {
        ruleTwoPass = true;
        finalStatus = true;
        log.info(
            "[DOB_WATERFALL][RULE_2_PASSED] Year Match found ({} == {}) AND Aadhaar is 01/01 ({}).",
            panDOB.getYear(),
            aadhaarDOB.getYear(),
            aadhaarDOB);
      } else {
        log.info(
            "[DOB_WATERFALL][RULE_2_FAILED] Logic 2 failed. Year Match: {}, Aadhaar Date: {}",
            isYearMatch,
            aadhaarDOB);
      }
    }

    if (!finalStatus) {
      rejectionReasons.add("DOB waterfall match failed for PAN DOB and Aadhaar DOB");
    }

    DOBWaterfallResponse response =
        new DOBWaterfallResponse(
            finalStatus,
            ruleOneTriggered,
            ruleOnePass,
            ruleTwoTriggered,
            ruleTwoPass,
            rejectionReasons);

    log.info(
        "[DOB_WATERFALL][FINAL_OUTCOME] Validation completed. Status: {}, Details: {}",
        finalStatus ? "PASS" : "FAIL",
        response);

    return Mono.just(response);
  }

  @Override
  public Mono<DOBWaterfallResult> readThroughCache(
      Mono<DOBWaterfallRequest> request, String productCode, String clientId) {
    log.info(
        "[VALIDATION_SERVICE][DOB_WATERFALL][READ_THROUGH_CACHE]"
            + " Attempting to fetch validation result from Redis cache.");

    String cacheKey = validationFunnelServiceUtil.buildCacheKey(productCode, clientId);

    return validationFunnelServiceUtil
        .getObjectFromCache(cacheKey, ClientValidationFunnelStatus.class)
        .flatMap(
            clientValidationFunnelStatus -> {
              if (Objects.isNull(clientValidationFunnelStatus)) {
                return Mono.empty();
              }

              log.info("[VALIDATION_SERVICE][DOB_WATERFALL][READ_THROUGH_CACHE] Cache hit.");

              try {
                Optional<ClientValidationFunnelStatus.ValidationStep> dobWaterfallStepOpt =
                    clientValidationFunnelStatus.getSteps().stream()
                        .filter(
                            step ->
                                step.getStepName()
                                        == ClientValidationFunnelStatus.StepName
                                            .IN_HOUSE_DOB_WATERFALL
                                    && step.getVendor()
                                        == ClientValidationFunnelStatus.Vendor
                                            .IN_HOUSE_DOB_WATERFALL)
                        .findFirst();

                if (dobWaterfallStepOpt.isEmpty()) {
                  return Mono.empty();
                }

                ClientValidationFunnelStatus.ValidationStep dobWaterfallStep =
                    dobWaterfallStepOpt.get();
                String dobWaterfallResponseStr = dobWaterfallStep.getResponse();

                DOBWaterfallResponse dobWaterfallResponse =
                    OBJECT_MAPPER.readValue(dobWaterfallResponseStr, DOBWaterfallResponse.class);

                return this.evaluateResult(Mono.just(dobWaterfallResponse), productCode)
                    .onErrorResume(
                        e -> {
                          log.warn(
                              "[VALIDATION_SERVICE][DOB_WATERFALL][READ_THROUGH_CACHE]"
                                  + " Failed to evaluate cached response. Error={}",
                              e.getMessage());
                          return Mono.empty();
                        });

              } catch (Exception e) {
                log.warn(
                    "[VALIDATION_SERVICE][DOB_WATERFALL][READ_THROUGH_CACHE][ERROR] Failed to parse"
                        + " cached response. Error={}",
                    e.getMessage());
                return Mono.empty();
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("[VALIDATION_SERVICE][DOB_WATERFALL][READ_THROUGH_CACHE] Cache miss.");
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][DOB_WATERFALL][READ_THROUGH_CACHE][ERROR] Redis"
                      + " operation failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            });
  }

  @Override
  public Mono<?> writeThroughCache(
      Mono<DOBWaterfallRequest> requestMono,
      Mono<DOBWaterfallResponse> responseMono,
      Mono<DOBWaterfallResult> resultMono,
      String clientId,
      String productCode) {
    log.info(
        "[VALIDATION_SERVICE][DOB_WATERFALL][WRITE_THROUGH_CACHE] Attempting to"
            + " cache {} response async",
        getVendorName());

    String key = validationFunnelServiceUtil.buildCacheKey(productCode, clientId);

    return Mono.zip(requestMono, responseMono, resultMono)
        .flatMap(
            tuple -> {
              DOBWaterfallRequest request = tuple.getT1();
              DOBWaterfallResponse response = tuple.getT2();
              DOBWaterfallResult result = tuple.getT3();

              return validationFunnelServiceUtil
                  .getObjectFromCache(key, ClientValidationFunnelStatus.class)
                  .flatMap(
                      funnelStatus -> {
                        // Update funnelStatus based on response code
                        if (result
                            .getEvaluationResult()
                            .getEvaluationSatus()
                            .equals(DOBWaterfallResult.Status.SERVICE_UNAVAILABLE)) {
                          funnelStatus =
                              createFailureWriteThroughCacheObejct(
                                  funnelStatus, response, result, clientId);
                        } else {
                          funnelStatus =
                              createWriteThroughCacheObject(
                                  funnelStatus, response, result, clientId);
                        }

                        // Cache updated object
                        return validationFunnelServiceUtil
                            .cacheObject(key, funnelStatus)
                            .doOnError(
                                e ->
                                    log.error(
                                        "[VALIDATION_SERVICE][DOB_WATERFALL][WRITE_THROUGH_CACHE][ERROR]"
                                            + " Failed to cache {} response. Error={}",
                                        getVendorName(),
                                        e.getMessage()))
                            .thenReturn(funnelStatus);
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            ClientValidationFunnelStatus funnelStatus =
                                createWriteThroughCacheObject(
                                    new ClientValidationFunnelStatus(), response, result, clientId);
                            return validationFunnelServiceUtil
                                .cacheObject(key, funnelStatus)
                                .doOnError(
                                    e ->
                                        log.error(
                                            "[VALIDATION_SERVICE][DOB_WATERFALL][WRITE_THROUGH_CACHE][ERROR]"
                                                + " Failed to cache {} response. Error={}",
                                            getVendorName(),
                                            e.getMessage()))
                                .thenReturn(funnelStatus);
                          }));
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "[VALIDATION_SERVICE][DOB_WATERFALL][WRITE_THROUGH_CACHE][ERROR]"
                      + " Redis operation failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            });
  }

  private ClientValidationFunnelStatus createWriteThroughCacheObject(
      ClientValidationFunnelStatus funnelStatus,
      DOBWaterfallResponse response,
      DOBWaterfallResult result,
      String clientId) {

    if (result
        .getEvaluationResult()
        .getEvaluationSatus()
        .equals(DOBWaterfallResult.Status.APPROVED)) {
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.PASS);
    } else if (result
        .getEvaluationResult()
        .getEvaluationSatus()
        .equals(DOBWaterfallResult.Status.REJECTED)) {
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.REJECT);
    } else if (result
        .getEvaluationResult()
        .getEvaluationSatus()
        .equals(DOBWaterfallResult.Status.SERVICE_UNAVAILABLE)) {
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.REJECT);
    } else {
      funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.PENDING);
    }

    // Determine step status (PASS / REJECT)
    ClientValidationFunnelStatus.StepStatus stepStatusResult =
        result.getEvaluationResult().getEvaluationSatus().equals(DOBWaterfallResult.Status.APPROVED)
            ? ClientValidationFunnelStatus.StepStatus.PASS
            : ClientValidationFunnelStatus.StepStatus.REJECT;

    // Create or update step
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL,
            ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
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
      DOBWaterfallResponse response,
      DOBWaterfallResult result,
      String clientId) {

    funnelStatus.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.REJECT);

    // Create or update step
    ClientValidationFunnelStatus.ValidationStep newStep =
        new ClientValidationFunnelStatus.ValidationStep(
            ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL,
            ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
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
  public Mono<DOBWaterfallResult> evaluateResult(
      Mono<DOBWaterfallResponse> response, String productCode) {
    log.info(
        "[VALIDATION_SERVICE][DOB_WATERFALL][EVALUATE_DOB_WATERFALL_RESPONSE]"
            + " Evaluating DOB waterfall response");

    return response
        .map(
            dobResponse -> {
              DOBWaterfallResult.Status status;
              String rejectionReason = null;

              if (dobResponse.isDobWaterFallFinalStatus()) {
                status = DOBWaterfallResult.Status.APPROVED;
              } else {
                status = DOBWaterfallResult.Status.REJECTED;
                rejectionReason =
                    Optional.ofNullable(dobResponse.getRejectionReasons())
                        .filter(list -> !list.isEmpty())
                        .map(
                            list ->
                                list.stream()
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.joining(", ")))
                        .orElse(StringUtils.EMPTY);
              }

              DOBWaterfallResult.EvaluationResult evaluationResult =
                  DOBWaterfallResult.EvaluationResult.builder()
                      .evaluationSatus(status)
                      .rejectionReason(rejectionReason)
                      .build();

              log.info(
                  "[VALIDATION_SERVICE][DOB_WATERFALL][EVALUATE_DOB_WATERFALL_RESPONSE]"
                      + " Successfully evaluated DOB waterfall response. Evaluated Result: {}",
                  evaluationResult);

              return new DOBWaterfallResult(dobResponse, evaluationResult);
            })
        .onErrorResume(
            throwable -> {
              log.error(
                  "[VALIDATION_SERVICE][DOB_WATERFALL][EVALUATE_DOB_WATERFALL_RESPONSE][ERROR]"
                      + " Evaluation failed. Error={}",
                  throwable.getMessage());

              DOBWaterfallResult.EvaluationResult errorEvaluation =
                  DOBWaterfallResult.EvaluationResult.builder()
                      .evaluationSatus(DOBWaterfallResult.Status.SERVICE_UNAVAILABLE)
                      .rejectionReason(
                          "An unexpected error occurred during evaluation: "
                              + throwable.getMessage())
                      .build();

              return Mono.just(new DOBWaterfallResult(null, errorEvaluation));
            });
  }

  @Override
  public Mono<?> persist(
      Mono<DOBWaterfallRequest> requestMono,
      Mono<DOBWaterfallResponse> responseMono,
      Mono<DOBWaterfallResult> evaluatedResult,
      String clientId,
      String productCode) {
    return Mono.zip(evaluatedResult, responseMono, requestMono)
        .flatMap(
            tuple -> {
              DOBWaterfallResult result = tuple.getT1();
              DOBWaterfallResponse response = tuple.getT2();
              DOBWaterfallRequest request = tuple.getT3();

              String requestJson = JsonUtils.serializeResponse(request);
              String responseJson = JsonUtils.serializeResponse(response);

              DOBWaterfallResult.Status dobWaterfallStatus =
                  result.getEvaluationResult().getEvaluationSatus();

              boolean isServiceSuccessful =
                  dobWaterfallStatus != DOBWaterfallResult.Status.SERVICE_UNAVAILABLE;

              ClientValidationFunnelStatus.ServiceStatus finalServiceStatus =
                  isServiceSuccessful
                      ? ClientValidationFunnelStatus.ServiceStatus.SUCCESS
                      : ClientValidationFunnelStatus.ServiceStatus.FAILURE;

              ClientValidationFunnelStatus.StepStatus finalStepStatus =
                  !isServiceSuccessful
                      ? ClientValidationFunnelStatus.StepStatus.REJECT
                      : (dobWaterfallStatus == DOBWaterfallResult.Status.APPROVED)
                          ? ClientValidationFunnelStatus.StepStatus.PASS
                          : ClientValidationFunnelStatus.StepStatus.REJECT;

              ValidationStepEntity stepEntity =
                  ValidationStepEntity.builder()
                      .clientId(clientId)
                      .stepName(IN_HOUSE_DOB_WATERFALL.name())
                      .vendor(IN_HOUSE_DOB_WATERFALL.name())
                      .status(finalStepStatus.name())
                      .serviceStatus(finalServiceStatus.name())
                      .request(requestJson)
                      .response(responseJson)
                      .build();

              boolean shouldRejectFunnel =
                  finalServiceStatus == ClientValidationFunnelStatus.ServiceStatus.FAILURE
                      || finalStepStatus == ClientValidationFunnelStatus.StepStatus.REJECT;

              Mono<Void> funnelUpdate =
                  (shouldRejectFunnel)
                      ? clientValidationFunnelStatusRepository
                          .upsertFunnelStatus(
                              clientId,
                              productCode,
                              finalServiceStatus
                                      == ClientValidationFunnelStatus.ServiceStatus.FAILURE
                                  ? ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW.name()
                                  : ClientValidationFunnelStatus.FinalStatus.REJECT.name())
                          .then()
                      : clientValidationFunnelStatusRepository
                          .upsertFunnelStatus(
                              clientId,
                              productCode,
                              ClientValidationFunnelStatus.FinalStatus.PASS.name())
                          .then();

              Mono<Integer> resultDbUpdate =
                  prepareAndUpsertResult(
                          reactor.util.function.Tuples.of(request, response), clientId, productCode)
                      .doOnError(
                          e ->
                              log.error(
                                  "[DOB_WATERFALL][RESULT][UPSERT][ERROR] clientId={} {}",
                                  clientId,
                                  e.getMessage()));

              return funnelUpdate
                  .then(
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
                          .doOnError(
                              e ->
                                  log.error(
                                      "[DOB_WATERFALL][STEP_STATUS][PERSIST][ERROR] clientId={}"
                                          + " vendor={} {}",
                                      stepEntity.getClientId(),
                                      stepEntity.getVendor(),
                                      e.getMessage())))
                  .then(resultDbUpdate);
            })
        .doOnError(
            e ->
                log.error(
                    "[DOB_WATERFALL][PERSIST][ERROR] clientId={} productCode={} {}",
                    clientId,
                    productCode,
                    e.getMessage()));
  }

  private Mono<Integer> prepareAndUpsertResult(
      reactor.util.function.Tuple2<DOBWaterfallRequest, DOBWaterfallResponse> tuple,
      String clientId,
      String productCode) {
    DOBWaterfallRequest request = tuple.getT1();
    DOBWaterfallResponse response = tuple.getT2();

    String reasons =
        Optional.ofNullable(response.getRejectionReasons())
            .filter(list -> !list.isEmpty())
            .map(list -> list.stream().filter(Objects::nonNull).collect(Collectors.joining(", ")))
            .orElse(StringUtils.EMPTY);

    String resultStatus =
        response.isDobWaterFallFinalStatus()
            ? DOBWaterfallResult.Status.APPROVED.name()
            : DOBWaterfallResult.Status.REJECTED.name();

    String stringifiedDateTimeFormat =
        LocalDateTime.now(ZoneId.of(TIME_ZONE_KOLKATA))
            .format(DateTimeFormatter.ofPattern(PERSIST_DATE_TIME_FORMAT));

    LocalDateTime dateTime =
        LocalDateTime.parse(
            stringifiedDateTimeFormat, DateTimeFormatter.ofPattern(PERSIST_DATE_TIME_FORMAT));

    return repository.upsertByClientIdAndProductCode(
        clientId,
        productCode,
        resultStatus,
        request.getPanDOB(),
        request.getAadhaarDOB(),
        reasons,
        dateTime);
  }

  private Mono<String> fetchPanDob(String clientId, String loanApplicationId, String productCode) {
    if (loanApplicationId == null) {
      return loanLevelClientDetailsService
          .findLatestByClientIdAndProductCode(clientId, productCode)
          .map(
              loanLevelClientDetail ->
                  Optional.ofNullable(loanLevelClientDetail.getDateOfBirth())
                      .orElse(StringUtils.EMPTY))
          .switchIfEmpty(
              Mono.defer(
                  () -> {
                    log.error(
                        "[LOAN_LEVEL_CLIENT_DATA][DOB_FUNNEL][DB_EMPTY] No entry for clientId: {}."
                            + " falling back to LeadService.",
                        clientId);
                    return fallbackToLeadService(clientId);
                  }))
          // DB Error -> Fallback to Lead Service
          .onErrorResume(
              e -> {
                log.error(
                    "[LOAN_LEVEL_CLIENT_DATA][DOB_FUNNEL][DB_ERROR] Failed for clientId: {}. error:"
                        + " {}. Falling back to LeadService.",
                    clientId,
                    e.getMessage());
                return fallbackToLeadService(clientId);
              });

    } else {
      return fetchClientSignal(clientId, loanApplicationId, productCode)
          .flatMap(
              signal -> {
                if (signal.isOnNext() && signal.get() != null) {
                  return Mono.just(
                      Optional.ofNullable(signal.get().getDateOfBirth()).orElse(StringUtils.EMPTY));
                }
                return Mono.just(StringUtils.EMPTY);
              })
          .switchIfEmpty(Mono.just(StringUtils.EMPTY))
          .doOnError(
              e ->
                  log.error(
                      "[DOB_FUNNEL][ERROR] Signal fetch failed for loanId: {}",
                      loanApplicationId,
                      e))
          .onErrorReturn(StringUtils.EMPTY);
    }
  }

  Mono<Signal<LoanLevelClientDetailsCacheDTO>> fetchClientSignal(
      String clientId, String loanApplicationId, String productCode) {
    return Mono.deferContextual(
        ctx ->
            loanLevelClientDetailsService
                .fetchLoanLevelClientDetails(clientId, loanApplicationId, productCode)
                .contextWrite(ctx)
                .materialize());
  }

  /** Helper to call Lead Service and extract the PAN DOB */
  private Mono<String> fallbackToLeadService(String clientId) {
    return m2PWrapperApi
        .getLeadData(clientId)
        .map(
            clientDetailsResponseDto ->
                Optional.ofNullable(clientDetailsResponseDto.getDateOfBirth())
                    .filter(StringUtils::isNotBlank)
                    .orElse(StringUtils.EMPTY))
        .onErrorResume(
            err -> {
              log.error(
                  "[LOAN_LEVEL_CLIENT_DATA][DOB_FUNNEL][LEAD_FALLBACK_FAILED] lead service failed"
                      + " for clientId: {}. error: {}",
                  clientId,
                  err.getMessage());
              return Mono.just(StringUtils.EMPTY);
            })
        .defaultIfEmpty(StringUtils.EMPTY);
  }

  private Mono<String> extractAadhaarDobFromXml(String base64Xml, String clientId) {
    return extractXmlFromBase64(base64Xml)
        .map(AadhaarXmlDetailsDTO::getDob)
        .defaultIfEmpty(StringUtils.EMPTY)
        .doOnError(
            e ->
                log.error(
                    "[DOB_FUNNEL][ERROR] Failed to extract Aadhaar DOB from XML for clientId={}",
                    clientId,
                    e))
        .onErrorResume(e -> Mono.just(StringUtils.EMPTY));
  }

  private Mono<DOBWaterfallResponse> handleUnexpectedError(Exception e) {
    log.error(
        "[DOB_WATERFALL][ERROR][UNEXPECTED_FAIL] Unhandled exception processing request. Error: {}",
        e.getMessage(),
        e);
    return Mono.just(createHardFailResponse(Collections.singletonList("unexpected error")));
  }

  public static DOBWaterfallResponse createHardFailResponse(List<String> rejectionReasons) {
    return DOBWaterfallResponse.builder()
        .dobWaterFallFinalStatus(false)
        .ruleOneTriggered(false)
        .ruleOnePass(false)
        .ruleTwoTriggered(false)
        .ruleTwoPass(false)
        .rejectionReasons(rejectionReasons != null ? rejectionReasons : Collections.emptyList())
        .build();
  }

  public Mono<Void> updateDatabaseForDobWaterfall(String clientId, String productCode) {
    return Mono.zip(
            clientValidationFunnelStatusRepository.findByClientIdAndProductCode(
                clientId, productCode),
            clientValidationFunnelStepsRepository.findByClientIdAndProductCodeAndStepNameAndVendor(
                clientId,
                productCode,
                ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL.name(),
                IN_HOUSE_DOB_WATERFALL.name()))
        .flatMap(
            tuple -> {
              ClientValidationFunnelStatusEntity finalStatusEntity = tuple.getT1();
              ValidationStepEntity stepEntity = tuple.getT2();

              // Check if we need to update
              if (ClientValidationFunnelStatus.FinalStatus.INIT
                  .name()
                  .equals(finalStatusEntity.getFinalStatus())) {
                return clientValidationFunnelStatusRepository
                    .upsertFunnelStatus(
                        clientId, productCode, ClientValidationFunnelStatus.FinalStatus.PASS.name())
                    .then(
                        clientValidationFunnelStepsRepository.upsertStep(
                            stepEntity.getClientId(),
                            productCode,
                            stepEntity.getStepName(),
                            stepEntity.getVendor(),
                            ClientValidationFunnelStatus.StepStatus.SKIPPED.name(),
                            ClientValidationFunnelStatus.ServiceStatus.SKIPPED.name(),
                            stepEntity.getRequest(),
                            stepEntity.getResponse()));
              }

              return Mono.empty();
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[VALIDATION_SERVICE][KYC_CALLBACK] No data found for client: {}", clientId);
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.error(
                  "[VALIDATION_SERVICE][KYC_CALLBACK][ERROR] Operation failed: {}", e.getMessage());
              return Mono.empty();
            })
        .then();
  }

  private void publishEventKafkaAsync(Runnable eventTask) {
    Mono.fromRunnable(eventTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error while publishing the event", error));
  }
}
