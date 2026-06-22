package com.trillionloans.los.service.impl;

import static com.trillionloans.los.constant.StringConstants.ACTIVE;
import static com.trillionloans.los.constant.StringConstants.MANDATE_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.MANDATE_DIGIO_CALLBACK;
import static com.trillionloans.los.constant.StringConstants.MANDATE_PARTNER_CALLBACK;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import com.google.gson.Gson;
import com.trillionloans.los.api.partner.PartnerApi;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.model.dto.DigioMandateWebhookDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.entity.MandateRegistrationDetailsEntity;
import com.trillionloans.los.model.response.digio.MandateLiveBanksDigioResponse;
import com.trillionloans.los.repository.CallbackRepository;
import com.trillionloans.los.repository.MandateRegistrationDetailsRepository;
import com.trillionloans.los.service.DigioFacadeService;
import com.trillionloans.los.service.MandateDetailsService;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import io.r2dbc.postgresql.codec.Json;
import java.util.HashMap;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

/**
 * Implementation of DigioFacadeService for handling Digio mandate webhooks. This service acts as a
 * facade to orchestrate the processing of mandate callbacks received from Digio. It manages the
 * complete lifecycle of webhook processing including:
 *
 * <ul>
 *   <li>Receiving and validating webhook payloads from Digio
 *   <li>Looking up existing mandate registration details
 *   <li>Retrieving product configuration and flow data
 *   <li>Routing to appropriate processing methods based on webhook type and mandate state
 *   <li>Integrating with partner systems through callback APIs
 *   <li>Logging all interactions for audit and troubleshooting purposes
 * </ul>
 *
 * <p>The service ensures fast response times to Digio by processing webhooks asynchronously while
 * maintaining distributed tracing context. All operations are logged extensively for monitoring and
 * debugging purposes.
 *
 * <p><b>Note:</b> Do not add MandateRegistrationService dependency here to avoid circular
 * dependency.
 *
 * @author Sofiyan
 * @version 1.0
 * @since 1.0
 */
@Service
@Slf4j
public class DigioFacadeServiceImpl implements DigioFacadeService {

  private final Gson gson;
  private final CallbackRepository callbackRepository;
  private final MandateRegistrationDetailsRepository mandateRegistrationDetailsRepository;
  private final PartnerApi partnerApi;
  private final ProductConfigMasterService productConfigMasterService;
  private final PartnerMasterService partnerMasterService;
  private final MandateDetailsService mandateDetailsService;

  /**
   * Constructs a new DigioFacadeServiceImpl with the required dependencies.
   *
   * @param callbackRepository Repository for managing callback log entities
   * @param gson JSON serialization/deserialization utility
   * @param mandateRegistrationDetailsRepository Repository for mandate registration details
   * @param partnerApi API client for partner integrations
   * @param productConfigMasterService Service for retrieving product configurations
   * @param partnerMasterService Service for managing partner master data
   */
  public DigioFacadeServiceImpl(
      CallbackRepository callbackRepository,
      Gson gson,
      MandateRegistrationDetailsRepository mandateRegistrationDetailsRepository,
      PartnerApi partnerApi,
      ProductConfigMasterService productConfigMasterService,
      PartnerMasterService partnerMasterService,
      MandateDetailsService mandateDetailsService) {
    this.callbackRepository = callbackRepository;
    this.gson = gson;
    this.mandateRegistrationDetailsRepository = mandateRegistrationDetailsRepository;
    this.partnerApi = partnerApi;
    this.productConfigMasterService = productConfigMasterService;
    this.partnerMasterService = partnerMasterService;
    this.mandateDetailsService = mandateDetailsService;
  }

  /**
   * Registers and processes mandate webhooks received from Digio. This method serves as the main
   * entry point for all Digio webhook processing. It immediately acknowledges receipt to Digio
   * while initiating asynchronous background processing to ensure fast response times and prevent
   * timeouts. The method maintains distributed tracing context throughout the async processing
   * chain to enable proper monitoring and debugging across service boundaries.
   *
   * @param digioMandateWebhook The webhook payload received from Digio containing mandate
   *     information, event type, and related data
   * @return A Mono containing the acknowledgment response sent back to Digio. This response is sent
   *     immediately while processing continues asynchronously.
   * @throws BaseException if critical errors occur during initial webhook validation
   * @see #initiateMandateCallbackProcessing(DigioMandateWebhookDTO)
   */
  @Override
  public Mono<String> registerMandateWebhook(DigioMandateWebhookDTO digioMandateWebhook) {
    log.info("[{}] received callback from Digio", MANDATE_CALLBACK_IDENTIFIER);

    return Mono.deferContextual(
        parentContext -> {
          initiateMandateCallbackProcessing(digioMandateWebhook)
              .subscribeOn(Schedulers.parallel())
              .contextWrite(context -> context.put(TRACE_ID, parentContext.get(TRACE_ID)))
              .subscribe();

          log.info("[{}] Callback response sent to Digio", MANDATE_CALLBACK_IDENTIFIER);
          return Mono.just("Callback received");
        });
  }

