package com.trillionloans.los.service.impl;

import static com.trillionloans.los.constant.StringConstants.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.PartnerApi;
import com.trillionloans.los.constant.CreditLineStatus;
import com.trillionloans.los.constant.QcCheckStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.entity.CreditLineEntity;
import com.trillionloans.los.model.partner.m2p.M2PGenerateCreditLineRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2PGenerateCreditLineResponseDTO;
import com.trillionloans.los.model.request.CreditLineLoanApplication;
import com.trillionloans.los.model.request.InitiateCreditLineRequestDTO;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.request.m2p.CreditLineStatusCallbackRequest;
import com.trillionloans.los.model.response.CreditLineCallbackToPartnerDTO;
import com.trillionloans.los.model.response.InitiateCreditLineResponseDTO;
import com.trillionloans.los.model.response.m2p.M2PFetchCreditLineResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pErrorResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.repository.CreditLineRepository;
import com.trillionloans.los.repository.LoanClientPartnerMapRepository;
import com.trillionloans.los.service.CreditLineLimitApprovedNotificationService;
import com.trillionloans.los.service.CreditLineService;
import com.trillionloans.los.service.LoanApplicationService;
import com.trillionloans.los.service.LoanTaggingService;
import com.trillionloans.los.service.M2pFacadeService;
import com.trillionloans.los.service.QcScfRuleService;
import com.trillionloans.los.service.db.CallbackStoreService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.drawdownorchestrator.DrawdownService;
import com.trillionloans.los.util.CreditLineUtil;
import com.trillionloans.los.util.LoanDataUtil;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

@Slf4j
@Service
@AllArgsConstructor
public class CreditLineServiceImpl implements CreditLineService {
  private final M2PWrapperApi m2PWrapperApi;
  private final LoanApplicationService loanApplicationService;
  private final ObjectMapper objectMapper;
  private final LoanClientPartnerMapRepository loanClientPartnerMapRepository;
  private final CreditLineRepository creditLineRepository;
  private final ProductConfigMasterService productConfigMasterService;
  private final QcScfRuleService qcScfRuleService;
  private final PartnerApi partnerApi;
  private final CallbackStoreService callbackStoreService;
  private final Gson gson;
  private final LoanTaggingService loanTaggingService;
  private final DrawdownService drawdownService;
  private final CreditLineLimitApprovedNotificationService
      creditLineLimitApprovedNotificationService;
  private final M2pFacadeService m2pFacadeService;

  // ========== PUBLIC METHODS (Interface Implementations) ==========

  @Override
  public Mono<M2pLoanCreationResponseDTO> createCreditLineLead(
      CreditLineLoanApplication creditLineRequest, String leadId, String productCode) {

    // Validate that product is a credit line product
    validateCreditLineProduct(productCode);

    LoanApplication loanApplication =
        CreditLineUtil.mapToLoanApplication(creditLineRequest, productCode);

    return loanApplicationService.triggerLoanAppCreationBasedOnPanValidationServiceType(
        loanApplication, leadId, productCode);
  }

  @Override
  public Mono<M2PGenerateCreditLineResponseDTO> generateCreditLine(
      M2PGenerateCreditLineRequestDTO request, String leadId, String productCode) {

    // Flow -> Generate credit line, persistCreditLineId, register CTA, do not throw error if CTA
    // registration fails.
    return Mono.justOrEmpty(request)
        .flatMap(req -> m2PWrapperApi.generateCreditLine(Mono.just(req), leadId))
        .map(resp -> objectMapper.convertValue(resp, M2PGenerateCreditLineResponseDTO.class))
        // First persist the generated credit line id
        .flatMap(resp -> persistCreditLineId(leadId, resp.getAccountNumber()).thenReturn(resp))
        // Then try to register CTA but do not fail overall flow if CTA registration fails
        .flatMap(
            resp ->
                m2PWrapperApi
                    .registerCta(leadId, CREDIT_LINE_CREATION_CTA_IDENTIFIER)
                    .onErrorResume(
                        error -> {
                          log.error(
                              "[CREDIT_LINE_CTA_REGISTRATION_FAILED] Failed to register CTA for"
                                  + " leadId={}, error={}",
                              leadId,
                              error.getMessage());
                          return Mono.empty();
                        })
                    .thenReturn(resp))
        .doOnSuccess(
            resp ->
                log.info(
                    "[CREDIT_LINE_CREATED] leadId={}, accountNumber={}, allowedLimit={}",
                    leadId,
                    resp.getAccountNumber(),
                    resp.getAllowedLimit()));
  }

