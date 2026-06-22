package com.trillionloans.los.service.validationservice;

import com.trillionloans.los.config.ValidationFunnelProperties;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.mapper.KarzaInternalStatusEnum;
import com.trillionloans.los.mapper.OpvResponseCode;
import com.trillionloans.los.mapper.PanValidationServiceType;
import com.trillionloans.los.model.ClientValidationFunnelStatusEntity;
import com.trillionloans.los.model.KarzaNameSimilarityRequest;
import com.trillionloans.los.model.NsdlPanVerificationResult;
import com.trillionloans.los.model.ValidationStepEntity;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.request.ClientValidationFunnelStatus;
import com.trillionloans.los.model.request.KarzaPanAuthenticateRequest;
import com.trillionloans.los.model.request.KarzaPanAuthenticateResult;
import com.trillionloans.los.model.request.NsdlPanVerificationRequest;
import com.trillionloans.los.service.LoanLevelClientDetailsService;
import com.trillionloans.los.service.StandalonePanValidationService;
import com.trillionloans.los.service.db.ClientValidationFunnelStatusRepository;
import com.trillionloans.los.service.db.ClientValidationFunnelStepsRepository;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.util.PanValidationUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Service
public class ValidationFunnelService {
  private final ValidationFunnelProperties properties;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  // vendor impl services
  private final NsdlPanValidationService nsdlValidationService;
  private final DOBWaterfallValidationService dobValidationService;
  private final KarzaPanValidationService karzaPanValidationService;
  private final KarzaNameSimilarityService karzaNameSimilarityService;

  // config service
  private final ValidationFunnelConfigService validationFunnelConfigService;

  // repositories
  private final ClientValidationFunnelStepsRepository validationStepRepository;
  private final ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository;

  // standalone nsdl pan validation service
  private final StandalonePanValidationService standalonePanValidationService;

  private final ValidationFunnelServiceUtil funnelServiceUtil;
  private final KafkaEventProducerService eventProducerService;

  public ValidationFunnelService(
      ValidationFunnelProperties properties,
      ValidationFunnelConfigService validationFunnelConfigService,
      LoanLevelClientDetailsService loanLevelClientDetailsService,
      NsdlPanValidationService nsdlValidationService,
      KarzaPanValidationService karzaPanValidationService,
      KarzaNameSimilarityService karzaNameSimilarityService,
      DOBWaterfallValidationService dobValidationService,
      ClientValidationFunnelStatusRepository clientValidationFunnelStatusRepository,
      ClientValidationFunnelStepsRepository validationStepRepository,
      StandalonePanValidationService standalonePanValidationService,
      ValidationFunnelServiceUtil funnelServiceUtil,
      KafkaEventProducerService eventProducerService) {
    this.properties = properties;
    this.validationFunnelConfigService = validationFunnelConfigService;
    this.loanLevelClientDetailsService = loanLevelClientDetailsService;
    this.nsdlValidationService = nsdlValidationService;
    this.karzaPanValidationService = karzaPanValidationService;
    this.karzaNameSimilarityService = karzaNameSimilarityService;
    this.dobValidationService = dobValidationService;
    this.clientValidationFunnelStatusRepository = clientValidationFunnelStatusRepository;
    this.validationStepRepository = validationStepRepository;
    this.standalonePanValidationService = standalonePanValidationService;
    this.funnelServiceUtil = funnelServiceUtil;
    this.eventProducerService = eventProducerService;
  }

  public Mono<PanValidationServiceType> determineActiveService(String productCode) {
    return validationFunnelConfigService
        .getValidationFunnelConfig(productCode)
        .flatMap(
            phase2Config -> {

              // Check Phase 2 first
              if (properties.isMasterFlag() && phase2Config.isValidationFunnelFlagEnabled()) {
                return Mono.just(PanValidationServiceType.VALIDATION_FUNNEL);
              }

              // Phase 2 is OFF → check Phase 1
              return standalonePanValidationService
                  .getStandalonePanValidationConfig(productCode)
                  .map(
                      phase1Config -> {
                        if (phase1Config.isPanValidationFeatureFlag()) {
                          return PanValidationServiceType.STANDALONE_NSDL_PAN_VALIDATION;
                        }

                        // Neither active
                        return PanValidationServiceType.NONE;
                      });
            })
        .onErrorResume(
            ex -> {
              log.error(
                  "[VALIDATION_SERVICE] Error while determining active pan validation service type."
                      + " Returning PanValidationServiceType as NONE");
              return Mono.just(PanValidationServiceType.NONE);
            });
  }

