package com.trillionloans.los.service.impl;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.AUTH_SUCCESS;
import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;
import static com.trillionloans.los.constant.StringConstants.DATE_TIME_FORMAT;
import static com.trillionloans.los.constant.StringConstants.EMPTY_MANDATE_REGISTRATION_CONFIG;
import static com.trillionloans.los.constant.StringConstants.ERROR_FETCHING_LIVE_BANK;
import static com.trillionloans.los.constant.StringConstants.ERROR_FETCHING_LIVE_BANK_LOG;
import static com.trillionloans.los.constant.StringConstants.FAIL;
import static com.trillionloans.los.constant.StringConstants.FAILED_TO_SAVE_MANDATE_DETAILS;
import static com.trillionloans.los.constant.StringConstants.MANDATE_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.MANDATE_DATA_MISMATCH;
import static com.trillionloans.los.constant.StringConstants.MANDATE_DIGIO_CALLBACK;
import static com.trillionloans.los.constant.StringConstants.MANDATE_PARTNER_CALLBACK;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_DETAILS_LOG_HEADER;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_LOG_HEADER;
import static com.trillionloans.los.constant.StringConstants.PARSING_ERROR;
import static com.trillionloans.los.constant.StringConstants.REDIS_OPS;
import static com.trillionloans.los.constant.StringConstants.SAVINGS;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.constant.StringConstants.UNSUPPORTED_VENDOR;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.trillionloans.los.api.partner.DigioApi;
import com.trillionloans.los.api.partner.PartnerApi;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.model.dto.DigioMandateWebhookDTO;
import com.trillionloans.los.model.dto.MandateRegistrationConfigDTO;
import com.trillionloans.los.model.dto.internal.LoanFunnelDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.entity.MandateRegistrationDetailsEntity;
import com.trillionloans.los.model.request.NachMandateRequest;
import com.trillionloans.los.model.response.MandateRegistrationDetailsResponse;
import com.trillionloans.los.model.response.digio.MandateDetailsDigioResponse;
import com.trillionloans.los.model.response.digio.MandateLiveBanksDigioResponse;
import com.trillionloans.los.repository.CallbackRepository;
import com.trillionloans.los.repository.MandateRegistrationDetailsRepository;
import com.trillionloans.los.service.LoanApplicationService;
import com.trillionloans.los.service.MandateDetailsService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.RedisCacheService;
import com.trillionloans.los.service.producers.KafkaFunnelLoggingService;
import io.r2dbc.postgresql.codec.Json;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// This service will contain the shared functionality between DigioFacadeServiceImpl and
// MandateRegistrationService

@Service
@Slf4j
public class MandateDetailsServiceImpl implements MandateDetailsService {
  // Move the fetchMandateRegistration logic from MandateRegistrationService here
  // This service will contain the shared functionality between DigioFacadeServiceImpl and
  // MandateRegistrationService
  private final Gson gson;
  private final PartnerApi partnerApi;
  private final CallbackRepository callbackRepository;
  private final ProductConfigMasterService productConfigMasterService;
  private final MandateRegistrationDetailsRepository mandateRegistrationDetailsRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DateTimeFormatter dateTimeFormatter;
  private final DateTimeFormatter dateFormatter;
  private final LoanApplicationService loanApplicationService;
  private final DigioApi digioApi;
  private final RedisCacheService redisCacheService;
  private final Environment env;
  private final KafkaFunnelLoggingService kafkaFunnelLoggingService;
  private static final String REDIS_KEY = "DIGIO_LIVE_BANKS";

  public MandateDetailsServiceImpl(
      PartnerApi partnerApi,
      CallbackRepository callbackRepository,
      Gson gson,
      ProductConfigMasterService productConfigMasterService,
      MandateRegistrationDetailsRepository mandateRegistrationDetailsRepository,
      LoanApplicationService loanApplicationService,
      DigioApi digioApi,
      RedisCacheService redisCacheService,
      Environment env,
      KafkaFunnelLoggingService kafkaFunnelLoggingService) {
    this.gson = gson;
    this.partnerApi = partnerApi;
    this.callbackRepository = callbackRepository;
    this.productConfigMasterService = productConfigMasterService;
    this.mandateRegistrationDetailsRepository = mandateRegistrationDetailsRepository;
    this.dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    this.dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
    this.loanApplicationService = loanApplicationService;
    this.digioApi = digioApi;
    this.redisCacheService = redisCacheService;
    this.env = env;
    this.kafkaFunnelLoggingService = kafkaFunnelLoggingService;
  }

