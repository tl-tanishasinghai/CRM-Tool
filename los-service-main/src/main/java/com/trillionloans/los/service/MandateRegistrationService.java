package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.CLIENT_DETAILS_ERROR;
import static com.trillionloans.los.constant.StringConstants.EMPTY_MANDATE_REGISTRATION_CONFIG;
import static com.trillionloans.los.constant.StringConstants.FAIL;
import static com.trillionloans.los.constant.StringConstants.FAILED_TO_SAVE_MANDATE_DETAILS;
import static com.trillionloans.los.constant.StringConstants.LOAN_CLIENT_MISMATCH;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_DETAILS_LOG_HEADER;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_LOG_HEADER;
import static com.trillionloans.los.constant.StringConstants.PARSING_ERROR;
import static com.trillionloans.los.constant.StringConstants.PARTNER_NOT_FOUND;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.constant.StringConstants.UNAPPROVED_LOAN;
import static com.trillionloans.los.constant.StringConstants.UNSUPPORTED_VENDOR;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.DigioApi;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.model.dto.MandateRegistrationConfigDTO;
import com.trillionloans.los.model.dto.internal.LoanFunnelDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.MandateRegistrationDetailsEntity;
import com.trillionloans.los.model.request.MandateRegistrationRequest;
import com.trillionloans.los.model.request.digio.MandateRegistrationDigioRequest;
import com.trillionloans.los.model.response.ClientDetailsResponseDto;
import com.trillionloans.los.model.response.MandateRegistrationDetailsResponse;
import com.trillionloans.los.model.response.MandateRegistrationResponse;
import com.trillionloans.los.model.response.digio.MandateLiveBanksDigioResponse;
import com.trillionloans.los.model.response.digio.MandateRegistrationDigioResponse;
import com.trillionloans.los.repository.MandateRegistrationDetailsRepository;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.producers.KafkaFunnelLoggingService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service handling mandate registration logic, including preparation of requests, calling external
 * Digio API, and processing responses.
 */
@Service
@Slf4j
public class MandateRegistrationService {

  // ObjectMapper to convert between objects; could be injected if customized
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final LeadService leadService;
  private final LoanApplicationService loanApplicationService;
  private final DigioApi digioApi;
  private final ProductConfigMasterService productConfigMasterService;
  private final MandateRegistrationDetailsRepository mandateRegistrationDetailsRepository;

  /** Base login URL for Digio, injected from application properties. */
  private final String loginUrl;

  private final PartnerMasterService partnerMasterService;
  private final MandateDetailsService mandateDetailsService;
  private final KafkaFunnelLoggingService kafkaFunnelLoggingService;
  private final LoanClientLookupService loanClientLookupService;

  /**
   * Constructor for MandateRegistrationService.
   *
   * @param loginUrl Digio API login URL
   * @param digioApi External API client for Digio interaction
   * @param leadService Service to fetch lead/client data
   * @param loanApplicationService Service to fetch loan application data
   * @param productConfigMasterService Service to get product configuration
   */
  public MandateRegistrationService(
      @Value("${digio.api.login}") String loginUrl,
      DigioApi digioApi,
      LeadService leadService,
      LoanApplicationService loanApplicationService,
      ProductConfigMasterService productConfigMasterService,
      MandateRegistrationDetailsRepository mandateRegistrationDetailsRepository,
      PartnerMasterService partnerMasterService,
      MandateDetailsService mandateDetailsService,
      KafkaFunnelLoggingService kafkaFunnelLoggingService,
      LoanClientLookupService loanClientLookupService) {
    this.loginUrl = loginUrl;
    this.digioApi = digioApi;
    this.leadService = leadService;
    this.loanApplicationService = loanApplicationService;
    this.productConfigMasterService = productConfigMasterService;
    this.mandateRegistrationDetailsRepository = mandateRegistrationDetailsRepository;
    this.partnerMasterService = partnerMasterService;
    this.mandateDetailsService = mandateDetailsService;
    this.kafkaFunnelLoggingService = kafkaFunnelLoggingService;
    this.loanClientLookupService = loanClientLookupService;
  }