  @Override
  public Mono<Object> fetchCreditLine(String leadId, String productCode) {
    return m2PWrapperApi.fetchCreditLine(leadId);
  }

  @Override
  public Mono<Object> fetchCreditLineDetailsByLineId(String lineId, String productCode) {

    if (StringUtils.isBlank(lineId)) {
      log.error("[GET_CREDIT_LINE_DETAILS_BY_LINE_ID] lineId is null or blank");
      return Mono.error(
          new BaseException(
              "lineId is required", "lineId cannot be null or blank", HttpStatus.BAD_REQUEST));
    }

    if (StringUtils.isBlank(productCode)) {
      log.error(
          "[GET_CREDIT_LINE_DETAILS_BY_LINE_ID] productCode is null or blank for lineId={}",
          lineId);
      return Mono.error(
          new BaseException(
              "productCode is required",
              "productCode cannot be null or blank",
              HttpStatus.BAD_REQUEST));
    }

    log.info(
        "[GET_CREDIT_LINE_DETAILS_BY_LINE_ID] Fetching credit line status for lineId={},"
            + " productCode={}",
        lineId,
        productCode);

    return drawdownService
        .getLeadIdFromCacheOrDb(lineId)
        .flatMap(
            leadId -> {
              log.info(
                  "[GET_CREDIT_LINE_DETAILS_BY_LINE_ID] Resolved leadId={} for lineId={}, fetching"
                      + " status",
                  leadId,
                  lineId);
              return m2PWrapperApi.fetchCreditLine(leadId);
            })
        .doOnError(
            error ->
                log.error(
                    "[GET_CREDIT_LINE_DETAILS_BY_LINE_ID] Failed to get credit line status for"
                        + " lineId={}, error={}",
                    lineId,
                    error.getMessage()));
  }

  @Override
  public Mono<Object> activateCreditLine(String leadId, String productCode) {
    return m2PWrapperApi
        .activateCreditLine(leadId)
        .flatMap(
            resp ->
                m2PWrapperApi
                    .registerCta(leadId, DISBURSEMENT_TRIGGER_CTA_IDENTIFIER)
                    .onErrorResume(
                        error -> {
                          log.error(
                              "[CREDIT_LINE_ACTIVATION_CTA_FAILED] Failed to register CTA for"
                                  + " leadId={}, error={}",
                              leadId,
                              error.getMessage());
                          return Mono.empty();
                        })
                    .thenReturn(resp));
  }

  @Override
  public Mono<Object> approveCreditLine(String leadId, String productCode) {
    return m2PWrapperApi
        .approveCreditLine(leadId)
        .flatMap(
            resp ->
                m2PWrapperApi
                    .registerCta(leadId, CREDIT_LINE_APPROVAL_CTA_IDENTIFIER)
                    .onErrorResume(
                        error -> {
                          log.error(
                              "[CREDIT_LINE_APPROVAL_CTA_FAILED] Failed to register CTA for"
                                  + " leadId={}, error={}",
                              leadId,
                              error.getMessage());
                          return Mono.empty();
                        })
                    .thenReturn(resp));
  }