  public Mono<Tuple2<Optional<LoanLevelClientDetailsCacheDTO>, Signal<NsdlPanVerificationResult>>>
      runValidationFunnelAtLoanApplicationCreation(
          String productCode, String clientId, String loanApplicationId, LoanLevelClientDetailsCacheDTO clientDetails) {
    Mono<Void> insertFunnelMono =
        Mono.fromRunnable(() -> this.insertFunnelInDB(clientId, productCode));

    return insertFunnelMono // first insert the funnel in the db with INIT status
        .then( Mono.defer(() -> {
              Optional<LoanLevelClientDetailsCacheDTO> optionalClientDetails =
                  Optional.of(clientDetails);

              Mono<NsdlPanVerificationRequest> panRequest =
                  PanValidationUtil.buildPanVerificationRequest(clientDetails);

              // NSDL Pan Validation
              return nsdlValidationService
                  .init(
                      panRequest,
                      clientDetails.getProductCode(),
                      clientDetails.getClientId().toString(),
                      loanApplicationId)
                  .materialize()
                  .flatMap(
                      panSignal -> {
                        // NSDL failed or errored: return NSDL failure signal
                        if (panSignal.isOnError()) {
                          return Mono.just(Tuples.of(optionalClientDetails, panSignal));
                        }

                        NsdlPanVerificationResult result = panSignal.get();
                        assert result != null;

                        // check NSDL result and take the decision to proceed with karza
                        if (checkNSDLResult(result)) {
                          publishEventKafkaAsync(
                              () ->
                                  eventProducerService.publishEvent(
                                      new EventContext(
                                          Event.NSDL_PAN_SOFT_REJECTED,
                                          loanApplicationId,
                                          clientId),
                                      null,
                                      null));
                          // NSDL passed, proceed to Karza checks
                          return runKarzaFunnel(
                              clientDetails, productCode, clientId, panSignal, loanApplicationId);
                        }

                        // All other statuses keep the "no Karza" flow, but get distinct events.
                        switch (result
                            .getPanVerificationResults()
                            .get(0)
                            .getEvaluationResult()
                            .getStatus()) {
                          case APPROVED:
                            publishEventKafkaAsync(
                                () ->
                                    eventProducerService.publishEvent(
                                        new EventContext(
                                            Event.NSDL_PAN_APPROVED, loanApplicationId, clientId),
                                        null,
                                        null));
                            break;
                          case REJECTED:
                            publishEventKafkaAsync(
                                () ->
                                    eventProducerService.publishEvent(
                                        new EventContext(
                                            Event.NSDL_PAN_REJECTED, loanApplicationId, clientId),
                                        null,
                                        null));
                            break;
                          case MANUAL_REVIEW:
                            publishEventKafkaAsync(
                                () ->
                                    eventProducerService.publishEvent(
                                        new EventContext(
                                            Event.NSDL_PAN_MANUAL_REVIEW,
                                            loanApplicationId,
                                            clientId),
                                        null,
                                        null));
                            break;
                          default:
                            break;
                        }

                        return Mono.just(Tuples.of(optionalClientDetails, panSignal));
                      });
            }));
  }