  /**
   * Initiates the core mandate callback processing logic. This method orchestrates the main
   * business logic for processing Digio webhooks. It performs the following operations in sequence:
   *
   * <ol>
   *   <li>Looks up existing mandate registration details using the mandate ID
   *   <li>Retrieves product configuration and flow data for the associated partner
   *   <li>Determines the appropriate processing path based on webhook event type and mandate state
   *   <li>Routes to either M2P update flow or direct partner callback registration
   * </ol>
   *
   * <p>The method handles error scenarios gracefully, including missing mandate data and
   * configuration retrieval failures.
   *
   * @param digioMandateWebhook The Digio webhook payload containing mandate and event information
   * @return A Mono containing the result of the processing operation
   * @throws BaseException when mandate details are not found in the database
   * @throws BaseException when configuration data is missing or invalid
   * @throws NotFoundException when partner information cannot be located
   */
  @SuppressWarnings("unchecked")
  private Mono<Object> initiateMandateCallbackProcessing(
      DigioMandateWebhookDTO digioMandateWebhook) {
    log.info(
        "[{}] Processing Digio mandate webhook: {}",
        MANDATE_CALLBACK_IDENTIFIER,
        digioMandateWebhook.getEvent());

    HashMap<String, String> mandate =
        (HashMap<String, String>)
            digioMandateWebhook.getPayload().get(digioMandateWebhook.getEntities().get(0));
    String mandateId = mandate.get("id");

    return mandateRegistrationDetailsRepository
        .findByMandateId(mandateId)
        .flatMap(
            mandateRegistrationDetails -> {
              log.info(
                  "[{}] Found mandate registration details for mandateId: {}",
                  MANDATE_CALLBACK_IDENTIFIER,
                  mandateRegistrationDetails.getMandateId());

              return getMandateRegistrationFlowData(mandateRegistrationDetails)
                  .flatMap(
                      mandateRegistrationFlowData -> {
                        log.info(
                            "[{}] Found Product config for Mandate registration for productCode:"
                                + " {}",
                            MANDATE_CALLBACK_IDENTIFIER,
                            mandateRegistrationFlowData.getT1());

                        ProductControl.Flow mandateRegistrationProductFlow =
                            productConfigMasterService.getFlowFromProductConfig(
                                mandateRegistrationFlowData.getT2(),
                                MANDATE_REGISTRATION_IDENTIFIER);

                        String partnerCode = mandateRegistrationFlowData.getT1();

                        // get PartnerId from mandateRegistrationDetails
                        // get productCode from PartnerId

                        if (mandateRegistrationDetails.getUpdatedAtLos() == null
                            && ("mndt.authsuccess".equalsIgnoreCase(digioMandateWebhook.getEvent())
                                || "apimndt.authsuccess"
                                    .equalsIgnoreCase(digioMandateWebhook.getEvent()))) {
                          return partnerMasterService
                              .findByPartnerIdAndStatus(
                                  mandateRegistrationDetails.getPartnerId(), ACTIVE)
                              .flatMap(
                                  partnerMasterEntity -> {
                                    log.info(
                                        "[{}] start process Digio Callback With Updating M2p",
                                        MANDATE_CALLBACK_IDENTIFIER);
                                    return mandateDetailsService
                                        .processDigioCallbackWithUpdatingM2p(
                                            mandateId,
                                            digioMandateWebhook,
                                            mandateRegistrationDetails,
                                            partnerMasterEntity.getProductCode(),
                                            mandateRegistrationProductFlow,
                                            partnerCode);
                                  })
                              .doOnError(
                                  error ->
                                      log.error(
                                          "[{}] Failed to fetch partnerMasterEntity for the"
                                              + " partnerId: {}",
                                          MANDATE_CALLBACK_IDENTIFIER,
                                          mandateRegistrationDetails.getPartnerId()));
                        }

                        return partnerMasterService
                            .findByPartnerIdAndStatus(
                                mandateRegistrationDetails.getPartnerId(), ACTIVE)
                            .flatMap(
                                partnerMasterEntity -> {
                                  log.info(
                                      "[{}] start registering Mandate Callback To Partner",
                                      MANDATE_CALLBACK_IDENTIFIER);
                                  return registerMandateCallbackToPartner(
                                      mandateId,
                                      digioMandateWebhook,
                                      mandateRegistrationDetails,
                                      partnerMasterEntity.getProductCode(),
                                      mandateRegistrationProductFlow,
                                      partnerCode);
                                })
                            .doOnError(
                                error ->
                                    log.error(
                                        "[{}] Failed to fetch partnerMasterEntity for the"
                                            + " partnerId: {}",
                                        MANDATE_CALLBACK_IDENTIFIER,
                                        mandateRegistrationDetails.getPartnerId()));
                      });
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[{}] Mandate details not present in the database for mandateId: {}",
                      MANDATE_CALLBACK_IDENTIFIER,
                      digioMandateWebhook.getId());
                  return Mono.error(
                      new BaseException(
                          "Mandate details not found",
                          "Mandate details not found for mandateId: " + digioMandateWebhook.getId(),
                          HttpStatus.NOT_FOUND));
                }))
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] Error processing mandate registration details: {}",
                  MANDATE_CALLBACK_IDENTIFIER,
                  error.getMessage());
              return Mono.error(
                  new BaseException(
                      "Error processing mandate registration details",
                      error.getMessage(),
                      HttpStatus.BAD_REQUEST));
            });
  }

  /**
   * Retrieves mandate registration flow data and product configuration. This method fetches the
   * necessary configuration data required for processing the mandate callback. It performs a series
   * of lookups to gather:
   *
   * <ul>
   *   <li>Partner master data using the partner ID from mandate registration details
   *   <li>Product configuration data for the partner's product code
   *   <li>Mandate registration flow configuration from the product config
   * </ul>
   *
   * <p>The method validates that all required configuration data is present and properly structured
   * before returning the results.
   *
   * @param mandateRegistrationDetails The mandate registration entity containing partner ID and
   *     other registration details
   * @return A Mono containing a Tuple2 with product code (T1) and product control configuration
   *     (T2)
   * @throws NotFoundException when partner master data cannot be found for the given partner ID
   * @throws BaseException when mandate registration flow data is null or missing from configuration
   * @see PartnerMasterService#findByPartnerIdAndStatus(String, String)
   * @see ProductConfigMasterService#getProductConfigMasterData(String)
   * @see ProductConfigMasterService#getFlowFromProductConfig(ProductControl, String)
   */
  private Mono<Tuple2<String, ProductControl>> getMandateRegistrationFlowData(
      MandateRegistrationDetailsEntity mandateRegistrationDetails) {

    return partnerMasterService
        .findByPartnerIdAndStatus(mandateRegistrationDetails.getPartnerId(), ACTIVE)
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    "Partner not found for partnerId: "
                        + mandateRegistrationDetails.getPartnerId())))
        .flatMap(
            partnerMasterEntity ->
                productConfigMasterService
                    .getProductConfigMasterData(partnerMasterEntity.getProductCode())
                    .flatMap(
                        productControlConfigData -> {
                          // extracting data from product configuration
                          ProductControl.Flow mandateRegistrationFlowData =
                              productConfigMasterService.getFlowFromProductConfig(
                                  productControlConfigData.getT2(),
                                  MANDATE_REGISTRATION_IDENTIFIER);

                          // npe checks for product configuration data
                          if (Objects.isNull(mandateRegistrationFlowData)) {
                            return Mono.error(
                                new BaseException(
                                    SOMETHING_WENT_WRONG_CONFIG,
                                    SOMETHING_WENT_WRONG_CONFIG,
                                    HttpStatus.INTERNAL_SERVER_ERROR));
                          }

                          return Mono.just(productControlConfigData);
                        }));
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
            .build();

    CallbackLogEntity partnerMandateCallback =
        CallbackLogEntity.builder()
            .type(MANDATE_PARTNER_CALLBACK)
            .request(Json.of(gson.toJson(digioMandateWebhook)))
            .referenceId(mandateId)
            .uri(mandateRegistrationFlowData.getPartnerUri())
            .isRetry(mandateRegistrationFlowData.getRetryCount() > 0)
            .productCode(productCode)
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
              mandateDetailsService.setErrorDataInCallbackEntity(partnerMandateCallback, error);
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
                              "[{}] Error saving digio callback: {}",
                              MANDATE_CALLBACK_IDENTIFIER,
                              error.getMessage());
                          mandateDetailsService.setErrorDataInCallbackEntity(
                              digioMandateCallback, error);
                          return callbackRepository
                              .save(digioMandateCallback)
                              .flatMap(data -> Mono.error(error));
                        }));
  }

  @Override
  public Flux<MandateLiveBanksDigioResponse> fetchDigioMandateLiveBanks(String productCode) {
    return mandateDetailsService.processFetchDigioMandateLiveBanks(productCode);
  }
}