  /**
   * Orchestrates creation of mandate registration.
   *
   * @param loanId The loan ID
   * @param leadId The lead/client ID
   * @param mandateRegistrationRequest Request DTO with mandate registration input
   * @param productCode Product code used for config lookup
   * @return Mono emitting MandateRegistrationResponse or error if any step fails
   */
  public Mono<MandateRegistrationResponse> createMandateRegistration(
      String loanId,
      String leadId,
      MandateRegistrationRequest mandateRegistrationRequest,
      String productCode) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
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
                kafkaFunnelLoggingService.logMandateRegistrationAsync(
                    leadId,
                    loanId,
                    LoanFunnelDTO.SubStage.MR_PRODUCT_CONFIG,
                    FAIL,
                    null,
                    EMPTY_MANDATE_REGISTRATION_CONFIG);
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
                    "[{}] mandateRegistrationConfig is null or incomplete for loan: {}",
                    MANDATE_REGISTRATION_LOG_HEADER,
                    loanId);
                kafkaFunnelLoggingService.logMandateRegistrationAsync(
                    leadId,
                    loanId,
                    LoanFunnelDTO.SubStage.MR_PRODUCT_CONFIG,
                    FAIL,
                    null,
                    EMPTY_MANDATE_REGISTRATION_CONFIG);
                return Mono.error(new NotFoundException(PARSING_ERROR));
              }

              // Proceed to process the mandate registration with extracted flow data

              return partnerMasterService
                  .findByProductCode(productCode)
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                leadId,
                                loanId,
                                LoanFunnelDTO.SubStage.MR_PRODUCT_CONFIG,
                                FAIL,
                                null,
                                PARTNER_NOT_FOUND + productCode);
                            log.error(PARTNER_NOT_FOUND + "{}", productCode);
                            return Mono.error(
                                new NotFoundException(PARTNER_NOT_FOUND + productCode));
                          }))
                  .flatMap(
                      partnerMasterEntity -> {
                        kafkaFunnelLoggingService.logMandateRegistrationAsync(
                            leadId,
                            loanId,
                            LoanFunnelDTO.SubStage.MR_PRODUCT_CONFIG,
                            SUCCESS,
                            null,
                            null);
                        return processMandateRegistration(
                            loanId,
                            leadId,
                            mandateRegistrationRequest,
                            mandateRegistrationConfig,
                            partnerMasterEntity.getPartnerId(),
                            productCode);
                      });
            });
  }

  public Mono<MandateRegistrationDetailsResponse> fetchMandateRegistration(
      String leadId, String loanId, String mandateId, String productCode) {
    return mandateDetailsService.processFetchMandateRegistration(
        leadId, loanId, mandateId, productCode);
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

  /**
   * Processes the mandate registration by preparing request, fetching required data, calling Digio
   * API, and preparing the final response.
   *
   * @param loanId Loan ID
   * @param leadId Client lead ID
   * @param mandateRegistrationRequest Client input request for mandate registration
   * @param mandateRegistrationConfig Flow configuration extracted from product config
   * @return Mono emitting MandateRegistrationResponse
   */
  private Mono<MandateRegistrationResponse> processMandateRegistration(
      String loanId,
      String leadId,
      MandateRegistrationRequest mandateRegistrationRequest,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      String partnerId,
      String productCode) {

    // Prepare the Digio API request DTO based on mandate config and client request
    MandateRegistrationDigioRequest mandateRegistrationDigioRequest =
        prepareDigioMandateRegistrationRequest(
            leadId, loanId, mandateRegistrationConfig, mandateRegistrationRequest);

    // Fetch client details and enrich the mandate request with customer info
    log.info(
        "[{}] Fetching client details for client-id: {} and to set customer name in the"
            + " mandateRegistrationDigioRequest",
        MANDATE_REGISTRATION_LOG_HEADER,
        leadId);
    return switch (mandateRegistrationConfig.getVendorName()) {
      case DIGIO ->
          triggerDigioFlowForMandateRegistration(
              leadId,
              loanId,
              mandateRegistrationDigioRequest,
              mandateRegistrationConfig,
              partnerId,
              productCode);
      /* future vendor flows can be added here
       e.g. case OTHER_VENDOR -> triggerOtherVendorFlow(...)
      */
      default -> unsupportedVendorResponse(loanId, mandateRegistrationConfig);
    };
  }

  @SuppressWarnings("unchecked")
  private Mono<MandateRegistrationResponse> triggerDigioFlowForMandateRegistration(
      String leadId,
      String loanId,
      MandateRegistrationDigioRequest mandateRegistrationDigioRequest,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      String partnerId,
      String productCode) {
    return leadService
        .getLeadData(leadId)
        .doOnError(
            error -> {
              log.error(
                  "[{}] [ERROR] error fetching client details: {}",
                  MANDATE_REGISTRATION_LOG_HEADER,
                  error.getMessage());
              kafkaFunnelLoggingService.logMandateRegistrationAsync(
                  leadId,
                  loanId,
                  LoanFunnelDTO.SubStage.MR_CLIENT_DETAILS,
                  FAIL,
                  null,
                  "Error fetching client details");
            })
        .flatMap(
            leadData -> {
              kafkaFunnelLoggingService.logMandateRegistrationAsync(
                  leadId, loanId, LoanFunnelDTO.SubStage.MR_CLIENT_DETAILS, SUCCESS, null, null);
              log.info(
                  "[{}] Need following details from Client details api for client-id: {}. Details"
                      + " like customerName, mobileNumber and email",
                  MANDATE_REGISTRATION_LOG_HEADER,
                  leadId);

              mandateRegistrationDigioRequest.setCustomerIdentifier(leadData.getMobileNo());
              mandateRegistrationDigioRequest
                  .getMandateData()
                  .setCustomerName(getCustomerName(leadData));
              mandateRegistrationDigioRequest
                  .getMandateData()
                  .setCustomerMobile(leadData.getMobileNo());
              mandateRegistrationDigioRequest
                  .getMandateData()
                  .setCustomerEmail(leadData.getEmail());

              log.info(
                  "[{}] Fetching loan application details for loan-id: {} and set in the"
                      + " mandateRegistrationDigioRequest",
                  MANDATE_REGISTRATION_LOG_HEADER,
                  loanId);

              // Fetch loan application and augment mandate request with loan amount
              return loanClientLookupService
                  .getClientIdForLoan(loanId, null)
                  .doOnError(
                      error -> {
                        log.error(
                            "[{}] [ERROR] error fetching loan details: {}",
                            MANDATE_REGISTRATION_LOG_HEADER,
                            error.getMessage());
                        kafkaFunnelLoggingService.logMandateRegistrationAsync(
                            leadId,
                            loanId,
                            LoanFunnelDTO.SubStage.MR_LOAN_DETAILS,
                            FAIL,
                            null,
                            "Error fetching loan details");
                      })
                  .map(this::convertToHashMap)
                  .flatMap(
                      loanApplicationMap -> {
                        if (leadId != null
                            && !leadId.equals(loanApplicationMap.get("clientId").toString())) {
                          log.error(
                              "[{}] Requested loan details does not matches with client details",
                              MANDATE_REGISTRATION_LOG_HEADER);
                          kafkaFunnelLoggingService.logMandateRegistrationAsync(
                              leadId,
                              loanId,
                              LoanFunnelDTO.SubStage.MR_CLIENT_DETAILS,
                              FAIL,
                              null,
                              "Client ID and Loan ID Mismatch");
                          return Mono.error(
                              new ClientSideException(
                                  LOAN_CLIENT_MISMATCH,
                                  LOAN_CLIENT_MISMATCH,
                                  HttpStatus.BAD_REQUEST));
                        }

                        Long statusId =
                            ((Integer)
                                    ((Map<String, Object>) loanApplicationMap.get("status"))
                                        .get("id"))
                                .longValue();
                        log.info("[{}] statusId: {}", MANDATE_REGISTRATION_LOG_HEADER, statusId);

                        if (statusId != null && statusId != 300) {
                          log.error(
                              "[{}] Loan is not in approved state. Current statusId: {}",
                              MANDATE_REGISTRATION_LOG_HEADER,
                              statusId);
                          kafkaFunnelLoggingService.logMandateRegistrationAsync(
                              leadId,
                              loanId,
                              LoanFunnelDTO.SubStage.MR_LOAN_DETAILS,
                              FAIL,
                              null,
                              "Loan is not in approved state");
                          return Mono.error(
                              new ClientSideException(
                                  UNAPPROVED_LOAN, UNAPPROVED_LOAN, HttpStatus.BAD_REQUEST));
                        }
                        mandateRegistrationDigioRequest
                            .getMandateData()
                            .setMaximumAmount(getLoanAmount(loanApplicationMap));
                        kafkaFunnelLoggingService.logMandateRegistrationAsync(
                            leadId,
                            loanId,
                            LoanFunnelDTO.SubStage.MR_LOAN_DETAILS,
                            SUCCESS,
                            null,
                            null);
                        return mandateDetailsService
                            .processFetchDigioMandateLiveBanks(productCode)
                            .doOnError(
                                error -> {
                                  log.error(
                                      "[{}] [ERROR] error fetching live bank details: {}",
                                      MANDATE_REGISTRATION_LOG_HEADER,
                                      error.getMessage());
                                  kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                      leadId,
                                      loanId,
                                      LoanFunnelDTO.SubStage.MR_BANK_DETAILS,
                                      FAIL,
                                      null,
                                      "Error fetching live bank details");
                                })
                            .collectList()
                            .flatMap(
                                mandateLiveBanksDigioResponse -> {
                                  log.info(
                                      "[{}] Validating bank details for client-id: {}",
                                      MANDATE_REGISTRATION_LOG_HEADER,
                                      leadId);
                                  // mandateRegistrationDigioRequest.getMandateData().getDestinationBankName() is a customer bank name
                                  // mandateRegistrationDigioRequest.getMandateData().getDestinationBankId() is a customer bank ifsc code
                                  boolean isValidBank =
                                      validateBankDetails(
                                          mandateRegistrationDigioRequest
                                              .getMandateData()
                                              .getDestinationBankName(),
                                          mandateRegistrationDigioRequest
                                              .getMandateData()
                                              .getDestinationBankId(),
                                          mandateLiveBanksDigioResponse);
                                  if (!isValidBank) {
                                    log.error(
                                        "[{}] Bank details validation failed for client-id: {}."
                                            + " Bank Name: {}, IFSC: {}",
                                        MANDATE_REGISTRATION_LOG_HEADER,
                                        leadId,
                                        mandateRegistrationDigioRequest
                                            .getMandateData()
                                            .getDestinationBankName(),
                                        mandateRegistrationDigioRequest
                                            .getMandateData()
                                            .getDestinationBankId());
                                    kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                        leadId,
                                        loanId,
                                        LoanFunnelDTO.SubStage.MR_BANK_DETAILS,
                                        FAIL,
                                        null,
                                        "Bank Details Validations Failed");
                                    return Mono.error(
                                        new ClientSideException(
                                            "Invalid bank details provided",
                                            "The provided bank name or IFSC code is invalid or does"
                                                + " not match our records.",
                                            HttpStatus.BAD_REQUEST));
                                  }
                                  log.info(
                                      "[{}] Bank name provided in the request exist in the Digio"
                                          + " live bank list for client-id: {}, loanId: {}",
                                      MANDATE_REGISTRATION_LOG_HEADER,
                                      loanId,
                                      leadId);
                                  kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                      leadId,
                                      loanId,
                                      LoanFunnelDTO.SubStage.MR_BANK_DETAILS,
                                      SUCCESS,
                                      null,
                                      null);
                                  return digioApi
                                      .createMandateRegistration(
                                          mandateRegistrationDigioRequest, loanId, leadId)
                                      .flatMap(
                                          mandateRegistrationDigioResponse -> {
                                            log.info(
                                                "[{}] Mandate registration request created"
                                                    + " successfully at Digio's end for loan: {}",
                                                MANDATE_REGISTRATION_LOG_HEADER,
                                                loanId);
                                            kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                                leadId,
                                                loanId,
                                                LoanFunnelDTO.SubStage.MR_DIGIO_CREATE,
                                                SUCCESS,
                                                null,
                                                null);
                                            // save the mandate registration response in the
                                            // database
                                            return saveMandateRegistrationDetails(
                                                    leadId,
                                                    loanId,
                                                    mandateRegistrationDigioRequest,
                                                    mandateRegistrationDigioResponse,
                                                    mandateRegistrationConfig,
                                                    partnerId)
                                                .flatMap(
                                                    savedDetails -> {
                                                      kafkaFunnelLoggingService
                                                          .logMandateRegistrationAsync(
                                                              leadId,
                                                              loanId,
                                                              LoanFunnelDTO.SubStage.MR_FINAL,
                                                              SUCCESS,
                                                              null,
                                                              null);
                                                      return Mono.just(
                                                          prepareMandateRegistrationResponse(
                                                              mandateRegistrationDigioRequest,
                                                              mandateRegistrationDigioResponse,
                                                              mandateRegistrationConfig));
                                                    })
                                                .onErrorResume(
                                                    error -> {
                                                      log.error(
                                                          "[{}] Error saving mandate registration"
                                                              + " details for loan: {}. Error: {}",
                                                          MANDATE_REGISTRATION_LOG_HEADER,
                                                          loanId,
                                                          error.getMessage());
                                                      kafkaFunnelLoggingService
                                                          .logMandateRegistrationAsync(
                                                              leadId,
                                                              loanId,
                                                              LoanFunnelDTO.SubStage
                                                                  .MR_DIGIO_CREATE,
                                                              FAIL,
                                                              null,
                                                              "Failed to Save Mandate Details in"
                                                                  + " Trillion's DB");
                                                      return Mono.error(
                                                          new ClientSideException(
                                                              FAILED_TO_SAVE_MANDATE_DETAILS,
                                                              error.getMessage(),
                                                              HttpStatus.BAD_REQUEST));
                                                    });
                                            // Prepare and return application-level response DTO
                                          })
                                      .onErrorResume(
                                          error -> {
                                            log.error(
                                                "[{}] Error  creating mandate registration request"
                                                    + " for loan: {} at Digio's end. Error: {}",
                                                MANDATE_REGISTRATION_LOG_HEADER,
                                                loanId,
                                                error.getMessage());
                                            kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                                leadId,
                                                loanId,
                                                LoanFunnelDTO.SubStage.MR_DIGIO_CREATE,
                                                FAIL,
                                                null,
                                                "Failed to create Mandate Registration at Digio's"
                                                    + " End");
                                            return Mono.error(
                                                new ClientSideException(
                                                    "Failed to create mandate registration request"
                                                        + " at Digio's End",
                                                    error
                                                            instanceof
                                                            ClientSideException clientSideException
                                                        ? clientSideException.getClientResponse()
                                                        : error.getMessage(),
                                                    HttpStatus.BAD_REQUEST));
                                          });
                                });
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.error(
                                "[{}] Failed to find the loan details for the loanId: {}",
                                MANDATE_REGISTRATION_LOG_HEADER,
                                loanId);
                            kafkaFunnelLoggingService.logMandateRegistrationAsync(
                                leadId,
                                loanId,
                                LoanFunnelDTO.SubStage.MR_LOAN_DETAILS,
                                FAIL,
                                null,
                                "Loan Details Not Found");
                            return Mono.error(
                                new NotFoundException(
                                    "Failed to find the loan details for the loanId: " + loanId));
                          }));
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[{}] Client details not found for lead: {}. Hence failed to update"
                          + " mandate-registration-Digio-request-body",
                      MANDATE_REGISTRATION_LOG_HEADER,
                      leadId);
                  kafkaFunnelLoggingService.logMandateRegistrationAsync(
                      leadId,
                      loanId,
                      LoanFunnelDTO.SubStage.MR_CLIENT_DETAILS,
                      FAIL,
                      null,
                      "Client Details Not Found");
                  return Mono.error(new NotFoundException(CLIENT_DETAILS_ERROR));
                }));
  }

  private boolean validateBankDetails(
      String customerBankName,
      String customerIfscCode,
      List<MandateLiveBanksDigioResponse> liveBankResponse) {
    if (StringUtils.isBlank(customerBankName) || StringUtils.isBlank(customerIfscCode)) {
      log.error(
          "[{}] Customer bank name or IFSC code is blank. Bank Name: {}, IFSC: {}",
          MANDATE_REGISTRATION_LOG_HEADER,
          customerBankName,
          customerIfscCode);
      return false;
    }

    for (MandateLiveBanksDigioResponse bank : liveBankResponse) {
      if (bank.getName().equalsIgnoreCase(customerBankName)
          && customerIfscCode.toLowerCase().startsWith(bank.getIfscPrefix().toLowerCase())) {
        return true; // Valid bank found
      }
    }

    log.error(
        "[{}] No matching bank found for Bank Name: {}, IFSC: {} in Digio live bank list",
        MANDATE_REGISTRATION_LOG_HEADER,
        customerBankName,
        customerIfscCode);
    return false; // No valid bank found
  }

  private Mono<MandateRegistrationDetailsEntity> saveMandateRegistrationDetails(
      String leadId,
      String loanId,
      MandateRegistrationDigioRequest mandateRegistrationDigioRequest,
      MandateRegistrationDigioResponse mandateRegistrationDigioResponse,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      String partnerId) {

    MandateRegistrationDetailsEntity mandateRegistrationDetailsEntity =
        MandateRegistrationDetailsEntity.builder()
            .clientId(leadId)
            .loanId(loanId)
            .partnerId(partnerId)
            .mandateId(mandateRegistrationDigioResponse.getMandateId())
            .authMode(mandateRegistrationDigioRequest.getAuthMode().getDisplayName())
            .amount(mandateRegistrationDigioRequest.getMandateData().getMaximumAmount().toString())
            .frequencyType(mandateRegistrationDigioRequest.getMandateData().getFrequency())
            .vendorName(mandateRegistrationConfig.getVendorName().getDisplayName())
            .isRecurring(mandateRegistrationConfig.getIsRecurring())
            .state(mandateRegistrationDigioResponse.getState())
            .firstCollectionDate(
                LocalDateTime.parse(
                    mandateRegistrationDigioRequest.getMandateData().getFirstCollectionDate()))
            .finalCollectionDate(
                LocalDateTime.parse(
                    mandateRegistrationDigioRequest.getMandateData().getFinalCollectionDate()))
            .generateAccessToken(mandateRegistrationConfig.getGenerateAccessToken())
            .notifyCustomer(mandateRegistrationConfig.getNotifyCustomer())
            .createdAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
            .updatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
            .version(1)
            .isDeleted(false)
            .build();

    return mandateRegistrationDetailsRepository
        .save(mandateRegistrationDetailsEntity)
        .doOnSuccess(
            savedDetails ->
                log.info(
                    "[{}] Mandate registration details saved successfully for loan: {}",
                    MANDATE_REGISTRATION_LOG_HEADER,
                    loanId))
        .doOnError(
            error ->
                log.error(
                    "[{}] Error saving mandate registration details for loan: {}. Error: {}",
                    MANDATE_REGISTRATION_LOG_HEADER,
                    loanId,
                    error.getMessage()));
  }

  /**
   * Prepares the Digio mandate registration request DTO by combining input data and configuration
   * values.
   *
   * @param loanId Loan ID reference
   * @param mandateRegistrationConfig Configuration DTO for mandate registration
   * @param mandateRegistrationRequest Input client mandate registration request
   * @return Prepared Digio-specific mandate registration request DTO
   */
  private MandateRegistrationDigioRequest prepareDigioMandateRegistrationRequest(
      String leadId,
      String loanId,
      MandateRegistrationConfigDTO mandateRegistrationConfig,
      MandateRegistrationRequest mandateRegistrationRequest) {
    log.info(
        "[{}] Preparing MandateRegistrationDigioRequest for leadId: {}, and loanId: {}",
        MANDATE_REGISTRATION_LOG_HEADER,
        leadId,
        loanId);
    String firstCollectionDate =
        LocalDateTime.now(ZoneId.of(ASIA_KOLKATA))
            .plusDays(mandateRegistrationConfig.getFirstCollectionIncrement())
            .toString();

    String finalCollectionDate =
        LocalDateTime.now(ZoneId.of(ASIA_KOLKATA))
            .plusYears(mandateRegistrationConfig.getFinalCollectionIncrement())
            .toString();

    // Build the complete Digio mandate registration request DTO
    return MandateRegistrationDigioRequest.builder()
        .authMode(mandateRegistrationConfig.getAuthMode())
        .corporateConfigId(mandateRegistrationConfig.getCorporateConfigId())
        .mandateType(mandateRegistrationConfig.getMandateType())
        .notifyCustomer(mandateRegistrationConfig.getNotifyCustomer())
        .generateAccessToken(mandateRegistrationConfig.getGenerateAccessToken())
        .mandateData(
            MandateRegistrationDigioRequest.MandateData.builder()
                .customerAccountNumber(mandateRegistrationRequest.getAccountNumber())
                .customerAccountType(mandateRegistrationRequest.getAccountType())
                .isRecurring(mandateRegistrationConfig.getIsRecurring())
                .frequency(mandateRegistrationConfig.getFrequencyType())
                .managementCategory(mandateRegistrationConfig.getManagementCategory())
                .customerRefNumber(loanId)
                .firstCollectionDate(firstCollectionDate)
                .finalCollectionDate(finalCollectionDate)
                .destinationBankId(mandateRegistrationRequest.getIfsc())
                .destinationBankName(mandateRegistrationRequest.getBankName())
                .instrumentType(mandateRegistrationConfig.getInstrumentType())
                .build())
        .build();
  }

  /**
   * Prepares the application-level response DTO from the Digio API response and the original
   * mandate registration request and configuration.
   *
   * @param mandateRegistrationDigioRequest The original Digio request DTO
   * @param mandateRegistrationDigioResponse The Digio API response DTO
   * @param mandateRegistrationConfig Config DTO for reference
   * @return MandateRegistrationResponse to be returned by the service
   */
  private MandateRegistrationResponse prepareMandateRegistrationResponse(
      MandateRegistrationDigioRequest mandateRegistrationDigioRequest,
      MandateRegistrationDigioResponse mandateRegistrationDigioResponse,
      MandateRegistrationConfigDTO mandateRegistrationConfig) {
    return MandateRegistrationResponse.builder()
        .mandateId(mandateRegistrationDigioResponse.getMandateId())
        .customerUrl(
            prepareCustomerUrl(
                mandateRegistrationDigioRequest,
                mandateRegistrationDigioResponse,
                mandateRegistrationConfig))
        .createdAt(mandateRegistrationDigioResponse.getCreatedAt())
        .state(mandateRegistrationDigioResponse.getState())
        .type(mandateRegistrationDigioResponse.getType())
        .expireAt(
            mandateRegistrationDigioResponse.getAccessToken() != null
                ? mandateRegistrationDigioResponse.getAccessToken().getValidTill()
                : null)
        .build();
  }

  /**
   * Constructs the customer URL for mandate registration redirection.
   *
   * @param mandateRegistrationDigioRequest The original Digio request DTO
   * @param mandateRegistrationDigioResponse The Digio API response DTO
   * @param mandateRegistrationConfig Configuration DTO containing redirection URL
   * @return Fully formed customer redirection URL string
   */
  private String prepareCustomerUrl(
      MandateRegistrationDigioRequest mandateRegistrationDigioRequest,
      MandateRegistrationDigioResponse mandateRegistrationDigioResponse,
      MandateRegistrationConfigDTO mandateRegistrationConfig) {

    StringBuilder urlBuilder =
        new StringBuilder(loginUrl)
            .append("/")
            .append(mandateRegistrationDigioResponse.getMandateId())
            .append("/")
            // Append a substring of a UUID for uniqueness
            .append(UUID.randomUUID().toString(), 0, 7)
            .append("/")
            .append(mandateRegistrationDigioRequest.getCustomerIdentifier())
            .append("?redirect_url=")
            .append(mandateRegistrationConfig.getRedirectionUrl());

    // Conditionally append token_id parameter if configured
    if (mandateRegistrationConfig.getGenerateAccessToken() != null
        && mandateRegistrationConfig.getGenerateAccessToken()) {
      urlBuilder
          .append("&token_id=")
          .append(mandateRegistrationDigioResponse.getAccessToken().getId());
    }

    return urlBuilder.toString();
  }

  /**
   * Returns the full customer name by concatenating first, middle (if present), and last names from
   * client details DTO.
   *
   * @param clientResponseDto Client details DTO containing name parts
   * @return Concatenated full customer name
   */
  private String getCustomerName(ClientDetailsResponseDto clientResponseDto) {
    if (StringUtils.isEmpty(clientResponseDto.getMiddleName())) {
      return clientResponseDto.getFirstName() + " " + clientResponseDto.getLastName();
    } else {
      return clientResponseDto.getFirstName()
          + " "
          + clientResponseDto.getMiddleName()
          + " "
          + clientResponseDto.getLastName();
    }
  }

  /**
   * Extracts loan amount from the loan application map.
   *
   * @param loanApplicationMap Map representing loan application data
   * @return Loan amount as Double, or null if not present
   */
  private Double getLoanAmount(Map<String, Object> loanApplicationMap) {
    return (Double) loanApplicationMap.get("loanAmountRequested");
  }

  /**
   * Converts the generic loan application object into a Map for easier processing.
   *
   * @param loanApplication Loan application as an object (usually JSON deserialized)
   * @return Map representation of the loan application; empty if not convertible
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> convertToHashMap(Object loanApplication) {
    if (loanApplication instanceof Map) {
      return (Map<String, Object>) loanApplication;
    } else {
      return new HashMap<>();
    }
  }
}