  public void runDOBWaterfallValidationFunnelAtAadharXMLUpload(
      String base64Xml, String clientId, String productCode, String loanApplicationId) {
    validationFunnelConfigService
        .getValidationFunnelConfig(productCode)
        .flatMap(
            config -> {
              boolean funnelFeatureFlag =
                  Objects.nonNull(config) && config.isValidationFunnelFlagEnabled();
              boolean isActive = properties.isMasterFlag() && funnelFeatureFlag;
              boolean isDOBFunnelActive = config.isDobWaterfallFunnelFeatureFlagEnabled();

              if (isActive && isDOBFunnelActive) {
                log.info(
                    "[DOB_WATERFALL][ASYNC_TRIGGER][INIT] Triggering DOB"
                        + " Waterfall funnel for clientId={}, productCode={}",
                    clientId,
                    productCode);
                publishEventKafkaAsync(
                    () ->
                        eventProducerService.publishEvent(
                            new EventContext(
                                Event.DOB_WATERFALL_VALIDATION_FUNNEL, loanApplicationId, clientId),
                            null,
                            null));

                return runDOBWaterfallValidationFunnelAtAadharXMLUploadThroughValidationFunnel(
                    base64Xml, clientId, productCode, loanApplicationId);
              } else if (isActive && !isDOBFunnelActive) {
                publishEventKafkaAsync(
                    () ->
                        eventProducerService.publishEvent(
                            new EventContext(
                                Event.STANDALONE_DOB_WATERFALL, loanApplicationId, clientId),
                            null,
                            null));
                return dobValidationService
                    .updateDatabaseForDobWaterfall(clientId, productCode)
                    .then(
                        dobValidationService.triggerStandaloneDOBWaterfallValidationService(
                            base64Xml, clientId, loanApplicationId, productCode));
              } else {
                log.info(
                    "[STANDALONE_DOB_WATERFALL][ASYNC_TRIGGER][INIT] Triggering DOB"
                        + " Waterfall funnel for clientId={}, productCode={}",
                    clientId,
                    productCode);
                publishEventKafkaAsync(
                    () ->
                        eventProducerService.publishEvent(
                            new EventContext(
                                Event.STANDALONE_DOB_WATERFALL, loanApplicationId, clientId),
                            null,
                            null));
                return dobValidationService.triggerStandaloneDOBWaterfallValidationService(
                    base64Xml, clientId, loanApplicationId, productCode);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            v ->
                log.info(
                    "[DOB_FUNNEL][ASYNC_TRIGGER][SUCCESS] DOB waterfall setup complete for"
                        + " clientId={}",
                    clientId),
            e ->
                log.error(
                    "[DOB_FUNNEL][ASYNC_TRIGGER][ERROR] Failed to setup DOB waterfall for"
                        + " clientId={}",
                    clientId,
                    e));
  }

  public Mono<Void> runDOBWaterfallValidationFunnelAtAadharXMLUploadThroughValidationFunnel(
      String base64Xml, String clientId, String productCode, String loanApplicationId) {
    return funnelServiceUtil
        .readValidationServiceFinalStatus(productCode, clientId)
        .flatMap(
            validationServiceKYCCallbackResponse -> {
              if (validationServiceKYCCallbackResponse
                      .getFinalStatus()
                      .equals(ClientValidationFunnelStatus.FinalStatus.PASS)
                  || validationServiceKYCCallbackResponse
                      .getFinalStatus()
                      .equals(ClientValidationFunnelStatus.FinalStatus.REJECT)
                  || validationServiceKYCCallbackResponse
                      .getFinalStatus()
                      .equals(ClientValidationFunnelStatus.FinalStatus.MANUAL_REVIEW)) {
                log.info(
                    "[DOB_FUNNEL] Funnel status is {}. Not running the DOB waterfall for clientId="
                        + " {} productCode= {}",
                    validationServiceKYCCallbackResponse.getFinalStatus(),
                    clientId,
                    productCode);
                return Mono.empty();
              }

              return upsertDOBWaterfallStep(
                      clientId,
                      productCode,
                      ClientValidationFunnelStatus.StepStatus.INIT,
                      ClientValidationFunnelStatus.ServiceStatus.INIT)
                  .flatMap(
                      validationStep -> {
                        return dobValidationService.triggerDOBWaterfallValidationService(
                            base64Xml, clientId, loanApplicationId, productCode);
                      });
            })
        .doOnError(
            e ->
                log.error(
                    "[VALIDATION][ERROR] Error while reading validation service final status for"
                        + " clientId={}",
                    clientId))
        .then();
  }

  private Mono<ValidationStepEntity> upsertDOBWaterfallStep(
      String clientId,
      String productCode,
      ClientValidationFunnelStatus.StepStatus status,
      ClientValidationFunnelStatus.ServiceStatus serviceStatus) {

    return validationStepRepository.upsertStep(
        clientId,
        productCode,
        ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL.name(),
        ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL.name(),
        status.name(),
        serviceStatus.name(),
        null,
        null);
  }

  private Mono<Tuple2<Optional<LoanLevelClientDetailsCacheDTO>, Signal<NsdlPanVerificationResult>>>
      runKarzaFunnel(
          LoanLevelClientDetailsCacheDTO clientDetails,
          String productCode,
          String clientId,
          Signal<NsdlPanVerificationResult> nsdlSuccessSignal,
          String loanApplicationId) {
    Optional<LoanLevelClientDetailsCacheDTO> optionalClientDetails = Optional.of(clientDetails);

    // Prepare Karza PAN Request
    KarzaPanAuthenticateRequest karzaPanRequest =
        KarzaPanAuthenticateRequest.builder()
            .pan(clientDetails.getPanNumber())
            .clientData(
                KarzaPanAuthenticateRequest.ClientData.builder()
                    .caseId(UUID.randomUUID().toString())
                    .build())
            .consent("Y")
            .build();

    // Karza Pan Authentication
    return karzaPanValidationService
        .init(Mono.just(karzaPanRequest), productCode, clientId, loanApplicationId)
        .materialize()
        .flatMap(
            karzaPanAuthSignal -> {
              if (karzaPanAuthSignal.isOnError()) {
                Signal<NsdlPanVerificationResult> errorSignal =
                    Signal.error(Objects.requireNonNull(karzaPanAuthSignal.getThrowable()));
                return Mono.just(Tuples.of(optionalClientDetails, errorSignal));
              }

              KarzaPanAuthenticateResult karzaPanAuthenticateResult = karzaPanAuthSignal.get();

              // check Karza pan authentication result and take the decision to proceed with karza
              // name similarity
              if (checkKarzaPanAuthenticateResult(karzaPanAuthenticateResult)) {
                publishEventKafkaAsync(
                    () ->
                        eventProducerService.publishEvent(
                            new EventContext(Event.KARZA_PAN_APPROVED, loanApplicationId, clientId),
                            null,
                            null));
                // Karza pan authentication passed: proceed to Karza Name Similarity
                assert karzaPanAuthenticateResult != null;
                return runKarzaNameSimilarity(
                    clientDetails,
                    productCode,
                    clientId,
                    karzaPanAuthenticateResult,
                    nsdlSuccessSignal,
                    loanApplicationId);
              } else {
                publishEventKafkaAsync(
                    () ->
                        eventProducerService.publishEvent(
                            new EventContext(Event.KARZA_PAN_REJECTED, loanApplicationId, clientId),
                            null,
                            null));
              }

              return Mono.just(Tuples.of(optionalClientDetails, nsdlSuccessSignal));
            });
  }

  /** Helper function for the final Karza Name Similarity check. */
  private Mono<Tuple2<Optional<LoanLevelClientDetailsCacheDTO>, Signal<NsdlPanVerificationResult>>>
      runKarzaNameSimilarity(
          LoanLevelClientDetailsCacheDTO clientDetails,
          String productCode,
          String leadId,
          KarzaPanAuthenticateResult karzaPanAuthenticateResult,
          Signal<NsdlPanVerificationResult> nsdlSuccessSignal,
          String loanApplicationId) {
    Optional<LoanLevelClientDetailsCacheDTO> optionalClientDetails = Optional.of(clientDetails);

    String nameFromClientRequest =
        buildFullName(
            clientDetails.getFirstName(),
            clientDetails.getMiddleName(),
            clientDetails.getLastName());

    assert karzaPanAuthenticateResult != null;
    String nameFromKarzaPanAuthenticateResponse =
        karzaPanAuthenticateResult.getVendorResponse().getResult().getName();

    return karzaNameSimilarityService
        .getKarzaNameSimilarityConfig(productCode)
        .flatMap(
            config -> {
              KarzaNameSimilarityRequest karzaNameSimilarityRequest =
                  KarzaNameSimilarityRequest.builder()
                      .name1(nameFromClientRequest)
                      .name2(nameFromKarzaPanAuthenticateResponse)
                      .type(KarzaNameSimilarityRequest.Type.individual)
                      .preset("s")
                      .allowPartialMatch(config.isAllowPartialMatch())
                      .suppressReorderPenalty(config.isSuppressReorderPenalty())
                      .clientData(
                          KarzaNameSimilarityRequest.ClientData.builder()
                              .caseId(UUID.randomUUID().toString())
                              .build())
                      .build();

              // Karza Name Similarity
              return karzaNameSimilarityService
                  .init(
                      Mono.just(karzaNameSimilarityRequest), productCode, leadId, loanApplicationId)
                  .materialize()
                  .flatMap(
                      karzaNameSimilarityResultSignal -> {
                        if (karzaNameSimilarityResultSignal.isOnError()) {
                          Signal<NsdlPanVerificationResult> errorSignal =
                              Signal.error(
                                  Objects.requireNonNull(
                                      karzaNameSimilarityResultSignal.getThrowable()));
                          return Mono.just(Tuples.of(optionalClientDetails, errorSignal));
                        }

                        return Mono.just(Tuples.of(optionalClientDetails, nsdlSuccessSignal));
                      });
            });
  }

  public void insertFunnelInDB(String clientId, String productCode) {
    List<ClientValidationFunnelStatus.ValidationStep> steps =
        List.of(
            new ClientValidationFunnelStatus.ValidationStep(
                ClientValidationFunnelStatus.StepName.PAN_VALIDATION,
                ClientValidationFunnelStatus.Vendor.NSDL,
                null,
                ClientValidationFunnelStatus.StepStatus.INIT,
                ClientValidationFunnelStatus.ServiceStatus.INIT),
            new ClientValidationFunnelStatus.ValidationStep(
                ClientValidationFunnelStatus.StepName.PAN_VALIDATION,
                ClientValidationFunnelStatus.Vendor.KARZA,
                null,
                ClientValidationFunnelStatus.StepStatus.INIT,
                ClientValidationFunnelStatus.ServiceStatus.INIT),
            new ClientValidationFunnelStatus.ValidationStep(
                ClientValidationFunnelStatus.StepName.NAME_SIMILARITY,
                ClientValidationFunnelStatus.Vendor.KARZA,
                null,
                ClientValidationFunnelStatus.StepStatus.INIT,
                ClientValidationFunnelStatus.ServiceStatus.INIT),
            new ClientValidationFunnelStatus.ValidationStep(
                ClientValidationFunnelStatus.StepName.IN_HOUSE_DOB_WATERFALL,
                ClientValidationFunnelStatus.Vendor.IN_HOUSE_DOB_WATERFALL,
                null,
                ClientValidationFunnelStatus.StepStatus.INIT,
                ClientValidationFunnelStatus.ServiceStatus.INIT));

    ClientValidationFunnelStatus funnel = new ClientValidationFunnelStatus();
    funnel.setClientId(clientId);
    funnel.setFinalStatus(ClientValidationFunnelStatus.FinalStatus.INIT);
    funnel.setSteps(steps);

    this.upsertFunnelWithSteps(funnel, productCode)
        .doOnError(
            e ->
                log.error(
                    "Failed to insert funnel for clientId={} error = {}", clientId, e.getMessage()))
        .subscribe();
  }

  public Mono<Void> upsertFunnelWithSteps(ClientValidationFunnelStatus funnel, String productCode) {
    // Upsert funnel with INIT finalStatus
    Mono<ClientValidationFunnelStatusEntity> saveFunnelMono =
        clientValidationFunnelStatusRepository
            .upsertFunnelStatus(
                funnel.getClientId(),
                productCode,
                ClientValidationFunnelStatus.FinalStatus.INIT.name())
            .doOnSuccess(
                entity ->
                    log.debug(
                        "Upserted client_validation_funnel_status for clientId={}",
                        entity.getClientId()));

    List<ValidationStepEntity> stepEntities =
        funnel.getSteps().stream()
            .map(
                step ->
                    ValidationStepEntity.builder()
                        .clientId(funnel.getClientId())
                        .stepName(step.getStepName().name())
                        .vendor(step.getVendor().name())
                        .status(ClientValidationFunnelStatus.StepStatus.INIT.name())
                        .serviceStatus(ClientValidationFunnelStatus.ServiceStatus.INIT.name())
                        .request(null)
                        .response(null)
                        .build())
            .toList();

    Flux<ValidationStepEntity> saveStepsFlux =
        Flux.fromIterable(stepEntities)
            .flatMap(
                step ->
                    validationStepRepository.upsertStep(
                        step.getClientId(),
                        productCode,
                        step.getStepName(),
                        step.getVendor(),
                        step.getStatus(),
                        step.getServiceStatus(),
                        step.getRequest(),
                        step.getResponse()));

    // 1. Wait for step deletion to complete.
    // 2. Then, perform the funnel upsert.
    // 3. Then, perform the steps upsert.
    //    return deleteStepsMono.then(saveFunnelMono).thenMany(saveStepsFlux).then();
    return saveFunnelMono.thenMany(saveStepsFlux).then();
  }

  public boolean checkNSDLResult(NsdlPanVerificationResult results) {
    return OpvResponseCode.SUCCESS.getCode().equals(results.getResponseCode())
        && Objects.nonNull(results.getPanVerificationResults())
        && !results.getPanVerificationResults().isEmpty()
        && results.getPanVerificationResults().get(0).getEvaluationResult().getStatus()
            == NsdlPanVerificationResult.Status.SOFT_REJECTED;
  }

  private boolean checkKarzaPanAuthenticateResult(KarzaPanAuthenticateResult result) {
    return KarzaInternalStatusEnum.CODE_101
        .getCode()
        .equals(String.valueOf(result.getVendorResponse().getStatusCode()));
  }

  public static String buildFullName(String... parts) {
    return Arrays.stream(parts)
        .filter(StringUtils::isNotBlank) // skip null/empty parts
        .collect(Collectors.joining(StringUtils.SPACE));
  }

  public Mono<ClientValidationFunnelStatusEntity>
      findLatestClientValidationFunnelStatusByClientIdAndProductCode(
          String clientId, String productCode) {
    return clientValidationFunnelStatusRepository.findLatestByClientIdAndProductCode(
        clientId, productCode);
  }

  private void publishEventKafkaAsync(Runnable eventTask) {
    Mono.fromRunnable(eventTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error while publishing the event", error));
  }
}