  /**
   * Processes Digio callback with mandate registration updates. This method handles webhooks that
   * require updating the mandate registration status in the system before proceeding with partner
   * integration. It is typically invoked for successful authentication events (mndt.authsuccess)
   * where the mandate details haven't been previously updated in the LOS system. The method
   * performs a two-step process:
   *
   * <ol>
   *   <li>Fetches and updates mandate registration details from Digio to sync the latest status
   *   <li>Proceeds with standard partner callback registration after successful update
   * </ol>
   *
   * <p>This ensures that the system has the most current mandate information before notifying
   * partners about the webhook event.
   *
   * @param digioMandateWebhook The webhook payload from Digio
   * @param mandateRegistrationDetails The existing mandate registration entity from database
   * @param productCode The product code for configuration and routing
   * @param mandateRegistrationProductFlow The flow configuration for processing
   * @return A Mono containing the result of the complete processing operation
   * @throws BaseException when mandate registration fetch or update operations fail
   * @throws ClientSideException when partner integration fails after successful update
   */
  public Mono<Object> processDigioCallbackWithUpdatingM2p(
      String mandateId,
      DigioMandateWebhookDTO digioMandateWebhook,
      MandateRegistrationDetailsEntity mandateRegistrationDetails,
      String productCode,
      ProductControl.Flow mandateRegistrationProductFlow,
      String partnerCode) {

    log.info("[{}] Inside processDigioCallbackWithUpdatingM2p", MANDATE_CALLBACK_IDENTIFIER);

    return processFetchMandateRegistration(
            mandateRegistrationDetails.getClientId(),
            mandateRegistrationDetails.getLoanId(),
            mandateRegistrationDetails.getMandateId(),
            productCode)
        .flatMap(
            response -> {
              log.info(
                  "[{}] successfully completed fetchMandateRegistration process",
                  MANDATE_CALLBACK_IDENTIFIER);
              return registerMandateCallbackToPartner(
                  mandateId,
                  digioMandateWebhook,
                  mandateRegistrationDetails,
                  productCode,
                  mandateRegistrationProductFlow,
                  partnerCode);
            })
        .doOnError(
            error -> {
              log.error(
                  "[{}] Error processing Digio callback with M2P update: {}",
                  MANDATE_CALLBACK_IDENTIFIER,
                  error.getMessage());
              CallbackLogEntity callback =
                  CallbackLogEntity.builder()
                      .type(MANDATE_DIGIO_CALLBACK)
                      .request(Json.of(gson.toJson(digioMandateWebhook)))
                      .referenceId(digioMandateWebhook.getId())
                      .uri(mandateRegistrationProductFlow.getPartnerUri())
                      .isRetry(mandateRegistrationProductFlow.getRetryCount() > 0)
                      .createdAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
                      .productCode(productCode)
                      .build();
              setErrorDataInCallbackEntity(callback, error);
              callbackRepository.save(callback).subscribe();
            });
  }