  @Override
  public Mono<InitiateCreditLineResponseDTO> initiateCreditLine(
      InitiateCreditLineRequestDTO request, String leadId, String productCode) {

    log.info(
        "[INITIATE_CREDIT_LINE] Initiating credit line for leadId={}, productCode={}",
        leadId,
        productCode);

    CreditLineEntity creditLineEntity =
        CreditLineEntity.builder()
            .leadId(leadId)
            .productCode(productCode)
            .creditLimit(BigDecimal.valueOf(request.getAllowedLimit()))
            .tenureType(request.getTenureDetails().getType())
            .tenureValue(request.getTenureDetails().getValue())
            .status(CreditLineStatus.PENDING.getValue())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    return creditLineRepository
        .save(creditLineEntity)
        .map(
            savedEntity -> {
              log.info(
                  "[INITIATE_CREDIT_LINE] Successfully saved credit line with id={} for leadId={}",
                  savedEntity.getId(),
                  leadId);
              return InitiateCreditLineResponseDTO.builder()
                  .status(SUCCESS)
                  .leadId(leadId)
                  .message("Credit line initiated successfully with PENDING status")
                  .build();
            })
        .doOnError(
            error ->
                log.error(
                    "[INITIATE_CREDIT_LINE] Failed to initiate credit line for leadId={}, error={}",
                    leadId,
                    error.getMessage()))
        .onErrorMap(
            error ->
                new BaseException(
                    "Failed to initiate credit line",
                    error.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Override
  public Mono<CreditLineCallbackToPartnerDTO> getCreditLineStatusByLeadId(
      String leadId, String productCode) {

    log.info(
        "[GET_CREDIT_LINE_STATUS] Fetching credit line status for leadId={}, productCode={}",
        leadId,
        productCode);

    return creditLineRepository
        .findFirstByLeadIdAndProductCodeOrderByCreatedAtDesc(leadId, productCode)
        .switchIfEmpty(
            Mono.error(
                new BaseException(
                    "Credit line not found for leadId: " + leadId,
                    NOT_FOUND,
                    HttpStatus.NOT_FOUND)))
        .flatMap(
            creditLineEntity -> {
              log.info(
                  "[GET_CREDIT_LINE_STATUS] Found credit line with status={} for leadId={}",
                  creditLineEntity.getStatus(),
                  leadId);

              String status = creditLineEntity.getStatus();

              // If the CreditLineStatus is: PENDING, REJECTED, ACTIVE -> just build and return DTO
              if (CreditLineStatus.PENDING.getValue().equals(status)
                  || CreditLineStatus.REJECTED.getValue().equals(status)
                  || CreditLineStatus.ACTIVE.getValue().equals(status)) {
                return Mono.just(CreditLineUtil.buildCreditLineStatusDto(creditLineEntity));
              }

              // If the CreditLineStatus is: OPS_APPROVED, CREATED, APPROVED ->
              // Sync with M2P first to handle cases where M2P is ahead of local status,
              // then process based on M2P's actual state
              if (CreditLineStatus.OPS_APPROVED.getValue().equals(status)
                  || CreditLineStatus.CREATED.getValue().equals(status)
                  || CreditLineStatus.APPROVED.getValue().equals(status)) {
                return syncWithM2PAndProcess(creditLineEntity, productCode);
              }

              // Fallback: for any other status, behave like the original implementation
              return Mono.just(CreditLineUtil.buildCreditLineStatusDto(creditLineEntity));
            })
        .doOnError(
            error ->
                log.error(
                    "[GET_CREDIT_LINE_STATUS] Failed to get credit line status for leadId={},"
                        + " error={}",
                    leadId,
                    error.getMessage()));
  }

  @Override
  public Mono<CreditLineCallbackToPartnerDTO> getCreditLineStatusByLineId(
      String lineId, String productCode) {

    if (StringUtils.isBlank(lineId)) {
      log.error("[GET_CREDIT_LINE_STATUS_BY_LINE_ID] lineId is null or blank");
      return Mono.error(
          new BaseException(
              "lineId is required", "lineId cannot be null or blank", HttpStatus.BAD_REQUEST));
    }

    if (StringUtils.isBlank(productCode)) {
      log.error(
          "[GET_CREDIT_LINE_STATUS_BY_LINE_ID] productCode is null or blank for lineId={}", lineId);
      return Mono.error(
          new BaseException(
              "productCode is required",
              "productCode cannot be null or blank",
              HttpStatus.BAD_REQUEST));
    }

    log.info(
        "[GET_CREDIT_LINE_STATUS_BY_LINE_ID] Fetching credit line status for lineId={},"
            + " productCode={}",
        lineId,
        productCode);

    return drawdownService
        .getLeadIdFromCacheOrDb(lineId)
        .flatMap(
            leadId -> {
              log.info(
                  "[GET_CREDIT_LINE_STATUS_BY_LINE_ID] Resolved leadId={} for lineId={}, fetching"
                      + " status",
                  leadId,
                  lineId);
              return getCreditLineStatusByLeadId(leadId, productCode);
            })
        .doOnError(
            error ->
                log.error(
                    "[GET_CREDIT_LINE_STATUS_BY_LINE_ID] Failed to get credit line status for"
                        + " lineId={}, error={}",
                    lineId,
                    error.getMessage()));
  }

  @Override
  public Mono<Object> processCreditLineStatusCallback(
      CreditLineStatusCallbackRequest callbackRequest, String productCode) {

    String loanApplicationId = callbackRequest.getLoanApplicationId();
    String status = callbackRequest.getStatus();

    log.info(
        "[CREDIT_LINE_STATUS_CALLBACK] Processing callback for loanApplicationId={}, status={}",
        loanApplicationId,
        status);

    if (!SUCCESS.equalsIgnoreCase(status)) {
      log.warn(
          "[CREDIT_LINE_STATUS_CALLBACK] Received non-SUCCESS status={} for loanApplicationId={}",
          status,
          loanApplicationId);
      return Mono.just("Callback received but status is not SUCCESS");
    }

    return creditLineRepository
        .findFirstByLeadIdAndProductCodeOrderByCreatedAtDesc(loanApplicationId, productCode)
        .switchIfEmpty(
            Mono.error(
                new BaseException(
                    "Credit line not found for leadId: " + loanApplicationId,
                    NOT_FOUND,
                    HttpStatus.NOT_FOUND)))
        .flatMap(
            creditLineEntity -> {
              creditLineEntity.setStatus(CreditLineStatus.OPS_APPROVED.getValue());
              creditLineEntity.setUpdatedAt(LocalDateTime.now());

              log.info(
                  "[CREDIT_LINE_STATUS_CALLBACK] Updated credit line status to OPS_APPROVED for"
                      + " leadId={}",
                  loanApplicationId);

              return creditLineRepository.save(creditLineEntity);
            })
        // Trigger generate / approve / activate flow and send callback to partner
        .flatMap(updatedEntity -> processGenerateApproveActivate(updatedEntity, productCode, true))
        .doOnSuccess(
            result ->
                log.info(
                    "[CREDIT_LINE_STATUS_CALLBACK] Successfully processed callback for"
                        + " loanApplicationId={}",
                    loanApplicationId))
        .doOnError(
            error ->
                log.error(
                    "[CREDIT_LINE_STATUS_CALLBACK] Failed to process callback for"
                        + " loanApplicationId={}, error={}",
                    loanApplicationId,
                    error.getMessage()));
  }

  // ========== PRIVATE METHODS ==========

  /**
   * Validates that the product code is a valid credit line product.
   *
   * @param productCode the product code to validate
   * @throws BaseException if the product is not a credit line product
   */
  private void validateCreditLineProduct(String productCode) {
    if (!LoanDataUtil.isCreditLineProduct(productCode)) {
      log.error(
          "[CREDIT_LINE_VALIDATION_FAILED] Product code '{}' is not a valid credit line product",
          productCode);
      throw new BaseException(
          "Invalid product code. Only credit line products are allowed for this API.",
          "Product code '" + productCode + "' is not a valid credit line product.",
          HttpStatus.BAD_REQUEST);
    }
  }

  private Mono<Void> persistCreditLineId(String leadId, String accountNumber) {
    return loanClientPartnerMapRepository
        .findByLoanApplicationId(Integer.valueOf(leadId))
        .flatMap(
            entity -> {
              entity.setLineId(accountNumber);
              return loanClientPartnerMapRepository.save(entity);
            })
        .doOnSuccess(
            entity ->
                log.info(
                    "[CREDIT_LINE_PERSISTED] leadId={}, creditLineId={}", leadId, accountNumber))
        .doOnError(
            error ->
                log.error(
                    "[CREDIT_LINE_PERSIST_FAILED] leadId={}, creditLineId={}, error={}",
                    leadId,
                    accountNumber,
                    error.getMessage()))
        .onErrorResume(error -> Mono.empty())
        .then();
  }

  /**
   * Fetches credit line status from M2P and maps to typed DTO for internal sync logic. This method
   * is used internally for state synchronization and does not change the public fetchCreditLine
   * API.
   */
  private Mono<M2PFetchCreditLineResponseDTO> fetchCreditLineForSync(String leadId) {
    return m2PWrapperApi
        .fetchCreditLine(leadId)
        .map(response -> objectMapper.convertValue(response, M2PFetchCreditLineResponseDTO.class));
  }

  /**
   * Syncs local credit line entity to match M2P state. Updates status, m2p_credit_line_id, and
   * timestamps based on M2P response.
   */
  private Mono<CreditLineEntity> syncLocalToM2P(
      CreditLineEntity entity, M2PFetchCreditLineResponseDTO m2pResponse) {

    String leadId = entity.getLeadId();
    String m2pStatus = m2pResponse.getStatus();
    String localStatus = entity.getStatus();

    log.info(
        "[CREDIT_LINE_SYNC] Syncing local status from {} to M2P status {} for leadId={}",
        localStatus,
        m2pStatus,
        leadId);

    entity.setM2pCreditLineId(m2pResponse.getAccountNumber());
    entity.setStatus(m2pStatus);
    entity.setUpdatedAt(LocalDateTime.now());

    if (CreditLineStatus.CREATED.getValue().equals(m2pStatus)
        || CreditLineStatus.APPROVED.getValue().equals(m2pStatus)
        || CreditLineStatus.ACTIVE.getValue().equals(m2pStatus)) {
      if (entity.getLimitCreatedAt() == null) {
        entity.setLimitCreatedAt(CreditLineUtil.epochMsToLocalDateTime(m2pResponse.getCreatedOn()));
      }
    }

    if (CreditLineStatus.APPROVED.getValue().equals(m2pStatus)
        || CreditLineStatus.ACTIVE.getValue().equals(m2pStatus)) {
      if (entity.getLimitApprovedAt() == null) {
        entity.setLimitApprovedAt(LocalDateTime.now());
      }
    }

    if (CreditLineStatus.ACTIVE.getValue().equals(m2pStatus)) {
      if (entity.getLimitActivatedAt() == null) {
        entity.setLimitActivatedAt(
            CreditLineUtil.epochMsToLocalDateTime(m2pResponse.getActivatedOn()));
      }
    }

    return creditLineRepository
        .save(entity)
        .doOnSuccess(
            saved ->
                log.info(
                    "[CREDIT_LINE_SYNC] Successfully synced credit line for leadId={},"
                        + " newStatus={}",
                    leadId,
                    m2pStatus));
  }

  /**
   * Syncs with M2P to get actual credit line status and processes accordingly. This method handles
   * the case where local repository status may be behind M2P's actual state.
   *
   * <p>Flow:
   *
   * <ul>
   *   <li>Fetch credit line status from M2P
   *   <li>If M2P returns 403 (not created) -> proceed with full generate/approve/activate flow
   *   <li>If M2P is ahead of local -> sync local to M2P state
   *   <li>Process based on M2P's actual status (ACTIVE -> return, APPROVED -> activate, etc.)
   * </ul>
   */
  private Mono<CreditLineCallbackToPartnerDTO> syncWithM2PAndProcess(
      CreditLineEntity creditLineEntity, String productCode) {

    String leadId = creditLineEntity.getLeadId();
    String localStatus = creditLineEntity.getStatus();

    log.info(
        "[CREDIT_LINE_SYNC] Starting M2P sync for leadId={}, localStatus={}", leadId, localStatus);

    return fetchCreditLineForSync(leadId)
        .flatMap(
            m2pResponse -> {
              String m2pStatus = m2pResponse.getStatus();

              log.info(
                  "[CREDIT_LINE_SYNC] M2P status={} for leadId={}, localStatus={}",
                  m2pStatus,
                  leadId,
                  localStatus);

              // Check if M2P is ahead and sync if needed
              Mono<CreditLineEntity> syncedEntityMono;
              if (CreditLineUtil.isM2PAhead(localStatus, m2pStatus)) {
                log.info(
                    "[CREDIT_LINE_SYNC] M2P is ahead, syncing local status for leadId={}", leadId);
                syncedEntityMono = syncLocalToM2P(creditLineEntity, m2pResponse);
              } else {
                syncedEntityMono = Mono.just(creditLineEntity);
              }

              return syncedEntityMono.flatMap(
                  syncedEntity -> {
                    // If M2P says ACTIVE, just return the DTO - nothing more to do
                    if (CreditLineStatus.ACTIVE.getValue().equals(m2pStatus)) {
                      log.info(
                          "[CREDIT_LINE_SYNC] Credit line already ACTIVE on M2P for leadId={},"
                              + " returning status",
                          leadId);
                      return Mono.just(CreditLineUtil.buildCreditLineStatusDto(syncedEntity));
                    }

                    // For all other statuses (CREATED, APPROVED, or not created),
                    // let processGenerateApproveActivate handle the routing based on synced status
                    log.info(
                        "[CREDIT_LINE_SYNC] Proceeding with processGenerateApproveActivate for"
                            + " leadId={}, syncedStatus={}",
                        leadId,
                        syncedEntity.getStatus());
                    return processGenerateApproveActivate(syncedEntity, productCode, false)
                        .then(
                            creditLineRepository
                                .findFirstByLeadIdAndProductCodeOrderByCreatedAtDesc(
                                    leadId, productCode)
                                .map(CreditLineUtil::buildCreditLineStatusDto));
                  });
            })
        .onErrorResume(
            error -> {
              // Handle 403 error - credit line not created on M2P yet
              // This is expected when local status is OPS_APPROVED but M2P has no credit line
              if (is403CreditLineNotFoundError(error)) {
                log.info(
                    "[CREDIT_LINE_SYNC] Credit line not found on M2P for leadId={}, proceeding with"
                        + " full flow",
                    leadId);
                return processGenerateApproveActivate(creditLineEntity, productCode, false)
                    .then(
                        creditLineRepository
                            .findFirstByLeadIdAndProductCodeOrderByCreatedAtDesc(
                                leadId, productCode)
                            .map(CreditLineUtil::buildCreditLineStatusDto));
              }

              log.error(
                  "[CREDIT_LINE_SYNC] Error fetching M2P status for leadId={}, error={}",
                  leadId,
                  error.getMessage());
              return Mono.error(error);
            });
  }

  /**
   * Checks if the error is a 403 "Credit Line not found" error from M2P. This indicates the credit
   * line has not been created on M2P yet.
   *
   * <p>M2P returns 403 with error response containing userMessageGlobalisationCode
   * "err.msg.creditline.not.found" when credit line doesn't exist.
   */
  private boolean is403CreditLineNotFoundError(Throwable error) {
    if (!(error instanceof ClientSideException clientSideException)) {
      return false;
    }

    M2pErrorResponseDTO errorResponse = getM2pErrorResponse(clientSideException.getResponseBody());
    if (errorResponse == null) {
      return false;
    }

    // Check httpStatusCode is 403
    if (!"403".equals(errorResponse.getHttpStatusCode())) {
      return false;
    }

    // Check errors list for creditline.not.found
    if (errorResponse.getErrors() != null && !errorResponse.getErrors().isEmpty()) {
      return errorResponse.getErrors().stream()
          .filter(Objects::nonNull)
          .map(M2pErrorResponseDTO.ErrorDetailDTO::getUserMessageGlobalisationCode)
          .anyMatch(CreditLineUtil::containsCreditLineNotFound);
    }

    return false;
  }

  /** Parses M2P error response from the response body. */
  private M2pErrorResponseDTO getM2pErrorResponse(Object responseBody) {
    if (responseBody == null) {
      return null;
    }
    try {
      return gson.fromJson(gson.toJson(responseBody), M2pErrorResponseDTO.class);
    } catch (Exception e) {
      log.warn("[CREDIT_LINE_SYNC] Failed to parse M2P error response: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Processes generate, approve, and activate credit line operations based on current status and
   * sends callback to partner.
   *
   * <p>State-based processing:
   *
   * <ul>
   *   <li>PENDING → Generate → Approve → Activate (full flow)
   *   <li>CREATED → Approve → Activate (skip Generate)
   *   <li>APPROVED → Activate only (skip Generate and Approve)
   *   <li>ACTIVE → Log and return (already activated)
   * </ul>
   */
  private Mono<Object> processGenerateApproveActivate(
      CreditLineEntity creditLineEntity, String productCode, boolean sendPartnerCallback) {

    String leadId = creditLineEntity.getLeadId();

    log.info(
        "[CREDIT_LINE_PROCESS] Starting QC checks for leadId={}, currentStatus={}",
        leadId,
        creditLineEntity.getStatus());

    return qcScfRuleService
        .processPreLimitActivationQcChecks(
            leadId, productCode, creditLineEntity.getM2pCreditLineId())
        .flatMap(
            qcResult -> {
              if (qcResult.getStatus().equals(QcCheckStatus.REJECTED)) {
                log.info(
                    "[CREDIT_LINE_QC] QC rejected for leadId={}, reason={}",
                    leadId,
                    qcResult.getReason());

                creditLineEntity.setStatus(CreditLineStatus.REJECTED.getValue());
                creditLineEntity.setUpdatedAt(LocalDateTime.now());

                return creditLineRepository
                    .save(creditLineEntity)
                    .flatMap(
                        rejectedEntity -> {
                          if (sendPartnerCallback) {
                            return sendCreditLineCallbackToPartner(rejectedEntity, productCode);
                          }
                          return Mono.just(rejectedEntity);
                        });
              }

              String currentStatus = creditLineEntity.getStatus();

              log.info(
                  "[CREDIT_LINE_PROCESS] QC approved for leadId={}, proceeding with status={}",
                  leadId,
                  currentStatus);

              // If already ACTIVE, just log and send callback if required.
              if (CreditLineStatus.ACTIVE.getValue().equals(currentStatus)) {
                log.info(
                    "[CREDIT_LINE_PROCESS] Credit line already ACTIVE for leadId={}, skipping all"
                        + " operations",
                    leadId);
                if (sendPartnerCallback) {
                  return sendCreditLineCallbackToPartner(creditLineEntity, productCode);
                }
                return Mono.just(creditLineEntity);
              }

              // If APPROVED, proceed with Activate only
              if (CreditLineStatus.APPROVED.getValue().equals(currentStatus)) {
                log.info(
                    "[CREDIT_LINE_PROCESS] Credit line already APPROVED for leadId={}, proceeding"
                        + " with Activate only",
                    leadId);
                return processActivate(creditLineEntity, productCode, sendPartnerCallback);
              }

              // If CREATED, proceed with Approve and Activate
              if (CreditLineStatus.CREATED.getValue().equals(currentStatus)) {
                log.info(
                    "[CREDIT_LINE_PROCESS] Credit line already CREATED for leadId={}, proceeding"
                        + " with Approve and Activate",
                    leadId);
                return processApproveAndActivate(
                    creditLineEntity, productCode, sendPartnerCallback);
              }

              // If PENDING, proceed with full flow: Generate → Approve → Activate
              return processGenerateApproveAndActivate(
                  creditLineEntity, productCode, sendPartnerCallback);
            });
  }

  /** Processes full flow: Generate → Approve → Activate for PENDING credit lines. */
  private Mono<Object> processGenerateApproveAndActivate(
      CreditLineEntity creditLineEntity, String productCode, boolean sendPartnerCallback) {

    String leadId = creditLineEntity.getLeadId();

    // Build the generate credit line request
    M2PGenerateCreditLineRequestDTO generateRequest =
        M2PGenerateCreditLineRequestDTO.builder()
            .allowedLimit(creditLineEntity.getCreditLimit().intValue())
            .tenureDetails(
                M2PGenerateCreditLineRequestDTO.TenureDetails.builder()
                    .value(creditLineEntity.getTenureValue())
                    .type(creditLineEntity.getTenureType())
                    .build())
            .agreementIdentifier("BNPL")
            .build();

    // Use existing generateCreditLine method which handles CTA registration and persistence
    return generateCreditLine(generateRequest, leadId, productCode)
        .flatMap(
            generateResponse -> {
              // Update credit line entity with m2p_credit_line_id and CREATED status
              creditLineEntity.setM2pCreditLineId(generateResponse.getAccountNumber());
              creditLineEntity.setStatus(CreditLineStatus.CREATED.getValue());
              creditLineEntity.setLimitCreatedAt(LocalDateTime.now());
              creditLineEntity.setUpdatedAt(LocalDateTime.now());

              log.info(
                  "[CREDIT_LINE_GENERATE] Generated credit line for leadId={}, m2pCreditLineId={}",
                  leadId,
                  generateResponse.getAccountNumber());

              return creditLineRepository.save(creditLineEntity);
            })
        .flatMap(
            savedEntity ->
                processApproveAndActivate(savedEntity, productCode, sendPartnerCallback));
  }

  /** Processes Approve → Activate for CREATED credit lines. */
  private Mono<Object> processApproveAndActivate(
      CreditLineEntity creditLineEntity, String productCode, boolean sendPartnerCallback) {

    String leadId = creditLineEntity.getLeadId();

    // Use existing approveCreditLine method
    return approveCreditLine(leadId, productCode)
        .flatMap(
            approveResponse -> {
              // Update credit line with APPROVED status
              creditLineEntity.setStatus(CreditLineStatus.APPROVED.getValue());
              creditLineEntity.setLimitApprovedAt(LocalDateTime.now());
              creditLineEntity.setUpdatedAt(LocalDateTime.now());

              log.info("[CREDIT_LINE_APPROVE] Approved credit line for leadId={}", leadId);

              return creditLineRepository.save(creditLineEntity);
            })
        .flatMap(savedEntity -> processActivate(savedEntity, productCode, sendPartnerCallback));
  }

  /** Processes Activate only for APPROVED credit lines. */
  private Mono<Object> processActivate(
      CreditLineEntity creditLineEntity, String productCode, boolean sendPartnerCallback) {

    String leadId = creditLineEntity.getLeadId();

    // Use existing activateCreditLine method which handles CTA registration
    return activateCreditLine(leadId, productCode)
        .flatMap(
            activateResponse -> {
              // Update credit line with ACTIVE status
              creditLineEntity.setStatus(CreditLineStatus.ACTIVE.getValue());
              creditLineEntity.setLimitActivatedAt(LocalDateTime.now());
              creditLineEntity.setUpdatedAt(LocalDateTime.now());

              log.info("[CREDIT_LINE_ACTIVATE] Activated credit line for leadId={}", leadId);

              return creditLineRepository.save(creditLineEntity).thenReturn(creditLineEntity);
            })
        .flatMap(
            savedCreditLine -> {
              // Tag the lead for PSL after credit line is saved
              // Error handling ensures flow continues even if tagging fails
              return loanTaggingService
                  .tagLoanForPsl(leadId)
                  .doOnError(
                      e ->
                          log.warn(
                              "[CREDIT_LINE_ACTIVATE] PSL tagging failed for leadId={}, error={}",
                              leadId,
                              e.getMessage()))
                  .onErrorResume(e -> Mono.empty())
                  .then(
                      Mono.deferContextual(
                          parentCtx ->
                              Mono.fromRunnable(
                                  () -> {
                                    Function<Context, Context> propagateParent =
                                        inner -> {
                                          Context out = inner;
                                          if (parentCtx.hasKey(TRACE_ID)) {
                                            out = out.put(TRACE_ID, parentCtx.get(TRACE_ID));
                                          }
                                          if (parentCtx.hasKey(PARTNER_ID)) {
                                            out = out.put(PARTNER_ID, parentCtx.get(PARTNER_ID));
                                          }
                                          return out;
                                        };

                                    creditLineLimitApprovedNotificationService
                                        .sendLimitApprovedNotificationIfConfigured(
                                            leadId, savedCreditLine)
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .contextWrite(propagateParent)
                                        .subscribe(
                                            null,
                                            e ->
                                                log.warn(
                                                    "[CREDIT_LINE_ACTIVATE] async limit-approved"
                                                        + " notification failed leadId={},"
                                                        + " error={}",
                                                    leadId,
                                                    e.getMessage()));

                                    m2pFacadeService
                                        .triggerRiskProcess(leadId)
                                        .subscribeOn(Schedulers.parallel())
                                        .contextWrite(propagateParent)
                                        .subscribe(
                                            null,
                                            e ->
                                                log.warn(
                                                    "[CREDIT_LINE_ACTIVATE] async risk process"
                                                        + " failed leadId={}, error={}",
                                                    leadId,
                                                    e.getMessage()));
                                  })))
                  .thenReturn(savedCreditLine);
            })
        .flatMap(
            updatedCreditLine -> {
              if (sendPartnerCallback) {
                return sendCreditLineCallbackToPartner(updatedCreditLine, productCode);
              }
              return Mono.just(updatedCreditLine);
            });
  }

  /** Sends credit line activation callback to partner. */
  private Mono<Object> sendCreditLineCallbackToPartner(
      CreditLineEntity creditLineEntity, String productCode) {

    // Build callback DTO to send to partner
    CreditLineCallbackToPartnerDTO callbackDto =
        CreditLineCallbackToPartnerDTO.builder()
            .limitId(creditLineEntity.getM2pCreditLineId())
            .limit(creditLineEntity.getCreditLimit().intValue())
            .tenureDetails(
                CreditLineCallbackToPartnerDTO.TenureDetails.builder()
                    .value(creditLineEntity.getTenureValue())
                    .type(creditLineEntity.getTenureType())
                    .build())
            .status(creditLineEntity.getStatus())
            .leadId(creditLineEntity.getLeadId())
            .build();

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              String partnerCode = productControlConfigData.getT1();
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), CREDIT_LINE_STATUS_CALLBACK_IDENTIFIER);

              if (Objects.isNull(flowData)) {
                log.warn(
                    "[CREDIT_LINE_CALLBACK] Flow data not found for identifier={}, productCode={}",
                    CREDIT_LINE_STATUS_CALLBACK_IDENTIFIER,
                    productCode);
                return Mono.just(callbackDto);
              }

              // Create callback log entity
              CallbackLogEntity callback =
                  CallbackLogEntity.builder()
                      .type(CREDIT_LINE_STATUS_CALLBACK_IDENTIFIER)
                      .request(Json.of(gson.toJson(callbackDto)))
                      .referenceId(creditLineEntity.getLeadId())
                      .productCode(productCode)
                      .createdAt(LocalDateTime.now())
                      .isRetry(false)
                      .build();

              log.info(
                  "[CREDIT_LINE_CALLBACK] Sending callback to partner for leadId={}, partnerUri={}",
                  creditLineEntity.getLeadId(),
                  flowData.getPartnerUri());

              return partnerApi
                  .registerPartnerCallback(
                      callbackDto,
                      flowData.getPartnerUri(),
                      flowData.getCallMethod(),
                      partnerCode,
                      flowData.getRetryCount(),
                      CREDIT_LINE_STATUS_CALLBACK_IDENTIFIER)
                  .flatMap(
                      response -> {
                        callback.setResponse(Json.of(gson.toJson(response)));
                        return callbackStoreService.save(callback).thenReturn(response);
                      })
                  .doOnSuccess(
                      response ->
                          log.info(
                              "[CREDIT_LINE_CALLBACK] Successfully sent callback to partner for"
                                  + " leadId={}",
                              creditLineEntity.getLeadId()))
                  .onErrorResume(
                      error -> {
                        log.error(
                            "[CREDIT_LINE_CALLBACK] Failed to send callback to partner for"
                                + " leadId={}, error={}",
                            creditLineEntity.getLeadId(),
                            error.getMessage());
                        callback.setException(error.getMessage());
                        return callbackStoreService.save(callback).thenReturn(callbackDto);
                      });
            });
  }
}