  /**
   * Registers mandate callback with partner systems. This method handles the integration with
   * partner systems by forwarding the Digio webhook to the appropriate partner endpoint. It
   * performs the following operations:
   *
   * <ol>
   *   <li>Creates callback log entities for audit trail and monitoring
   *   <li>Calls the partner API with the webhook payload and configuration
   *   <li>Saves the partner response for future reference and debugging
   *   <li>Logs the Digio callback in the audit trail
   *   <li>Handles errors and saves error details for troubleshooting
   * </ol>
   *
   * <p>The method ensures comprehensive logging of all interactions between the system and external
   * partners for compliance and debugging purposes.
   *
   * @param digioMandateWebhook The webhook payload received from Digio
   * @param productCode The product code for configuration and routing
   * @param mandateRegistrationFlowData The flow configuration containing partner URI, call method,
   *     retry settings, and other parameters
   * @return A Mono containing the final callback log entity after all operations complete
   * @throws ClientSideException when partner API calls fail due to client-side issues
   * @throws ServerErrorException when partner API calls fail due to server-side issues
   * @throws BaseException when database operations fail during callback logging
   */
  private Mono<Object> registerMandateCallbackToPartner(
      String mandateId,
      DigioMandateWebhookDTO digioMandateWebhook,
      MandateRegistrationDetailsEntity mandateRegistrationDetails,
      String productCode,
      ProductControl.Flow mandateRegistrationFlowData,
      String partnerCode) {

    log.info("[{}] Inside processDigioCallbackWithoutUpdatingM2p", MANDATE_CALLBACK_IDENTIFIER);

    CallbackLogEntity digioMandateCallback =
        CallbackLogEntity.builder()
            .type(MANDATE_DIGIO_CALLBACK)
            .request(Json.of(gson.toJson(digioMandateWebhook)))
            .referenceId(mandateId)
            .uri(mandateRegistrationFlowData.getPartnerUri())
            .isRetry(mandateRegistrationFlowData.getRetryCount() > 0)
            .productCode(productCode)
            .createdAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
            .build();

    CallbackLogEntity partnerMandateCallback =
        CallbackLogEntity.builder()
            .type(MANDATE_PARTNER_CALLBACK)
            .request(Json.of(gson.toJson(digioMandateWebhook)))
            .referenceId(mandateId)
            .uri(mandateRegistrationFlowData.getPartnerUri())
            .isRetry(mandateRegistrationFlowData.getRetryCount() > 0)
            .productCode(productCode)
            .createdAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
            .build();

    return partnerApi
        .registerPartnerCallback(
            digioMandateWebhook,
            mandateRegistrationFlowData.getPartnerUri(),
            mandateRegistrationFlowData.getCallMethod(),
            partnerCode,
            mandateRegistrationFlowData.getRetryCount(),
            MANDATE_CALLBACK_IDENTIFIER)
        .flatMap(
            partnerCallbackResponse -> {
              log.info("[{}] Partner callback response received", MANDATE_CALLBACK_IDENTIFIER);
              partnerMandateCallback.setResponse(Json.of(gson.toJson(partnerCallbackResponse)));
              return callbackRepository
                  .save(partnerMandateCallback)
                  .doOnSuccess(
                      data ->
                          log.info(
                              "[{}] Partner callback saved successfully",
                              MANDATE_CALLBACK_IDENTIFIER));
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] Error in partner callback: {}",
                  MANDATE_CALLBACK_IDENTIFIER,
                  error.getMessage());
              setErrorDataInCallbackEntity(partnerMandateCallback, error);
              return callbackRepository
                  .save(partnerMandateCallback)
                  .flatMap(data -> Mono.error(error));
            })
        .flatMap(
            savedPartnerCallback ->
                callbackRepository
                    .save(digioMandateCallback)
                    .doOnSuccess(
                        data ->
                            log.info(
                                "[{}] Digio callback saved successfully",
                                MANDATE_CALLBACK_IDENTIFIER))
                    .onErrorResume(
                        error -> {
                          log.error(
                              "[{}] Error saving digio mandate callback of mandateId: {}, errror:"
                                  + " {}",
                              mandateRegistrationDetails.getMandateId(),
                              MANDATE_CALLBACK_IDENTIFIER,
                              error.getMessage());
                          setErrorDataInCallbackEntity(digioMandateCallback, error);
                          return callbackRepository
                              .save(digioMandateCallback)
                              .flatMap(data -> Mono.error(error));
                        }));
  }

  @Override
  public void setErrorDataInCallbackEntity(CallbackLogEntity callback, Throwable error) {
    if (error instanceof BaseException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    } else if (error instanceof ClientSideException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    } else if (error instanceof ForbiddenException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    } else if (error instanceof NotFoundException exception) {
      callback.setUri(exception.getUrl());
      callback.setException(exception.toString());
    } else if (error instanceof ServerErrorException exception) {
      callback.setUri(exception.getUrl());
      callback.setResponse(
          exception.getClientResponse() == null
              ? null
              : Json.of(gson.toJson(exception.getClientResponse())));
      callback.setException(exception.toString());
    } else if (error instanceof Exception exception) {
      callback.setException(exception.toString());
    }
  }

  @Override
  public Mono<MandateRegistrationDetailsResponse> processFetchMandateRegistration(
      String leadId, String loanId, String mandateId, String productCode) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              // Extract the mandate registration flow config from product config data
              ProductControl.Flow mandateRegistrationflowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), MANDATE_REGISTRATION_IDENTIFIER);

              if (Objects.isNull(mandateRegistrationflowData)) {
                log.error(
                    "[{}] No product config data found for product code: {}",
                    MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                    productCode);
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }

              log.info(
                  "[{}] Product config data fetched successfully for product code: {}. FlowData"
                      + " Identifier: {} is taken into account",
                  MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                  productCode,
                  mandateRegistrationflowData.getIdentifier());

              // Map generic flow conditions to strongly typed DTO for mandate registration config
              MandateRegistrationConfigDTO mandateRegistrationConfig =
                  objectMapper.convertValue(
                      mandateRegistrationflowData.getConditions(),
                      MandateRegistrationConfigDTO.class);

              if (mandateRegistrationConfig == null) {
                log.error(
                    "[{}] mandateRegistrationConfig is null or incomplete for loan: {}",
                    MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                    loanId);
                kafkaFunnelLoggingService.logMandateRegistrationAsync(
                    leadId,
                    loanId,
                    LoanFunnelDTO.SubStage.MRS_PRODUCT_CONFIG,
                    FAIL,
                    null,
                    EMPTY_MANDATE_REGISTRATION_CONFIG);
                return Mono.error(new NotFoundException(PARSING_ERROR));
              }
              kafkaFunnelLoggingService.logMandateRegistrationAsync(
                  leadId, loanId, LoanFunnelDTO.SubStage.MRS_PRODUCT_CONFIG, SUCCESS, null, null);

              // Proceed to process the mandate registration with extracted flow data
              return fetchMandateRegistrationUsingProductConfig(
                  leadId, loanId, mandateId, mandateRegistrationConfig, productCode);
            });
  }

  private Mono<MandateRegistrationDetailsResponse> fetchMandateRegistrationUsingProductConfig(
      String leadId,
      String loanId,
      String mandateId,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      String productCode) {
    return mandateRegistrationDetailsRepository
        .findByClientIdAndLoanIdAndMandateId(leadId, loanId, mandateId)
        .flatMap(
            mandateRegistrationDetailsEntity -> {
              log.info(
                  "[{}] Mandate registration details found from DB for client-id: {}, loan-id: {},"
                      + " mandate-id: {}, state: {}",
                  MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                  leadId,
                  loanId,
                  mandateId,
                  mandateRegistrationDetailsEntity.getState());
              // calling vendor's api to re-confirm the mandate state
              log.info(
                  "[{}] Fetching mandate registration details from vendor for client-id: {},"
                      + " loan-id: {}, mandate-id: {} to re-confirm the details from the vendor",
                  MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                  leadId,
                  loanId,
                  mandateId);
              kafkaFunnelLoggingService.logMandateRegistrationAsync(
                  leadId, loanId, LoanFunnelDTO.SubStage.MRS_DETAILS, SUCCESS, null, null);
              return switch (mandateRegistrationConfig.getVendorName()) {
                case DIGIO ->
                    triggerDigioFlowForMandateDetails(
                        leadId,
                        loanId,
                        mandateId,
                        mandateRegistrationConfig,
                        mandateRegistrationDetailsEntity,
                        productCode);
                /* future vendor flows can be added here
                e.g. case OTHER_VENDOR -> triggerOtherVendorFlow(...)
                */
                default -> unsupportedVendorResponse(loanId, mandateRegistrationConfig);
              };
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "[{}] No mandate registration details found for client-id: {}, loan-id: {},"
                          + " mandate-id: {} in db:mandate_registration_details",
                      MANDATE_REGISTRATION_LOG_HEADER,
                      leadId,
                      loanId,
                      mandateId);
                  kafkaFunnelLoggingService.logMandateRegistrationAsync(
                      leadId,
                      loanId,
                      LoanFunnelDTO.SubStage.MRS_DETAILS,
                      FAIL,
                      null,
                      "Mandate details not found in database for the requested loan");
                  return Mono.error(
                      new BaseException(
                          "Mandate details not found in database",
                          "No mandate registration details found for client-id: "
                              + leadId
                              + ", loan-id: "
                              + loanId
                              + ", mandate-id: "
                              + mandateId,
                          HttpStatus.NOT_FOUND));
                }));
  }

  private Mono<MandateRegistrationDetailsResponse> triggerDigioFlowForMandateDetails(
      String leadId,
      String loanId,
      String mandateId,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      MandateRegistrationDetailsEntity mandateRegistrationDetailsEntity,
      String productCode) {
    return fetchMandateRegistrationFromDigio(mandateId, loanId, leadId)
        .flatMap(
            mandateDetailsDigioResponse -> {
              kafkaFunnelLoggingService.logMandateRegistrationAsync(
                  leadId, loanId, LoanFunnelDTO.SubStage.MRS_DIGIO_STATUS, SUCCESS, null, null);
              if (mandateRegistrationDetailsEntity.getId() == null
                  && loanId.equals(
                      mandateDetailsDigioResponse.getMandateDetails().getCustomerRefNumber())) {
                MandateRegistrationDetailsEntity freshMandateRegistrationDetails =
                    prepareMandateRegisterDetails(
                        leadId, loanId, mandateRegistrationConfig, mandateDetailsDigioResponse);
                log.info(
                    "[{}] Saving mandate registration details in db for"
                        + " client-id: {}, loan-id: {}, mandate-id: {}",
                    MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                    leadId,
                    loanId,
                    mandateId);
                return mandateRegistrationDetailsRepository
                    .save(freshMandateRegistrationDetails)
                    .flatMap(
                        mandateRegistrationUpdatedDetails -> {
                          log.info(
                              "[{}] Mandate registration details saved"
                                  + " successfully for client-id: {}, loan-id:"
                                  + " {}, mandate-id: {}",
                              MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                              leadId,
                              loanId,
                              mandateId);
                          kafkaFunnelLoggingService.logMandateRegistrationAsync(
                              leadId,
                              loanId,
                              LoanFunnelDTO.SubStage.MRS_FINAL,
                              SUCCESS,
                              null,
                              null);
                          return Mono.just(
                              prepareMandateRegistrationDetailsResponse(
                                  mandateDetailsDigioResponse));
                        })
                    .onErrorResume(
                        error -> {
                          kafkaFunnelLoggingService.logMandateRegistrationAsync(
                              leadId,
                              loanId,
                              LoanFunnelDTO.SubStage.MRS_DIGIO_STATUS,
                              FAIL,
                              null,
                              "Error saving Mandate Details in Trillion's DB after fetching Mandate"
                                  + " Details from Digio");
                          return errorInUpdatingDatabase(leadId, loanId, mandateId, error);
                        });
              } else if (mandateRegistrationDetailsEntity.getState() != null
                  && !mandateRegistrationDetailsEntity
                      .getState()
                      .equals(mandateDetailsDigioResponse.getState())) {
                if (mandateRegistrationDetailsEntity.getUpdatedAtLos() == null
                    && AUTH_SUCCESS.equalsIgnoreCase(mandateDetailsDigioResponse.getState())) {
                  log.info(
                      "[{}] update mandate registration state from {} to {} for client-id: {},"
                          + " loan-id: {}, mandate-id: {}",
                      MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                      mandateRegistrationDetailsEntity.getState(),
                      mandateDetailsDigioResponse.getState(),
                      mandateRegistrationDetailsEntity.getClientId(),
                      mandateRegistrationDetailsEntity.getLoanId(),
                      mandateId);

                  return loanApplicationService
                      .uploadNachMandateRequest(
                          loanId,
                          prepareM2pMandateRegistration(
                              mandateDetailsDigioResponse,
                              mandateRegistrationConfig,
                              mandateRegistrationDetailsEntity),
                          productCode)
                      .flatMap(
                          m2pMandateResponse -> {
                            log.info(
                                "[{}] Nach mandate request uploaded successfully for client-id: {},"
                                    + " loan-id: {}, mandate-id: {} at M2P",
                                MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                                leadId,
                                loanId,
                                mandateId);
                            kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                leadId,
                                loanId,
                                LoanFunnelDTO.SubStage.MRS_M2P_UPDATE,
                                SUCCESS,
                                null,
                                null);

                            mandateRegistrationDetailsEntity.setState(
                                mandateDetailsDigioResponse.getState());
                            mandateRegistrationDetailsEntity.setVersion(
                                mandateRegistrationDetailsEntity.getVersion() + 1);
                            mandateRegistrationDetailsEntity.setUpdatedAtLos(
                                LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
                            mandateRegistrationDetailsEntity.setUpdatedAt(
                                LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));

                            return mandateRegistrationDetailsRepository
                                .save(mandateRegistrationDetailsEntity)
                                .flatMap(
                                    mandateRegistrationUpdatedDetails -> {
                                      log.info(
                                          "[{}] Mandate registration details updated"
                                              + " successfully for client-id: {}, loan-id:"
                                              + " {}, mandate-id: {}",
                                          MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                                          leadId,
                                          loanId,
                                          mandateId);
                                      kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                          leadId,
                                          loanId,
                                          LoanFunnelDTO.SubStage.MRS_FINAL,
                                          SUCCESS,
                                          null,
                                          null);
                                      return Mono.just(
                                          prepareMandateRegistrationDetailsResponse(
                                              mandateDetailsDigioResponse));
                                    })
                                .onErrorResume(
                                    error ->
                                        errorInUpdatingDatabase(leadId, loanId, mandateId, error));
                          })
                      .onErrorResume(
                          error -> {
                            log.error(
                                "[{}] Error uploading Nach mandate request for client-id: {},"
                                    + " loan-id: {}, mandate-id: {}. Error: {} to M2P",
                                MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                                leadId,
                                loanId,
                                mandateId,
                                error.getMessage());
                            kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                leadId,
                                loanId,
                                LoanFunnelDTO.SubStage.MRS_M2P_UPDATE,
                                FAIL,
                                null,
                                "Fail to upload Nach Mandate details to M2P");
                            return Mono.error(
                                new ClientSideException(
                                    FAILED_TO_SAVE_MANDATE_DETAILS,
                                    error.getMessage(),
                                    HttpStatus.BAD_REQUEST));
                          });
                }

                mandateRegistrationDetailsEntity.setState(mandateDetailsDigioResponse.getState());
                mandateRegistrationDetailsEntity.setVersion(
                    mandateRegistrationDetailsEntity.getVersion() + 1);
                mandateRegistrationDetailsEntity.setUpdatedAt(
                    LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
                return mandateRegistrationDetailsRepository
                    .save(mandateRegistrationDetailsEntity)
                    .flatMap(
                        mandateRegistrationUpdatedDetails -> {
                          log.info(
                              "[{}] Mandate registration details updated"
                                  + " successfully for client-id: {}, loan-id:"
                                  + " {}, mandate-id: {}",
                              MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                              leadId,
                              loanId,
                              mandateId);
                          kafkaFunnelLoggingService.logMandateRegistrationAsync(
                              leadId,
                              loanId,
                              LoanFunnelDTO.SubStage.MRS_FINAL,
                              SUCCESS,
                              null,
                              null);
                          return Mono.just(
                              prepareMandateRegistrationDetailsResponse(
                                  mandateDetailsDigioResponse));
                        })
                    .onErrorResume(
                        error -> errorInUpdatingDatabase(leadId, loanId, mandateId, error));
              } else if (!loanId.equals(
                      mandateDetailsDigioResponse.getMandateDetails().getCustomerRefNumber())
                  || !mandateId.equals(mandateDetailsDigioResponse.getMandateId())) {
                log.error(
                    "[{}] Inconsistency detected between Digio and Trillion data: Digio"
                        + " loanId={}, mandateId={}; Request path clientId={}, loanId={}.",
                    MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                    mandateDetailsDigioResponse.getMandateDetails().getCustomerRefNumber(),
                    mandateDetailsDigioResponse.getMandateId(),
                    leadId,
                    loanId);
                return Mono.error(
                    new ClientSideException(
                        MANDATE_DATA_MISMATCH, MANDATE_DATA_MISMATCH, HttpStatus.BAD_REQUEST));
              }
              log.info(
                  "[{}] Mandate registration details already exist in db for client-id: {},"
                      + " loan-id: {}, mandate-id: {} with state: {}. No update needed",
                  MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                  mandateRegistrationDetailsEntity.getClientId(),
                  mandateRegistrationDetailsEntity.getLoanId(),
                  mandateId,
                  mandateRegistrationDetailsEntity.getState());
              kafkaFunnelLoggingService.logMandateRegistrationAsync(
                  leadId, loanId, LoanFunnelDTO.SubStage.MRS_FINAL, SUCCESS, null, null);
              return Mono.just(
                  prepareMandateRegistrationDetailsResponse(mandateDetailsDigioResponse));
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] Error fetching mandate registration details from Digio for"
                      + " client-id: {}, loan-id: {}, mandate-id: {}. Error: {}",
                  MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                  mandateRegistrationDetailsEntity.getClientId(),
                  mandateRegistrationDetailsEntity.getLoanId(),
                  mandateId,
                  error instanceof ClientSideException clientSideException
                      ? clientSideException.getClientResponse()
                      : error.getMessage());
              kafkaFunnelLoggingService.logMandateRegistrationAsync(
                  leadId,
                  loanId,
                  LoanFunnelDTO.SubStage.MRS_DIGIO_STATUS,
                  FAIL,
                  null,
                  "Error fetching mandate registration details from Digio");
              return Mono.error(
                  new ClientSideException(
                      "Error fetching mandate registration details from Digio",
                      error instanceof ClientSideException clientSideException
                          ? clientSideException.getClientResponse()
                          : error.getMessage(),
                      HttpStatus.BAD_REQUEST));
            });
  }

  private Mono<MandateDetailsDigioResponse> fetchMandateRegistrationFromDigio(
      String mandateId, String loanId, String leadId) {
    return digioApi
        .fetchMandateRegistrationStatus(mandateId, loanId, leadId)
        .flatMap(
            mandateDetailsDigioResponse -> {
              log.info(
                  "[{}] Mandate registration status fetched successfully from Digio for mandate-id:"
                      + " {}",
                  MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
                  mandateId);
              return Mono.just(mandateDetailsDigioResponse);
            });
  }

  private <T> Mono<T> unsupportedVendorResponse(
      String loanId, MandateRegistrationConfigDTO mandateRegistrationConfig) {
    log.error(
        UNSUPPORTED_VENDOR,
        MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
        mandateRegistrationConfig.getVendorName(),
        loanId);
    return Mono.error(
        new BaseException(
            SOMETHING_WENT_WRONG_CONFIG,
            SOMETHING_WENT_WRONG_CONFIG,
            HttpStatus.INTERNAL_SERVER_ERROR));
  }

  private MandateRegistrationDetailsEntity prepareMandateRegisterDetails(
      String leadId,
      String loanId,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      MandateDetailsDigioResponse mandateDetailsDigioResponse) {
    return MandateRegistrationDetailsEntity.builder()
        .mandateId(mandateDetailsDigioResponse.getMandateId())
        .amount(mandateDetailsDigioResponse.getMandateDetails().getMaximumAmount().toString())
        .state(mandateDetailsDigioResponse.getState())
        .firstCollectionDate(
            LocalDateTime.parse(
                mandateDetailsDigioResponse.getMandateDetails().getFirstCollectionDate(),
                dateTimeFormatter))
        .finalCollectionDate(
            LocalDateTime.parse(
                mandateDetailsDigioResponse.getMandateDetails().getFinalCollectionDate(),
                dateTimeFormatter))
        .vendorName(mandateRegistrationConfig.getVendorName().getDisplayName())
        .authMode(mandateRegistrationConfig.getAuthMode().getDisplayName())
        .isRecurring(mandateRegistrationConfig.getIsRecurring())
        .frequencyType(mandateRegistrationConfig.getFrequencyType())
        .clientId(leadId)
        .loanId(loanId)
        .generateAccessToken(mandateRegistrationConfig.getGenerateAccessToken())
        .notifyCustomer(mandateRegistrationConfig.getNotifyCustomer())
        .authMode(mandateRegistrationConfig.getAuthMode().getDisplayName())
        .version(1)
        .createdAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
        .updatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
        .isDeleted(false)
        .build();
  }

  private MandateRegistrationDetailsResponse prepareMandateRegistrationDetailsResponse(
      MandateDetailsDigioResponse mandateDetailsDigioResponse) {
    return MandateRegistrationDetailsResponse.builder()
        .mandateId(mandateDetailsDigioResponse.getMandateId())
        .state(mandateDetailsDigioResponse.getState())
        .type(mandateDetailsDigioResponse.getType())
        .firstCollectionDate(
            mandateDetailsDigioResponse.getMandateDetails().getFirstCollectionDate())
        .finalCollectionDate(
            mandateDetailsDigioResponse.getMandateDetails().getFinalCollectionDate())
        .createdAt(mandateDetailsDigioResponse.getCreatedAt())
        .amount(mandateDetailsDigioResponse.getMandateDetails().getMaximumAmount())
        .authenticationTime(mandateDetailsDigioResponse.getMandateDetails().getAuthenticationTime())
        .frequency(mandateDetailsDigioResponse.getMandateDetails().getFrequency())
        .customerAccountNumber(
            mandateDetailsDigioResponse.getMandateDetails().getCustomerAccountNumber())
        .destinationBankId(mandateDetailsDigioResponse.getMandateDetails().getDestinationBankId())
        .bankDetails(
            MandateRegistrationDetailsResponse.BankDetails.builder()
                .sharedWithBank(mandateDetailsDigioResponse.getBankDetails().getSharedWithBank())
                .bankName(mandateDetailsDigioResponse.getBankDetails().getBankName())
                .state(mandateDetailsDigioResponse.getBankDetails().getState())
                .authenticatedAt(mandateDetailsDigioResponse.getBankDetails().getAuthenticatedAt())
                .build())
        .build();
  }

  public Mono<MandateRegistrationDetailsResponse> errorInUpdatingDatabase(
      String leadId, String loanId, String mandateId, Throwable error) {
    log.error(
        "[{}] Error saving mandate registration details for"
            + " client-id: {}, loan-id: {}, mandate-id: {}."
            + " Error: {}",
        MANDATE_REGISTRATION_DETAILS_LOG_HEADER,
        leadId,
        loanId,
        mandateId,
        error.getMessage());
    return Mono.error(
        new ClientSideException(
            FAILED_TO_SAVE_MANDATE_DETAILS, error.getMessage(), HttpStatus.BAD_REQUEST));
  }

  private NachMandateRequest prepareM2pMandateRegistration(
      MandateDetailsDigioResponse mandateDetailsDigioResponse,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      MandateRegistrationDetailsEntity mandateRegistrationDetailsEntity) {
    return NachMandateRequest.builder()
        .status(mandateDetailsDigioResponse.getState().toUpperCase())
        .umrn(mandateDetailsDigioResponse.getUmrn())
        .bankAccountType(
            SAVINGS.equalsIgnoreCase(
                    mandateDetailsDigioResponse.getMandateDetails().getCustomerAccountType())
                ? "SB"
                : "CA")
        .bankAccountHolderName(mandateDetailsDigioResponse.getMandateDetails().getCustomerName())
        .bankName(mandateDetailsDigioResponse.getMandateDetails().getDestinationBankName())
        .bankAccountNumber(
            mandateDetailsDigioResponse.getMandateDetails().getCustomerAccountNumber())
        .ifsc(mandateDetailsDigioResponse.getMandateDetails().getDestinationBankId())
        .mandateRegistrationRequestedDate(
            LocalDateTime.parse(mandateDetailsDigioResponse.getCreatedAt(), dateTimeFormatter)
                .format(dateFormatter))
        .periodStartDate(
            LocalDateTime.parse(
                    mandateDetailsDigioResponse.getMandateDetails().getFirstCollectionDate(),
                    dateTimeFormatter)
                .format(dateFormatter))
        .periodEndDate(
            LocalDateTime.parse(
                    mandateDetailsDigioResponse.getMandateDetails().getFinalCollectionDate(),
                    dateTimeFormatter)
                .format(dateFormatter))
        .periodUntilCancelled(mandateRegistrationConfig.getPeriodUntilCancelled())
        .debitTypeEnum(mandateRegistrationConfig.getDebitTypeEnum())
        .debitFrequencyEnum(mandateRegistrationDetailsEntity.getFrequencyType().toUpperCase())
        .amount(mandateDetailsDigioResponse.getMandateDetails().getMaximumAmount())
        .externalReferenceNumber(UUID.randomUUID().toString())
        .mode(mandateRegistrationConfig.getMode())
        .build();
  }

  public Flux<MandateLiveBanksDigioResponse> processFetchDigioMandateLiveBanks(String productCode) {
    return redisCacheService
        .getKey(REDIS_KEY)
        .cast(String.class)
        .flatMapMany(
            cachedJson -> {
              log.info("[{}] cache hit for key: {}", REDIS_OPS, REDIS_KEY);
              return deserializeCachedLiveBanks(cachedJson);
            })
        .switchIfEmpty(fetchAndCacheLiveBanks(productCode))
        .onErrorResume(
            error -> {
              log.error(
                  ERROR_FETCHING_LIVE_BANK_LOG,
                  REDIS_OPS,
                  productCode,
                  error instanceof ClientSideException clientSideException
                      ? clientSideException.getClientResponse()
                      : error.getMessage());
              return Flux.error(
                  new ClientSideException(
                      ERROR_FETCHING_LIVE_BANK,
                      error instanceof ClientSideException clientSideException
                          ? clientSideException.getClientResponse()
                          : error.getMessage(),
                      HttpStatus.BAD_REQUEST));
            });
  }

  private Flux<MandateLiveBanksDigioResponse> deserializeCachedLiveBanks(String cachedJson) {
    try {
      Type listType = new TypeToken<List<MandateLiveBanksDigioResponse>>() {}.getType();
      List<MandateLiveBanksDigioResponse> liveBanksList = gson.fromJson(cachedJson, listType);
      return Flux.fromIterable(liveBanksList);
    } catch (Exception e) {
      log.warn("[{}] Failed to deserialize cached live banks data", REDIS_OPS, e);
      return Flux.empty();
    }
  }

  private Flux<MandateLiveBanksDigioResponse> fetchAndCacheLiveBanks(String productCode) {
    return fetchDigioMandateLiveBanksWithoutCache(productCode)
        .collectList()
        .doOnNext(
            liveBanksList -> {
              log.info(
                  "[{}] cache miss for key: {}, caching {} items",
                  REDIS_OPS,
                  REDIS_KEY,
                  liveBanksList.size());

              redisCacheService
                  .putKeyWithTTL(
                      REDIS_KEY,
                      gson.toJson(liveBanksList),
                      Duration.ofHours(
                          Long.parseLong(
                              Objects.requireNonNull(env.getProperty("cache.digio-live-banks")))))
                  .subscribe();
            })
        .flatMapMany(Flux::fromIterable)
        .onErrorResume(
            error -> {
              log.error(
                  ERROR_FETCHING_LIVE_BANK_LOG,
                  REDIS_OPS,
                  productCode,
                  error instanceof ClientSideException clientSideException
                      ? clientSideException.getClientResponse()
                      : error.getMessage());
              return Flux.error(
                  new ClientSideException(
                      ERROR_FETCHING_LIVE_BANK,
                      error instanceof ClientSideException clientSideException
                          ? clientSideException.getClientResponse()
                          : error.getMessage(),
                      HttpStatus.BAD_REQUEST));
            });
  }

  private Flux<MandateLiveBanksDigioResponse> fetchDigioMandateLiveBanksWithoutCache(
      String productCode) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMapMany(
            productControlConfigData -> {
              // Extract the mandate registration flow config from product config data
              ProductControl.Flow mandateRegistrationFlowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), MANDATE_REGISTRATION_IDENTIFIER);

              if (Objects.isNull(mandateRegistrationFlowData)) {
                log.error(
                    "[{}] No product config data found for product code: {}",
                    MANDATE_REGISTRATION_LOG_HEADER,
                    productCode);
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }

              log.info(
                  "[{}] product config data fetched successfully for product code: {}",
                  MANDATE_REGISTRATION_LOG_HEADER,
                  productCode);
              log.info(
                  "[{}]  product config flowData Identifier: {} is taking into account",
                  MANDATE_REGISTRATION_LOG_HEADER,
                  mandateRegistrationFlowData.getIdentifier());

              // Map generic flow conditions to strongly typed DTO for mandate registration config
              MandateRegistrationConfigDTO mandateRegistrationConfig =
                  objectMapper.convertValue(
                      mandateRegistrationFlowData.getConditions(),
                      MandateRegistrationConfigDTO.class);

              if (mandateRegistrationConfig == null) {
                log.error(
                    "[{}] mandateRegistrationConfig is null or incomplete",
                    MANDATE_REGISTRATION_LOG_HEADER);
                return Mono.error(new NotFoundException(PARSING_ERROR));
              }

              // Proceed to process the mandate registration with extracted flow data
              return digioApi.fetchDigioMandateLiveBanks(
                  mandateRegistrationConfig.getAuthMode().getDisplayName());
            })
        .onErrorResume(
            error -> {
              log.error(
                  ERROR_FETCHING_LIVE_BANK_LOG,
                  REDIS_OPS,
                  productCode,
                  error instanceof ClientSideException clientSideException
                      ? clientSideException.getClientResponse()
                      : error.getMessage());
              return Flux.error(
                  new ClientSideException(
                      ERROR_FETCHING_LIVE_BANK,
                      error instanceof ClientSideException clientSideException
                          ? clientSideException.getClientResponse()
                          : error.getMessage(),
                      HttpStatus.BAD_REQUEST));
            });
  }
}
