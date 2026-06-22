package com.trillionloans.los.service.drawdownorchestrator;

import static com.trillionloans.los.constant.StringConstants.AGREEMENT_UPLOAD_LOGGER;
import static com.trillionloans.los.constant.StringConstants.DRAWDOWN_BRE;
import static com.trillionloans.los.constant.StringConstants.DRAWDOWN_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.DRAWDOWN_ORCHESTRATOR_LOGGER;
import static com.trillionloans.los.constant.StringConstants.DRAWDOWN_RISK_DEDUPE_ERROR;
import static com.trillionloans.los.constant.StringConstants.FETCH_LEAD_ID_LOGGER;
import static com.trillionloans.los.constant.StringConstants.MANUAL;
import static com.trillionloans.los.constant.StringConstants.PERSIST_DRAWDOWN_LOGGER;
import static com.trillionloans.los.model.response.creditline.DrawdownBreResponse.Eligibility.ELIGIBLE;

import com.google.gson.Gson;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.PartnerApi;
import com.trillionloans.los.constant.DisbursalStatus;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.exception.drawdown.CreditLineNotFoundException;
import com.trillionloans.los.exception.drawdown.DrawdownAgreementUploadException;
import com.trillionloans.los.exception.drawdown.DrawdownBreRejectedException;
import com.trillionloans.los.exception.drawdown.DrawdownDocumentPersistenceException;
import com.trillionloans.los.exception.drawdown.DrawdownNotEligibleException;
import com.trillionloans.los.exception.drawdown.DrawdownNotFoundException;
import com.trillionloans.los.exception.drawdown.DrawdownRiskRejectedException;
import com.trillionloans.los.exception.drawdown.DrawdownValidationException;
import com.trillionloans.los.exception.drawdown.DrawdownVerificationException;
import com.trillionloans.los.model.dto.DrawdownInternalRequest;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.*;
import com.trillionloans.los.model.request.BulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.DrawdownApproveRequest;
import com.trillionloans.los.model.request.DrawdownRejectRequest;
import com.trillionloans.los.model.request.DrawdownRequest;
import com.trillionloans.los.model.request.InvoiceData;
import com.trillionloans.los.model.request.m2p.M2PDrawdownApproveRequest;
import com.trillionloans.los.model.request.m2p.M2PDrawdownRejectRequest;
import com.trillionloans.los.model.request.m2p.M2PDrawdownRequest;
import com.trillionloans.los.model.response.DrawdownInternalResponse;
import com.trillionloans.los.model.response.DrawdownPartnerCallbackDTO;
import com.trillionloans.los.model.response.DrawdownResponse;
import com.trillionloans.los.model.response.EnrichedDrawdownInternalResponse;
import com.trillionloans.los.model.response.InvoiceResponse;
import com.trillionloans.los.model.response.creditline.DrawdownBreResponse;
import com.trillionloans.los.model.response.m2p.DrawdownCallbackDetailsDTO;
import com.trillionloans.los.model.response.m2p.M2PDrawdownResponse;
import com.trillionloans.los.model.response.m2p.M2pDocumentsUploadResponseDTO;
import com.trillionloans.los.repository.AnchorMasterRepository;
import com.trillionloans.los.repository.CreditLineRepository;
import com.trillionloans.los.repository.drawdown.DrawdownAdditionalDetailsRepository;
import com.trillionloans.los.repository.drawdown.DrawdownInvoiceMappingRepository;
import com.trillionloans.los.repository.drawdown.DrawdownRepository;
import com.trillionloans.los.service.CreditLineLimitApprovedNotificationService;
import com.trillionloans.los.service.FunnelRuleService;
import com.trillionloans.los.service.LoanApplicationCacheService;
import com.trillionloans.los.service.LoanApplicationService;
import com.trillionloans.los.service.db.CallbackStoreService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.disbursal.DisbursalService;
import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawdownService {
  private final Gson gson;
  private final PartnerApi partnerApi;
  private final M2PWrapperApi m2PWrapperApi;
  private final DrawdownBreService drawdownBreService;
  private final FunnelRuleService funnelRuleService;
  private final DrawdownRepository drawdownRepository;
  private final CreditLineRepository creditLineRepository;
  private final CallbackStoreService callbackStoreService;
  private final AnchorMasterRepository anchorMasterRepository;
  private final DrawdownInvoiceMappingRepository mappingRepository;
  private final ProductConfigMasterService productConfigMasterService;
  private final DrawdownAdditionalDetailsRepository additionalDetailsRepository;
  private final LoanApplicationService loanApplicationService;
  private final LoanApplicationCacheService loanApplicationCacheService;
  private final DisbursalService disbursalService;
  private final DrawdownDocumentService drawdownDocumentService;
  private final CreditLineLimitApprovedNotificationService
      creditLineLimitApprovedNotificationService;

  /**
   * Invoice-backed drawdown entrypoint (invoices already validated and persisted upstream).
   *
   * <p>(1) Saves the drawdown row and links it to the given invoices. (2) Runs the execution
   * pipeline: funnel/risk dedupe → BRE → agreement upload → fire-and-forget invoice document refs →
   * M2P drawdown trigger → status/disbursal side effects. See {@link #processDrawdownExecution}.
   */
  public Mono<DrawdownResponse> initiateDrawdownExecution(
      DrawdownInternalRequest internalReq,
      List<InvoiceResponse> processedInvoices,
      DrawdownRequest request,
      String lineId,
      String partnerId,
      String productCode) {

    return persistDrawdownAndDrawdownInvoiceMapping(internalReq, processedInvoices, lineId)
        .flatMap(
            drawdownInternalResponse -> {
              log.info(
                  "[{}][{}}] Drawdown persisted successfully." + " DrawdownID: {}",
                  DRAWDOWN_ORCHESTRATOR_LOGGER,
                  PERSIST_DRAWDOWN_LOGGER,
                  drawdownInternalResponse.getDrawdown().getId());

              return processDrawdownExecution(
                  drawdownInternalResponse,
                  request,
                  internalReq,
                  lineId,
                  partnerId,
                  productCode,
                  processedInvoices);
            });
  }

  /**
   * Persists drawdown (no invoices), then executes the full drawdown flow. Used for PRODUCT_KCL
   * (standalone drawdowns without invoice validation).
   */
  public Mono<DrawdownResponse> persistAndExecuteDrawdown(
      DrawdownInternalRequest internalReq,
      DrawdownRequest request,
      String lineId,
      String partnerId,
      String productCode) {

    return saveDrawdown(internalReq, lineId)
        .doOnNext(
            internalResp ->
                log.info(
                    "[DRAWDOWN_ORCHESTRATOR][PERSIST_DRAWDOWN] Drawdown persisted successfully."
                        + " DrawdownID: {}",
                    internalResp.getDrawdown().getId()))
        .flatMap(
            internalResp ->
                processDrawdownExecution(
                    internalResp, request, internalReq, lineId, partnerId, productCode, null));
  }

  /**
   * Executes the drawdown pipeline: Funnel/Risk dedupe → BRE → agreement upload (then
   * fire-and-forget invoice document refs) → M2P trigger. On error, updates status to FAILED unless
   * already set (e.g. RISK_REJECTED, BRE_REJECTED).
   *
   * @param internalResp persisted drawdown with internal response
   * @param request original request
   * @param internalReq internal request with payment details
   * @param lineId credit line ID
   * @param partnerId partner ID
   * @param productCode product code
   * @param processedInvoices persisted invoice ids in request order; used to fire-and-forget
   *     invoice document refs after agreement upload succeeds (null for standalone drawdown)
   * @return DrawdownResponse or propagates
   *     DrawdownRiskRejectedException/DrawdownBreRejectedException
   */
  public Mono<DrawdownResponse> processDrawdownExecution(
      DrawdownInternalResponse internalResp,
      DrawdownRequest request,
      DrawdownInternalRequest internalReq,
      String lineId,
      String partnerId,
      String productCode,
      List<InvoiceResponse> processedInvoices) {

    return getLeadIdFromCacheOrDb(lineId)
        .flatMap(
            leadId ->
                funnelRuleService
                    .applyFunnelLogicForDrawdown(lineId, leadId)
                    .flatMap(
                        validationResult -> {
                          log.info(
                              "Funnel validation for lineId {} returned isValid={}, clientId={}",
                              lineId,
                              validationResult.isValid(),
                              validationResult.getClientId());

                          if (!validationResult.isValid()) {
                            log.warn(
                                "[DRAWDOWN][DRAWDOWN_RISK_DEDUPE_ERROR] Drawdown rejected as per"
                                    + " the risk dedupe framework for the lineId: {}",
                                lineId);
                            return updateDrawdownStatusWithTimestamp(
                                    internalResp.getDrawdown(),
                                    null,
                                    Drawdown.DrawdownStatus.RISK_REJECTED)
                                .then(
                                    Mono.error(
                                        new DrawdownRiskRejectedException(
                                            DRAWDOWN_RISK_DEDUPE_ERROR)));
                          }

                          String clientId = validationResult.getClientId();

                          return triggerDrawdownBre(
                                  request,
                                  internalResp,
                                  lineId,
                                  clientId,
                                  leadId,
                                  partnerId,
                                  productCode)
                              .then(
                                  uploadAgreementIfPresent(
                                          request,
                                          lineId,
                                          productCode,
                                          internalResp.getDrawdown().getId(),
                                          partnerId)
                                      .doOnSuccess(
                                          unused ->
                                              scheduleInvoiceDocumentsFireAndForget(
                                                  processedInvoices,
                                                  internalReq,
                                                  lineId,
                                                  partnerId)))
                              .then(
                                  Mono.defer(
                                      () -> triggerDrawdown(internalReq, internalResp, lineId)));
                        }))
        .flatMap(
            enrichedResponse -> {
              log.info(
                  "[{}][TRIGGER_DRAWDOWN] External drawdown trigger successful: {}",
                  DRAWDOWN_ORCHESTRATOR_LOGGER,
                  enrichedResponse);

              // update status to OPS_APPROVAL_PENDING, then save to disbursal registry
              updateDrawdownStatusToOpsApprovalPending(enrichedResponse)
                  .then(
                      m2PWrapperApi
                          .getDisbursalAmountScf(
                              enrichedResponse.getM2PDrawdownResponse().getTransactionId())
                          .next()
                          .flatMap(
                              disbursalAmountResponse -> {
                                return disbursalService.saveDisbursalRegistryEntityWithAnchor(
                                    null,
                                    enrichedResponse.getM2PDrawdownResponse().getTransactionId(),
                                    lineId,
                                    productCode,
                                    request.getAnchorId(),
                                    MANUAL,
                                    DisbursalStatus.MANUAL_INI,
                                    disbursalAmountResponse.getGrossDisbursalAmount(),
                                    disbursalAmountResponse.getNetDisbursalAmount());
                              })
                          .switchIfEmpty(
                              disbursalService.saveDisbursalRegistryEntityWithAnchor(
                                  null,
                                  enrichedResponse.getM2PDrawdownResponse().getTransactionId(),
                                  lineId,
                                  productCode,
                                  request.getAnchorId(),
                                  MANUAL,
                                  DisbursalStatus.MANUAL_INI,
                                  null,
                                  null)))
                  .subscribeOn(Schedulers.boundedElastic())
                  .subscribe(
                      savedEntity ->
                          log.info(
                              "[DRAWDOWN_SERVICE] disbursal registry saved for transactionId: {}",
                              enrichedResponse.getM2PDrawdownResponse().getTransactionId()),
                      error ->
                          log.error(
                              "[DRAWDOWN_SERVICE] failed to save disbursal registry for"
                                  + " transactionId: {}: {}",
                              enrichedResponse.getM2PDrawdownResponse().getTransactionId(),
                              error.getMessage()));

              return DrawdownUtil.mapToDrawdownResponse(enrichedResponse)
                  .doOnNext(
                      drawdownResponse ->
                          log.info(
                              "[DRAWDOWN_ORCHESTRATOR] Drawdown submitted for OPS approval. {}",
                              drawdownResponse));
            })
        .onErrorResume(
            error -> {
              // Do not overwrite status when Risk/BRE rejection already set it
              boolean isRiskRejected = error instanceof DrawdownRiskRejectedException;
              boolean isBreRejected = error instanceof DrawdownBreRejectedException;

              if (!isRiskRejected && !isBreRejected) {
                updateDrawdownStatus(
                        internalResp.getDrawdown(),
                        StringUtils.EMPTY,
                        Drawdown.DrawdownStatus.FAILED)
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnError(
                        dbErr ->
                            log.error(
                                "[DB_ERROR] Failed to update status to FAILED: {}",
                                internalResp.getDrawdownId(),
                                dbErr.getMessage(),
                                dbErr))
                    .subscribe();
              }

              return Mono.error(error);
            });
  }

  /**
   * Uploads the drawdown agreement document if present in the request. Skips if no agreement is
   * provided.
   *
   * @param request the drawdown request containing optional agreement
   * @param lineId the credit line ID
   * @param productCode the product code
   * @param drawdownId persisted drawdown id
   * @param partnerId partner id for persisting document rows
   * @return Mono<Void> that completes on success or errors on failure
   */
  private Mono<Void> uploadAgreementIfPresent(
      DrawdownRequest request,
      String lineId,
      String productCode,
      Long drawdownId,
      String partnerId) {
    return Mono.defer(
        () -> {
          BulkDocumentsUploadRequest agreement = request.getDrawdownData().getDrawdownAgreement();

          if (agreement == null) {
            log.info("[{}] No agreement found. Skipping.", AGREEMENT_UPLOAD_LOGGER);
            return Mono.empty();
          }

          log.info("[{}] UPLOAD_START for lineId: {}", AGREEMENT_UPLOAD_LOGGER, lineId);

          return getLeadIdFromCacheOrDb(lineId)
              .switchIfEmpty(
                  Mono.defer(
                      () -> {
                        log.warn(
                            "[{}] EMPTY: Lead ID not found for {}",
                            AGREEMENT_UPLOAD_LOGGER,
                            lineId);
                        return Mono.empty();
                      }))
              .flatMap(
                  leadId -> {
                    log.info(
                        "[{}] Uploading agreement for for leadId: {}, lineId: {}",
                        AGREEMENT_UPLOAD_LOGGER,
                        leadId,
                        lineId);

                    return loanApplicationService
                        .uploadDocumentsAgainstLoan(agreement, leadId, productCode)
                        .cast(M2pDocumentsUploadResponseDTO.class)
                        .doOnSuccess(
                            response ->
                                log.info(
                                    "[{}] Agreement uploaded successfully for leadId: {}",
                                    AGREEMENT_UPLOAD_LOGGER,
                                    leadId))
                        .onErrorResume(error -> mapAgreementUploadFailureToError(lineId, error))
                        .flatMap(
                            uploadResponse ->
                                drawdownDocumentService.saveDrawdownAgreementDocumentReferences(
                                    drawdownId,
                                    agreement,
                                    lineId,
                                    productCode,
                                    partnerId,
                                    uploadResponse));
                  })
              .then();
        });
  }

  /**
   * Persists invoice document references (S3 snapshot + rows) without blocking the drawdown
   * pipeline. Invoked from {@code doOnSuccess} after {@link #uploadAgreementIfPresent} completes
   * successfully (including when there is no agreement to upload). Never throws; failures are
   * logged. Does not affect the drawdown Mono pipeline (errors are consumed on the async chain and
   * in catch blocks).
   */
  private void scheduleInvoiceDocumentsFireAndForget(
      List<InvoiceResponse> processedInvoices,
      DrawdownInternalRequest internalReq,
      String lineId,
      String partnerId) {
    try {
      if (processedInvoices == null || processedInvoices.isEmpty() || internalReq == null) {
        return;
      }
      List<InvoiceData> invoiceDataList = internalReq.getInvoiceData();
      if (invoiceDataList == null || invoiceDataList.isEmpty()) {
        return;
      }
      int n = Math.min(processedInvoices.size(), invoiceDataList.size());
      if (n != processedInvoices.size() || n != invoiceDataList.size()) {

        log.warn(
            "[DRAWDOWN_DOCUMENTS][INVOICE] Invoice response count ({}) and invoiceData count ({})"
                + " differ; persisting refs for first {} entries only.",
            processedInvoices.size(),
            invoiceDataList.size(),
            n);
      }
      Flux.range(0, n)
          .concatMap(
              i ->
                  Mono.defer(
                      () -> {
                        try {
                          InvoiceResponse inv = processedInvoices.get(i);
                          if (inv == null) {
                            log.warn(
                                "[DRAWDOWN_DOCUMENTS][INVOICE] Skipping index {}: null"
                                    + " InvoiceResponse.",
                                i);
                            return Mono.empty();
                          }
                          Long invoiceId = inv.getId();
                          InvoiceData data = invoiceDataList.get(i);
                          if (invoiceId == null || data == null) {
                            log.warn(
                                "[DRAWDOWN_DOCUMENTS][INVOICE] Skipping index {}: invoiceId or"
                                    + " payload null.",
                                i);
                            return Mono.empty();
                          }
                          return drawdownDocumentService
                              .saveInvoiceDocumentReferences(invoiceId, data, partnerId, lineId)
                              .onErrorResume(
                                  err -> {
                                    log.error(
                                        "[DRAWDOWN_DOCUMENTS][INVOICE] Fire-and-forget save failed"
                                            + " for invoiceId {}: {}",
                                        invoiceId,
                                        err.getMessage(),
                                        err);
                                    return Mono.empty();
                                  });
                        } catch (RuntimeException ex) {
                          log.error(
                              "[DRAWDOWN_DOCUMENTS][INVOICE] Unexpected error building save for"
                                  + " index {}",
                              i,
                              ex);
                          return Mono.empty();
                        }
                      }))
          .then()
          .onErrorResume(
              err -> {
                log.error(
                    "[DRAWDOWN_DOCUMENTS][INVOICE] Fire-and-forget chain failed (consumed)", err);
                return Mono.empty();
              })
          .subscribeOn(Schedulers.boundedElastic())
          .subscribe(
              unused -> {},
              err ->
                  log.error(
                      "[DRAWDOWN_DOCUMENTS][INVOICE] Fire-and-forget terminal error (consumed)",
                      err));
    } catch (Throwable t) {
      log.error(
          "[DRAWDOWN_DOCUMENTS][INVOICE] Failed to schedule invoice document persistence"
              + " (consumed)",
          t);
    }
  }

  private Mono<M2pDocumentsUploadResponseDTO> mapAgreementUploadFailureToError(
      String lineId, Throwable error) {
    if (error instanceof DrawdownDocumentPersistenceException) {
      return Mono.error(error);
    }

    String message =
        DrawdownUtil.extractAgreementUploadErrorMessage(
            error, gson, "Agreement upload failed. Please check document format and try again.");
    Object clientResponse = null;
    HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;

    if (error instanceof ClientSideException clientEx) {
      clientResponse = clientEx.getResponseBody();
      statusCode = clientEx.getHttpStatusCode();
    } else if (error instanceof ServerErrorException serverEx) {
      clientResponse = serverEx.getClientResponse();
      statusCode = HttpStatus.BAD_GATEWAY;
    } else if (error instanceof com.trillionloans.los.exception.BaseException baseEx) {
      clientResponse = baseEx.getClientResponse();
      statusCode = baseEx.getHttpStatusCode();
    }

    log.error(
        "[{}] Agreement upload failed for lineId: {}. Error: {}",
        AGREEMENT_UPLOAD_LOGGER,
        lineId,
        message,
        error);

    return Mono.error(new DrawdownAgreementUploadException(message, clientResponse, statusCode));
  }

  /**
   * Resolves leadId for a credit line: cache first, then DB fallback. Required for BRE, funnel
   * rules, and agreement upload.
   *
   * @param lineId the M2P credit line ID
   * @return Mono containing the leadId, or CreditLineNotFoundException if not found
   */
  public Mono<String> getLeadIdFromCacheOrDb(String lineId) {
    return loanApplicationCacheService
        .getCreditLinePartner(lineId)
        .doOnNext(
            entity ->
                log.info(
                    "[{}] Cache lookup result for lineId: {}, leadId: {}",
                    FETCH_LEAD_ID_LOGGER,
                    lineId,
                    entity.getLeadId()))
        .filter(entity -> StringUtils.isNotBlank(entity.getLeadId()))
        .map(CreditLinePartnerEntity::getLeadId)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[{}] LeadId not found in cache (or null/blank). Fetching from DB for"
                          + " lineId: {}",
                      FETCH_LEAD_ID_LOGGER,
                      lineId);

                  return creditLineRepository
                      .findByM2pCreditLineId(lineId)
                      .doOnNext(
                          entity ->
                              log.info(
                                  "[{}] DB lookup result for lineId: {}, leadId: {}",
                                  FETCH_LEAD_ID_LOGGER,
                                  lineId,
                                  entity.getLeadId()))
                      .filter(entity -> StringUtils.isNotBlank(entity.getLeadId()))
                      .map(CreditLineEntity::getLeadId)
                      .doOnNext(
                          leadId ->
                              log.info(
                                  "[{}] Successfully resolved leadId: {} for lineId: {}",
                                  FETCH_LEAD_ID_LOGGER,
                                  leadId,
                                  lineId))
                      .switchIfEmpty(
                          Mono.defer(
                              () -> {
                                log.error(
                                    "[{}] Credit line or leadId not found for lineId: {}",
                                    FETCH_LEAD_ID_LOGGER,
                                    lineId);
                                return Mono.error(
                                    new CreditLineNotFoundException(
                                        "Credit line or leadId not found for lineId: " + lineId));
                              }));
                }))
        .doOnError(
            error ->
                log.error(
                    "[{}] Error resolving leadId for lineId: {}. Error: {}",
                    FETCH_LEAD_ID_LOGGER,
                    lineId,
                    error.getMessage(),
                    error));
  }

  /**
   * Triggers BRE for drawdown eligibility. Updates drawdown status to BRE_APPROVED or BRE_REJECTED.
   * Throws DrawdownBreRejectedException when NOT_ELIGIBLE.
   */
  public Mono<?> triggerDrawdownBre(
      DrawdownRequest request,
      DrawdownInternalResponse drawdownInternalResponse,
      String lineId,
      String clientId,
      String leadId,
      String partnerId,
      String productCode) {
    return drawdownBreService
        .triggerDrawdownBre(
            request,
            lineId,
            clientId,
            leadId,
            partnerId,
            productCode,
            drawdownInternalResponse.getDrawdownId())
        .flatMap(
            breResponse -> {
              log.info("{} BRE Engine call successful for lineId: {}", DRAWDOWN_BRE, lineId);

              return updateDrawdownStatusAsPerBre(
                      drawdownInternalResponse.getDrawdownId(), breResponse)
                  .then(
                      Mono.defer(
                          () -> {
                            if (!ELIGIBLE.equals(breResponse.getStatus())) {
                              return Mono.error(
                                  new DrawdownBreRejectedException(
                                      "BRE Rejected. LineId: " + lineId));
                            }
                            return Mono.just(breResponse);
                          }))
                  .doOnSuccess(
                      resp ->
                          log.info("{} BRE summary record processed successfully", DRAWDOWN_BRE));
            })
        .doOnError(
            e -> log.error("{} Error in BRE trigger flow: {}", DRAWDOWN_BRE, e.getMessage(), e));
  }

  /**
   * Triggers drawdown at M2P vendor. Maps request to M2P format, calls API, enriches response.
   *
   * @param request internal request with payment details
   * @param drawdownInternalResponse persisted drawdown
   * @param loanAccountNumber M2P loan account number
   * @return EnrichedDrawdownInternalResponse with M2P transactionId and processing status
   */
  public Mono<EnrichedDrawdownInternalResponse> triggerDrawdown(
      DrawdownInternalRequest request,
      DrawdownInternalResponse drawdownInternalResponse,
      String loanAccountNumber) {

    String drawdownId = drawdownInternalResponse.getDrawdownId();
    log.info(
        "[DRAWDOWN] Initiating M2P trigger for DrawdownId: {}, LoanAccount: {}",
        drawdownId,
        loanAccountNumber);

    return Mono.just(drawdownInternalResponse)
        .flatMap(body -> DrawdownUtil.mapToM2PRequest(request, body.getDrawdownId()))
        .flatMap(
            m2pDrawdownRequest ->
                triggerVendorDrawdown(loanAccountNumber, m2pDrawdownRequest, drawdownId))
        .flatMap(
            m2pDrawdownResponse ->
                DrawdownUtil.mapToEnrichedDrawdownInternalResponse(
                    drawdownInternalResponse, m2pDrawdownResponse))
        .doOnError(
            error ->
                log.error(
                    "[DRAWDOWN] Error in drawdown flow for DrawdownId: {}. Error: {}",
                    drawdownId,
                    error.getMessage(),
                    error));
  }

  private Mono<M2PDrawdownResponse> triggerVendorDrawdown(
      String loanAccountNumber, M2PDrawdownRequest m2pDrawdownRequest, String drawdownId) {
    return m2PWrapperApi
        .triggerDrawdown(loanAccountNumber, m2pDrawdownRequest)
        .doOnSuccess(
            response ->
                log.info("[DRAWDOWN] Vendor call successful for drawdownId: {}", drawdownId))
        .onErrorResume(
            error -> {
              log.error(
                  "[DRAWDOWN] Error in M2P drawdown flow for drawdownId: {}. Error: {}",
                  drawdownId,
                  error.getMessage(),
                  error);

              return Mono.error(error);
            });
  }

  /**
   * Persists drawdown entity and links it to invoices via drawdown_invoice_mappings. Used for
   * invoice-backed drawdowns (PRODUCT_FUND).
   *
   * @param request drawdown request
   * @param processedInvoices validated invoice responses
   * @param lineId credit line ID
   * @return DrawdownInternalResponse with saved drawdown and invoice IDs
   */
  @Transactional
  public Mono<DrawdownInternalResponse> persistDrawdownAndDrawdownInvoiceMapping(
      DrawdownRequest request, List<InvoiceResponse> processedInvoices, String lineId) {

    return DrawdownUtil.mapToDrawdownEntity(
            request,
            Drawdown.DrawdownStatus.INIT,
            request.getPartnerId(),
            request.getAnchorId(),
            lineId)
        .flatMap(drawdownRepository::save)
        .flatMap(
            savedDrawdown -> {
              List<String> invoiceIds =
                  processedInvoices.stream()
                      .map(inv -> inv.getId().toString())
                      .collect(Collectors.toList());

              return saveInvoiceMappings(savedDrawdown.getId(), invoiceIds)
                  .then(
                      DrawdownUtil.mapToDrawdownInternalResponse(
                          savedDrawdown,
                          processedInvoices,
                          request.getPartnerId(),
                          request.getAnchorId()));
            });
  }

  /** Persists drawdown without invoice mappings. Used for standalone drawdowns (PRODUCT_KCL). */
  public Mono<DrawdownInternalResponse> saveDrawdown(DrawdownRequest request, String lineId) {

    return DrawdownUtil.mapToDrawdownEntity(
            request,
            Drawdown.DrawdownStatus.INIT,
            request.getPartnerId(),
            request.getAnchorId(),
            lineId)
        .flatMap(drawdownRepository::save)
        .flatMap(
            savedEntity ->
                DrawdownUtil.mapToDrawdownInternalResponse(
                    savedEntity, new ArrayList<>(), request.getPartnerId(), request.getAnchorId()));
  }

  /**
   * Updates drawdown to OPS_APPROVAL_PENDING after successful M2P trigger. Sets transactionId from
   * M2P response and opsApprovalPendingAt timestamp.
   */
  public Mono<Void> updateDrawdownStatusToOpsApprovalPending(
      EnrichedDrawdownInternalResponse enrichedDrawdownInternalResponse) {
    Drawdown drawdown =
        enrichedDrawdownInternalResponse.getDrawdownInternalResponse().getDrawdown();
    String transactionId =
        enrichedDrawdownInternalResponse.getM2PDrawdownResponse().getTransactionId();

    return updateDrawdownStatusWithTimestamp(
            drawdown, transactionId, Drawdown.DrawdownStatus.OPS_APPROVAL_PENDING)
        .doOnSuccess(
            savedDrawdown ->
                log.info(
                    "Successfully updated drawdownId {} to OPS_APPROVAL_PENDING",
                    savedDrawdown.getId()))
        .doOnError(
            e ->
                log.error(
                    "Failed to update drawdownId status to OPS_APPROVAL_PENDING for {}: {}",
                    drawdown.getId(),
                    e.getMessage(),
                    e))
        .then();
  }

  /** Updates drawdown status based on BRE response: BRE_APPROVED or BRE_REJECTED. */
  public Mono<Void> updateDrawdownStatusAsPerBre(
      String drawdownId, DrawdownBreResponse drawdownBreResponse) {

    return drawdownRepository
        .findById(drawdownId)
        .switchIfEmpty(
            Mono.error(
                new DrawdownNotFoundException("Drawdown record not found for ID: " + drawdownId)))
        .flatMap(
            drawdown -> {
              boolean isEligible =
                  drawdownBreResponse != null
                      && drawdownBreResponse.getStatus()
                          == DrawdownBreResponse.Eligibility.ELIGIBLE;

              Drawdown.DrawdownStatus targetStatus =
                  isEligible
                      ? Drawdown.DrawdownStatus.BRE_APPROVED
                      : Drawdown.DrawdownStatus.BRE_REJECTED;

              return updateDrawdownStatusWithTimestamp(drawdown, null, targetStatus)
                  .doOnSuccess(
                      saved ->
                          log.info(
                              "[BRE-UPDATE] Updated drawdownId {} to status {}",
                              drawdownId,
                              targetStatus))
                  .doOnError(
                      e ->
                          log.error(
                              "[BRE-UPDATE] Failed to update status for {}: {}",
                              drawdownId,
                              e.getMessage(),
                              e))
                  .then();
            });
  }

  /** Updates drawdown status and transactionId. Does not set timestamps. */
  public Mono<Drawdown> updateDrawdownStatus(
      Drawdown drawdown, String transactionId, Drawdown.DrawdownStatus status) {
    drawdown.setStatus(status);
    drawdown.setTransactionId(transactionId);
    return drawdownRepository.save(drawdown);
  }

  /** Creates drawdown_invoice_mappings records linking drawdown to invoices. */
  public Mono<Void> saveInvoiceMappings(Long drawdownId, List<String> invoiceIds) {
    return Flux.fromIterable(invoiceIds)
        .map(
            invId ->
                DrawdownInvoiceMapping.builder()
                    .drawdownId(drawdownId)
                    .invoiceId(Long.parseLong(invId))
                    .build())
        .collectList()
        .flatMapMany(mappingRepository::saveAll)
        .then();
  }

  // ==================== DRAWDOWN APPROVAL FLOW ====================

  /**
   * Approves a drawdown that is in OPS_APPROVAL_PENDING state.
   *
   * <p>Flow: 1. Fetch drawdown by transactionId 2. Validate eligibility (must be in
   * OPS_APPROVAL_PENDING state) 3. If already approved (SUCCESS), skip M2P call and just send
   * callback 4. Call M2P approve API 5. On success: update drawdown status to SUCCESS, fetch
   * disbursal details from report API 6. Persist additional details 7. Send callback to partner
   *
   * @param transactionId the M2P transaction ID
   * @param request the approval request containing payment details
   * @param productCode the product code for callback configuration
   * @return the partner callback DTO
   */
  public Mono<DrawdownPartnerCallbackDTO> approveDrawdown(
      String transactionId, DrawdownApproveRequest request, String productCode) {

    log.info(
        "[DRAWDOWN_APPROVE] Starting approval for transactionId: {}, productCode: {}",
        transactionId,
        productCode);

    return drawdownRepository
        .findByTransactionId(transactionId)
        .switchIfEmpty(
            Mono.error(
                new DrawdownNotFoundException(
                    "Drawdown not found for transactionId: " + transactionId)))
        .flatMap(
            drawdown -> {
              // Check if already approved
              if (drawdown.getStatus() == Drawdown.DrawdownStatus.SUCCESS) {
                log.info(
                    "[DRAWDOWN_APPROVE] Drawdown already approved for transactionId: {}. Proceeding"
                        + " to callback.",
                    transactionId);
                return processAlreadyApprovedDrawdown(drawdown, request, productCode);
              }

              // Validate eligibility
              if (drawdown.getStatus() != Drawdown.DrawdownStatus.OPS_APPROVAL_PENDING) {
                log.error(
                    "[DRAWDOWN_APPROVE] Invalid status for approval. Current status: {},"
                        + " transactionId: {}",
                    drawdown.getStatus(),
                    transactionId);
                return Mono.error(
                    new DrawdownNotEligibleException(
                        "Drawdown is not eligible for approval. Current status: "
                            + drawdown.getStatus()));
              }

              return processDrawdownApproval(drawdown, request, productCode);
            });
  }

  /**
   * Processes approval for a drawdown that is already in SUCCESS state. Skips M2P call and directly
   * processes callback.
   */
  private Mono<DrawdownPartnerCallbackDTO> processAlreadyApprovedDrawdown(
      Drawdown drawdown, DrawdownApproveRequest request, String productCode) {

    return additionalDetailsRepository
        .findByDrawdownId(drawdown.getId())
        .flatMap(
            existingDetails -> buildAndSendPartnerCallback(drawdown, existingDetails, productCode))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  // Additional details don't exist yet, fetch and save them
                  return fetchAndSaveAdditionalDetails(drawdown, request.getReferenceNumber())
                      .flatMap(
                          details -> buildAndSendPartnerCallback(drawdown, details, productCode));
                }));
  }

  /** Processes the full approval flow for a drawdown in OPS_APPROVAL_PENDING state. */
  private Mono<DrawdownPartnerCallbackDTO> processDrawdownApproval(
      Drawdown drawdown, DrawdownApproveRequest request, String productCode) {

    String lineId = drawdown.getLineId();
    String transactionId = drawdown.getTransactionId();

    log.info(
        "[DRAWDOWN_APPROVE] Calling M2P approve API for lineId: {}, transactionId: {}",
        lineId,
        transactionId);

    // Build M2P request
    M2PDrawdownApproveRequest m2pRequest =
        M2PDrawdownApproveRequest.builder()
            .transactionTime(request.getTransactionTime())
            .notes(request.getNotes())
            .paymentDetails(
                M2PDrawdownApproveRequest.PaymentDetails.builder()
                    .paymentType(request.getPaymentType())
                    .referenceNumber(request.getReferenceNumber())
                    .build())
            .build();

    return m2PWrapperApi
        .approveDrawdown(lineId, transactionId, m2pRequest)
        .flatMap(
            response ->
                Mono.deferContextual(
                    ctx -> {
                      log.info(
                          "[DRAWDOWN_APPROVE] M2P approve API call successful for transactionId:"
                              + " {} and triggering drawdown notification as well",
                          transactionId);
                      triggerDrawdownApprovedNotificationAsync(drawdown, ctx);
                      return Mono.just(response);
                    }))
        .flatMap(
            m2pResponse -> {
              // Update drawdown status to SUCCESS
              return updateDrawdownStatusWithTimestamp(
                      drawdown, transactionId, Drawdown.DrawdownStatus.SUCCESS)
                  .doOnSuccess(
                      updated ->
                          log.info(
                              "[DRAWDOWN_APPROVE] Drawdown status updated to SUCCESS for id: {}",
                              drawdown.getId()));
            })
        .flatMap(
            updatedDrawdown ->
                fetchAndSaveAdditionalDetails(updatedDrawdown, request.getReferenceNumber()))
        .flatMap(
            additionalDetails ->
                buildAndSendPartnerCallback(drawdown, additionalDetails, productCode))
        .onErrorResume(
            error -> {
              log.error(
                  "[DRAWDOWN_APPROVE] Error during approval for transactionId: {}. Error: {}",
                  transactionId,
                  error.getMessage(),
                  error);

              // Check if error indicates transaction is not in WAITING_FOR_APPROVAL state
              // This could mean the transaction was already approved
              if (DrawdownUtil.isNotWaitingForApprovalError(error, gson)) {
                log.info(
                    "[DRAWDOWN_APPROVE] Transaction {} may already be approved. Checking report"
                        + " API...",
                    transactionId);

                return handlePossiblyAlreadyApprovedTransaction(
                    drawdown, request.getReferenceNumber(), productCode);
              }

              return Mono.error(error);
            });
  }

  // ==================== DRAWDOWN REJECTION FLOW ====================

  /**
   * Rejects a drawdown that is in OPS_APPROVAL_PENDING state.
   *
   * <p>Flow: 1. Fetch drawdown by transactionId 2. Validate eligibility (must be in
   * OPS_APPROVAL_PENDING state) 3. Call M2P reject API 4. On success: update drawdown status to
   * OPS_REJECTED 5. Persist rejection reason in additional details 6. Send callback to partner with
   * status "REJECTED"
   *
   * @param transactionId the M2P transaction ID
   * @param request the rejection request containing notes and timestamp
   * @param productCode the product code for callback configuration
   * @return the partner callback DTO
   */
  public Mono<DrawdownPartnerCallbackDTO> rejectDrawdown(
      String transactionId, DrawdownRejectRequest request, String productCode) {

    log.info(
        "[DRAWDOWN_REJECT] Starting rejection for transactionId: {}, productCode: {}",
        transactionId,
        productCode);

    return drawdownRepository
        .findByTransactionId(transactionId)
        .switchIfEmpty(
            Mono.error(
                new DrawdownNotFoundException(
                    "Drawdown not found for transactionId: " + transactionId)))
        .flatMap(
            drawdown -> {
              // Check if already rejected
              if (drawdown.getStatus() == Drawdown.DrawdownStatus.OPS_REJECTED) {
                log.info(
                    "[DRAWDOWN_REJECT] Drawdown already rejected for transactionId: {}. Proceeding"
                        + " to callback.",
                    transactionId);
                return processAlreadyRejectedDrawdown(
                    drawdown, request.getRejectionNotes(), productCode);
              }

              // Validate eligibility - only OPS_APPROVAL_PENDING can be rejected
              if (drawdown.getStatus() != Drawdown.DrawdownStatus.OPS_APPROVAL_PENDING) {
                log.error(
                    "[DRAWDOWN_REJECT] Invalid status for rejection. Current status: {},"
                        + " transactionId: {}",
                    drawdown.getStatus(),
                    transactionId);
                return Mono.error(
                    new DrawdownNotEligibleException(
                        "Drawdown is not eligible for rejection. Current status: "
                            + drawdown.getStatus()));
              }

              return processDrawdownRejection(drawdown, request, productCode);
            });
  }

  /**
   * Processes rejection for a drawdown that is already in OPS_REJECTED state. Skips M2P call and
   * directly processes callback.
   */
  private Mono<DrawdownPartnerCallbackDTO> processAlreadyRejectedDrawdown(
      Drawdown drawdown, String rejectionNotes, String productCode) {

    return additionalDetailsRepository
        .findByDrawdownId(drawdown.getId())
        .flatMap(
            existingDetails ->
                buildAndSendRejectionCallback(drawdown, existingDetails, productCode))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  // Additional details don't exist yet, save them
                  return saveRejectionDetails(drawdown, rejectionNotes)
                      .flatMap(
                          details -> buildAndSendRejectionCallback(drawdown, details, productCode));
                }));
  }

  /** Processes the full rejection flow for a drawdown in OPS_APPROVAL_PENDING state. */
  private Mono<DrawdownPartnerCallbackDTO> processDrawdownRejection(
      Drawdown drawdown, DrawdownRejectRequest request, String productCode) {

    String lineId = drawdown.getLineId();
    String transactionId = drawdown.getTransactionId();

    log.info(
        "[DRAWDOWN_REJECT] Calling M2P reject API for lineId: {}, transactionId: {}",
        lineId,
        transactionId);

    // Build M2P request
    M2PDrawdownRejectRequest m2pRequest =
        M2PDrawdownRejectRequest.builder()
            .transactionTime(request.getTransactionTime())
            .notes(request.getRejectionNotes())
            .build();

    return m2PWrapperApi
        .rejectDrawdown(lineId, transactionId, m2pRequest)
        .doOnSuccess(
            response ->
                log.info(
                    "[DRAWDOWN_REJECT] M2P reject API call successful for transactionId: {}",
                    transactionId))
        .flatMap(
            m2pResponse -> {
              // Update drawdown status to OPS_REJECTED
              return updateDrawdownStatusWithTimestamp(
                      drawdown, transactionId, Drawdown.DrawdownStatus.OPS_REJECTED)
                  .doOnSuccess(
                      updated ->
                          log.info(
                              "[DRAWDOWN_REJECT] Drawdown status updated to OPS_REJECTED for id:"
                                  + " {}",
                              drawdown.getId()));
            })
        .flatMap(
            updatedDrawdown -> saveRejectionDetails(updatedDrawdown, request.getRejectionNotes()))
        .flatMap(
            additionalDetails ->
                buildAndSendRejectionCallback(drawdown, additionalDetails, productCode))
        .onErrorResume(
            error -> {
              log.error(
                  "[DRAWDOWN_REJECT] Error during rejection for transactionId: {}. Error: {}",
                  transactionId,
                  error.getMessage(),
                  error);
              return Mono.error(error);
            });
  }

  /** Saves rejection details to additional details table. */
  private Mono<DrawdownAdditionalDetails> saveRejectionDetails(
      Drawdown drawdown, String rejectionReason) {

    log.info("[DRAWDOWN_REJECT] Saving rejection details for drawdownId: {}", drawdown.getId());

    // Check if additional details already exist
    return additionalDetailsRepository
        .findByDrawdownId(drawdown.getId())
        .flatMap(
            existing -> {
              // Update existing record with rejection reason
              existing.setRejectionReason(rejectionReason);
              return additionalDetailsRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  DrawdownAdditionalDetails additionalDetails =
                      DrawdownAdditionalDetails.builder()
                          .drawdownId(drawdown.getId())
                          .rejectionReason(rejectionReason)
                          .build();

                  return additionalDetailsRepository
                      .save(additionalDetails)
                      .doOnSuccess(
                          saved ->
                              log.info(
                                  "[DRAWDOWN_REJECT] Rejection details saved for drawdownId: {}",
                                  drawdown.getId()));
                }));
  }

  /**
   * Builds partner callback DTO for rejection and sends to partner. Status sent to partner is
   * "REJECTED".
   */
  private Mono<DrawdownPartnerCallbackDTO> buildAndSendRejectionCallback(
      Drawdown drawdown, DrawdownAdditionalDetails additionalDetails, String productCode) {

    DrawdownPartnerCallbackDTO callbackDto =
        DrawdownPartnerCallbackDTO.builder()
            .lineId(drawdown.getLineId())
            .drawdownRequestId(String.valueOf(drawdown.getId()))
            .transactionId(drawdown.getTransactionId())
            .lanId(null)
            .status("REJECTED") // Send "REJECTED" to partner instead of OPS_REJECTED
            .receiptNumber(null)
            .approvedAmount(null)
            .netDisbursement(null)
            .disbursementDate(null)
            .rejectionReason(additionalDetails.getRejectionReason())
            .build();

    return sendPartnerCallback(callbackDto, drawdown, productCode);
  }

  // ==================== DRAWDOWN STATUS QUERY ====================

  /**
   * Gets the current status of a drawdown by transactionId. This API is useful when a callback is
   * missed.
   *
   * <p>Response logic: - If status is SUCCESS: returns full details from drawdown and
   * additional_details - If status is OPS_REJECTED: returns details with status as "REJECTED" and
   * rejection reason - For other statuses: returns only lineId, transactionId, and status (as
   * stored in DB)
   *
   * @param transactionId the M2P transaction ID
   * @return the drawdown status DTO (same format as partner callback)
   */
  /**
   * Finds a drawdown by external ID and partner ID. Uniqueness constraint is (external_id,
   * partner_id).
   *
   * @param externalId the client-provided external ID (idempotency key)
   * @param partnerId the partner ID
   * @return the drawdown if found
   */
  public Mono<Drawdown> findByExternalIdAndPartnerId(String externalId, String partnerId) {
    return drawdownRepository.findByExternalIdAndPartnerId(externalId, partnerId);
  }

  /**
   * Gets drawdown by external ID scoped to partner. Validates lineId matches for security.
   *
   * @param externalId the client-provided external ID
   * @param lineId the credit line ID (must match drawdown's lineId)
   * @param partnerId the partner ID
   * @return the drawdown status DTO
   */
  public Mono<DrawdownPartnerCallbackDTO> getDrawdownByExternalId(
      String externalId, String lineId, String partnerId) {
    return drawdownRepository
        .findByExternalIdAndPartnerId(externalId, partnerId)
        .switchIfEmpty(
            Mono.error(
                new DrawdownNotFoundException("Drawdown not found for externalId: " + externalId)))
        .flatMap(
            drawdown -> {
              if (StringUtils.isNotBlank(lineId) && !lineId.equals(drawdown.getLineId())) {
                return Mono.error(
                    new DrawdownNotFoundException(
                        "Drawdown not found for externalId: "
                            + externalId
                            + " and lineId: "
                            + lineId));
              }
              return buildStatusResponse(drawdown);
            });
  }

  /**
   * Gets drawdown status by M2P transactionId. If not found in DB, attempts to sync from vendor
   * (findAndSyncMissingTransaction). Response format matches partner callback.
   *
   * @param transactionId M2P transaction ID
   * @param lineId credit line ID (optional, for sync fallback)
   * @return DrawdownPartnerCallbackDTO with status, or DrawdownNotFoundException
   */
  public Mono<DrawdownPartnerCallbackDTO> getDrawdownStatus(String transactionId, String lineId) {

    log.info("[DRAWDOWN_STATUS] Fetching status for transactionId: {}", transactionId);

    return drawdownRepository
        .findByTransactionId(transactionId)
        // If transactionId is not found in our DB, try to find and sync it
        .switchIfEmpty(
            findAndSyncMissingTransaction(lineId, transactionId)
                .switchIfEmpty(
                    Mono.error(
                        new DrawdownNotFoundException(
                            "Drawdown not found for transactionId: " + transactionId))))
        .flatMap(this::buildStatusResponse);
  }

  /**
   * Synchronizes a drawdown record by scanning the Line ID when a direct Transaction ID lookup
   * fails. * This is a fallback that: 1. Identifies candidate records for a Line ID that are in
   * BRE_APPROVED status. 2. Cross-references each candidate with the vendor's API to find the
   * missing link. 3. Performs validation against Vendor TxnID must match requested txnID AND Vendor
   * ExternalID must match our drawdownID. 4. Updates the verified record to OPS_APPROVAL_PENDING
   * and persists the Transaction ID
   *
   * @param lineId The credit line identifier to narrow the search.
   * @param transactionId The external transaction identifier to match against.
   * @return A Mono containing the synchronized Drawdown, or a 404 error if no match is verified.
   */
  private Mono<Drawdown> findAndSyncMissingTransaction(String lineId, String transactionId) {
    if (StringUtils.isBlank(lineId)) {
      log.warn("[DRAWDOWN_SYNC] Cannot sync: Line ID is blank for TxID: {}", transactionId);
      return Mono.empty();
    }

    return drawdownRepository
        .findByLineId(lineId)
        .filter(drawdown -> Drawdown.DrawdownStatus.BRE_APPROVED.equals(drawdown.getStatus()))
        .flatMap(
            candidate ->
                m2PWrapperApi
                    .getAllTransactionsDetailsOnALine(lineId)
                    .flatMap(
                        vendorResp -> {
                          boolean isTxMatched =
                              Objects.equals(vendorResp.getTransactionIdentifier(), transactionId);
                          boolean isIdMatched =
                              Objects.equals(
                                  vendorResp.getExternalId(), String.valueOf(candidate.getId()));

                          if (isTxMatched && isIdMatched) {
                            return updateDrawdownStatusWithTimestamp(
                                candidate,
                                transactionId,
                                Drawdown.DrawdownStatus.OPS_APPROVAL_PENDING);
                          }
                          return Mono.empty();
                        })
                    .onErrorResume(
                        e -> {
                          log.warn(
                              "[DRAWDOWN_SYNC] Failed vendor fetch for candidate {}: {}",
                              candidate.getId(),
                              e.getMessage(),
                              e);
                          return Mono.empty();
                        }))
        .next()
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[DRAWDOWN_SYNC] No verifiable match for TxID: {} across Line: {}. Returning"
                          + " empty.",
                      transactionId,
                      lineId);
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.error(
                  "[DRAWDOWN_SYNC] Unexpected error during sync for TxID: {}", transactionId, e);
              return Mono.empty();
            });
  }

  /**
   * Builds status response based on drawdown status: SUCCESS (full details), OPS_REJECTED
   * (rejection reason), or minimal (lineId, transactionId, status).
   */
  private Mono<DrawdownPartnerCallbackDTO> buildStatusResponse(Drawdown drawdown) {
    Drawdown.DrawdownStatus status = drawdown.getStatus();

    // For SUCCESS status - return full details
    if (status == Drawdown.DrawdownStatus.SUCCESS) {
      return buildSuccessStatusResponse(drawdown);
    }

    // For OPS_REJECTED status - return rejection details with status as "REJECTED"
    if (status == Drawdown.DrawdownStatus.OPS_REJECTED) {
      return buildRejectedStatusResponse(drawdown);
    }

    // For all other statuses - return minimal details with status as-is
    return Mono.just(DrawdownUtil.buildMinimalStatusResponse(drawdown));
  }

  /** Builds full status response for SUCCESS drawdowns. */
  private Mono<DrawdownPartnerCallbackDTO> buildSuccessStatusResponse(Drawdown drawdown) {
    return additionalDetailsRepository
        .findByDrawdownId(drawdown.getId())
        .map(
            additionalDetails ->
                DrawdownPartnerCallbackDTO.builder()
                    .lineId(drawdown.getLineId())
                    .drawdownRequestId(String.valueOf(drawdown.getId()))
                    .transactionId(drawdown.getTransactionId())
                    .lanId(
                        additionalDetails.getLoanAccountNumber() != null
                            ? additionalDetails.getLoanAccountNumber().toString()
                            : null)
                    .status(drawdown.getStatus().name())
                    .receiptNumber(additionalDetails.getReceiptNumber())
                    .approvedAmount(additionalDetails.getApprovedAmount())
                    .netDisbursement(additionalDetails.getNetDisbursedAmount())
                    .disbursementDate(
                        additionalDetails.getDisbursedDate() != null
                            ? additionalDetails.getDisbursedDate().toString()
                            : null)
                    .rejectionReason(null)
                    .build())
        .switchIfEmpty(
            Mono.just(
                DrawdownUtil.buildMinimalStatusResponse(
                    drawdown))); // Fallback if no additional details
  }

  /**
   * Builds status response for OPS_REJECTED drawdowns. Status is sent as "REJECTED" to match
   * callback format.
   */
  private Mono<DrawdownPartnerCallbackDTO> buildRejectedStatusResponse(Drawdown drawdown) {
    return additionalDetailsRepository
        .findByDrawdownId(drawdown.getId())
        .map(
            additionalDetails ->
                DrawdownPartnerCallbackDTO.builder()
                    .lineId(drawdown.getLineId())
                    .drawdownRequestId(String.valueOf(drawdown.getId()))
                    .transactionId(drawdown.getTransactionId())
                    .lanId(null)
                    .status("REJECTED") // Send "REJECTED" instead of OPS_REJECTED
                    .receiptNumber(null)
                    .approvedAmount(null)
                    .netDisbursement(null)
                    .disbursementDate(null)
                    .rejectionReason(additionalDetails.getRejectionReason())
                    .build())
        .switchIfEmpty(
            Mono.just(
                DrawdownPartnerCallbackDTO.builder()
                    .lineId(drawdown.getLineId())
                    .drawdownRequestId(String.valueOf(drawdown.getId()))
                    .transactionId(drawdown.getTransactionId())
                    .lanId(null)
                    .status("REJECTED")
                    .build())); // Fallback if no additional details
  }

  // ==================== ERROR HANDLING HELPERS ====================

  /**
   * Handles the case where M2P returns error but transaction might already be approved. Calls
   * report API to verify and if approved, updates tables and sends callback.
   */
  private Mono<DrawdownPartnerCallbackDTO> handlePossiblyAlreadyApprovedTransaction(
      Drawdown drawdown, String receiptNumber, String productCode) {

    String transactionId = drawdown.getTransactionId();

    return m2PWrapperApi
        .getDrawdownCallbackDetails(transactionId)
        .flatMap(
            callbackDetails -> {
              // Check if loanAccountNumber is populated - indicates successful approval
              if (callbackDetails.getLoanAccountNumber() != null) {
                log.info(
                    "[DRAWDOWN_APPROVE] Transaction {} confirmed as already approved. "
                        + "LoanAccountNumber: {}. Proceeding with status update and callback.",
                    transactionId,
                    callbackDetails.getLoanAccountNumber());

                // Update status to SUCCESS if not already
                return updateDrawdownStatusIfNeeded(drawdown, Drawdown.DrawdownStatus.SUCCESS)
                    .flatMap(
                        updatedDrawdown ->
                            saveAdditionalDetailsFromCallbackDetails(
                                updatedDrawdown, callbackDetails, receiptNumber))
                    .flatMap(
                        additionalDetails ->
                            buildAndSendPartnerCallback(drawdown, additionalDetails, productCode));
              } else {
                // loanAccountNumber not populated - transaction genuinely not approved
                log.warn(
                    "[DRAWDOWN_APPROVE] Transaction {} report has no loanAccountNumber. "
                        + "Transaction is not approved.",
                    transactionId);
                return Mono.error(
                    new DrawdownNotEligibleException(
                        "Drawdown transaction is not in approvable state"));
              }
            })
        .onErrorResume(
            reportError -> {
              // Report API also failed - return original error context
              log.error(
                  "[DRAWDOWN_APPROVE] Failed to fetch callback details for transactionId: {}."
                      + " Error: {}",
                  transactionId,
                  reportError.getMessage(),
                  reportError);
              return Mono.error(
                  new DrawdownVerificationException(
                      "Unable to verify transaction approval status: " + reportError.getMessage()));
            });
  }

  /** Updates drawdown status only if it's different from current status. */
  private Mono<Drawdown> updateDrawdownStatusIfNeeded(
      Drawdown drawdown, Drawdown.DrawdownStatus targetStatus) {

    if (drawdown.getStatus() == targetStatus) {
      log.info(
          "[DRAWDOWN_APPROVE] Drawdown {} already in {} status. Skipping status update.",
          drawdown.getId(),
          targetStatus);
      return Mono.just(drawdown);
    }

    return updateDrawdownStatusWithTimestamp(drawdown, drawdown.getTransactionId(), targetStatus);
  }

  /**
   * Saves additional details from callback details DTO. Used when handling already approved
   * transactions.
   */
  private Mono<DrawdownAdditionalDetails> saveAdditionalDetailsFromCallbackDetails(
      Drawdown drawdown, DrawdownCallbackDetailsDTO callbackDetails, String receiptNumber) {

    return getOrSaveAdditionalDetails(drawdown, callbackDetails, receiptNumber);
  }

  /**
   * Fetches disbursal details from M2P report API and saves to additional details table. Checks if
   * additional details already exist to avoid duplicates.
   */
  private Mono<DrawdownAdditionalDetails> fetchAndSaveAdditionalDetails(
      Drawdown drawdown, String receiptNumber) {

    String transactionId = drawdown.getTransactionId();

    // Check if additional details already exist
    return additionalDetailsRepository
        .findByDrawdownId(drawdown.getId())
        .flatMap(
            existing -> {
              log.info(
                  "[DRAWDOWN_APPROVE] Additional details already exist for drawdownId: {}",
                  drawdown.getId());
              return Mono.just(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[DRAWDOWN_APPROVE] Fetching disbursal details for transactionId: {}",
                      transactionId);

                  return m2PWrapperApi
                      .getDrawdownCallbackDetails(transactionId)
                      .doOnSuccess(
                          details ->
                              log.info(
                                  "[DRAWDOWN_APPROVE] Disbursal details fetched:"
                                      + " loanAccountNumber={}, approvedAmount={},"
                                      + " netDisbursement={}",
                                  details.getLoanAccountNumber(),
                                  details.getApprovedAmount(),
                                  details.getNetDisbursement()))
                      .flatMap(
                          callbackDetails ->
                              createAndSaveAdditionalDetails(
                                  drawdown, callbackDetails, receiptNumber));
                }));
  }

  /**
   * Gets existing additional details or saves new ones from callback details. Common method to
   * avoid duplicate saving logic.
   */
  private Mono<DrawdownAdditionalDetails> getOrSaveAdditionalDetails(
      Drawdown drawdown, DrawdownCallbackDetailsDTO callbackDetails, String receiptNumber) {

    return additionalDetailsRepository
        .findByDrawdownId(drawdown.getId())
        .flatMap(
            existing -> {
              log.info(
                  "[DRAWDOWN_APPROVE] Additional details already exist for drawdownId: {}",
                  drawdown.getId());
              return Mono.just(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> createAndSaveAdditionalDetails(drawdown, callbackDetails, receiptNumber)));
  }

  /** Creates and saves additional details entity from callback details. */
  private Mono<DrawdownAdditionalDetails> createAndSaveAdditionalDetails(
      Drawdown drawdown, DrawdownCallbackDetailsDTO callbackDetails, String receiptNumber) {

    LocalDate disbursedDate = DrawdownUtil.parseDisbursedDate(callbackDetails.getDisbursedDate());

    DrawdownAdditionalDetails additionalDetails =
        DrawdownAdditionalDetails.builder()
            .drawdownId(drawdown.getId())
            .loanAccountNumber(callbackDetails.getLoanAccountNumber())
            .approvedAmount(callbackDetails.getApprovedAmount())
            .netDisbursedAmount(callbackDetails.getNetDisbursement())
            .disbursedDate(disbursedDate)
            .receiptNumber(receiptNumber)
            .build();

    return additionalDetailsRepository
        .save(additionalDetails)
        .doOnSuccess(
            saved ->
                log.info(
                    "[DRAWDOWN_APPROVE] Additional details saved for drawdownId: {}",
                    drawdown.getId()));
  }

  /** Builds partner callback DTO and sends to partner. */
  private Mono<DrawdownPartnerCallbackDTO> buildAndSendPartnerCallback(
      Drawdown drawdown, DrawdownAdditionalDetails additionalDetails, String productCode) {

    DrawdownPartnerCallbackDTO callbackDto =
        DrawdownPartnerCallbackDTO.builder()
            .lineId(drawdown.getLineId())
            .drawdownRequestId(String.valueOf(drawdown.getId()))
            .transactionId(drawdown.getTransactionId())
            .lanId(
                additionalDetails.getLoanAccountNumber() != null
                    ? additionalDetails.getLoanAccountNumber().toString()
                    : null)
            .status(drawdown.getStatus().name())
            .receiptNumber(additionalDetails.getReceiptNumber())
            .approvedAmount(additionalDetails.getApprovedAmount())
            .netDisbursement(additionalDetails.getNetDisbursedAmount())
            .disbursementDate(
                additionalDetails.getDisbursedDate() != null
                    ? additionalDetails.getDisbursedDate().toString()
                    : null)
            .build();

    return sendPartnerCallback(callbackDto, drawdown, productCode);
  }

  /**
   * Fires an async (fire-and-forget) pipeline that: 1. Resolves the loanApplicationId and
   * CreditLineEntity from the drawdown's lineId. 2. Fetches the drawdown agreement document ID from
   * M2P. 3. Calls sendDrawdownApprovedNotificationIfConfigured. Errors are logged and swallowed so
   * the caller is never affected.
   */
  private void triggerDrawdownApprovedNotificationAsync(
      Drawdown drawdown, reactor.util.context.ContextView ctx) {
    getLeadIdFromCacheOrDb(drawdown.getLineId())
        .flatMap(
            loanApplicationId ->
                creditLineRepository
                    .findByM2pCreditLineId(drawdown.getLineId())
                    .flatMap(
                        creditLine ->
                            drawdownDocumentService
                                .findAllDrawdownDocuments(drawdown.getId(), drawdown.getLineId())
                                .next()
                                .flatMap(
                                    doc -> {
                                      if (doc.getM2pDocumentId() == null) {
                                        log.warn(
                                            "[DRAWDOWN_APPROVE] No m2pDocumentId on drawdown"
                                                + " document for drawdownId={}, skipping"
                                                + " notification",
                                            drawdown.getId());
                                        return Mono.empty();
                                      }
                                      return creditLineLimitApprovedNotificationService
                                          .sendDrawdownApprovedNotificationIfConfigured(
                                              loanApplicationId,
                                              creditLine,
                                              drawdown,
                                              String.valueOf(doc.getM2pDocumentId()));
                                    })))
        .contextWrite(ctx)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            null,
            err ->
                log.error(
                    "[DRAWDOWN_APPROVE] Async notification failed for drawdownId={}: {}",
                    drawdown.getId(),
                    err.getMessage()));
  }

  /** Sends callback to partner using the configured callback flow. */
  private Mono<DrawdownPartnerCallbackDTO> sendPartnerCallback(
      DrawdownPartnerCallbackDTO callbackDto, Drawdown drawdown, String productCode) {

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              String partnerCode = productControlConfigData.getT1();
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), DRAWDOWN_CALLBACK_IDENTIFIER);

              if (Objects.isNull(flowData)) {
                log.warn(
                    "[DRAWDOWN_CALLBACK] Flow data not found for identifier={}, productCode={}",
                    DRAWDOWN_CALLBACK_IDENTIFIER,
                    productCode);
                return Mono.just(callbackDto);
              }

              // Create callback log entity
              CallbackLogEntity callback =
                  CallbackLogEntity.builder()
                      .type(DRAWDOWN_CALLBACK_IDENTIFIER)
                      .request(Json.of(gson.toJson(callbackDto)))
                      .referenceId(drawdown.getTransactionId())
                      .productCode(productCode)
                      .createdAt(LocalDateTime.now())
                      .isRetry(false)
                      .build();

              log.info(
                  "[DRAWDOWN_CALLBACK] Sending callback to partner for transactionId={},"
                      + " partnerUri={}",
                  drawdown.getTransactionId(),
                  flowData.getPartnerUri());

              return partnerApi
                  .registerPartnerCallback(
                      callbackDto,
                      flowData.getPartnerUri(),
                      flowData.getCallMethod(),
                      partnerCode,
                      flowData.getRetryCount(),
                      DRAWDOWN_CALLBACK_IDENTIFIER)
                  .flatMap(
                      response -> {
                        callback.setResponse(Json.of(gson.toJson(response)));
                        return callbackStoreService.save(callback).thenReturn(callbackDto);
                      })
                  .doOnSuccess(
                      response ->
                          log.info(
                              "[DRAWDOWN_CALLBACK] Successfully sent callback to partner for"
                                  + " transactionId={}",
                              drawdown.getTransactionId()))
                  .onErrorResume(
                      error -> {
                        log.error(
                            "[DRAWDOWN_CALLBACK] Failed to send callback to partner for"
                                + " transactionId={}, error={}",
                            drawdown.getTransactionId(),
                            error.getMessage(),
                            error);
                        callback.setException(error.getMessage());
                        return callbackStoreService.save(callback).thenReturn(callbackDto);
                      });
            });
  }

  public Mono<DrawdownInternalRequest> validateAnchorIdAndPopulateMerchantDetails(
      DrawdownRequest request) {
    if (request == null) {
      return Mono.error(new DrawdownValidationException("Request cannot be null"));
    }

    return anchorMasterRepository
        .findByAnchorId(request.getAnchorId())
        .switchIfEmpty(
            Mono.error(
                new DrawdownValidationException(
                    "Anchor not found in Master for ID: " + request.getAnchorId())))
        .flatMap(
            anchorMaster -> {
              if (!anchorMaster.getAnchorId().equals(request.getAnchorId())) {
                return Mono.error(new DrawdownValidationException("Invalid anchor id."));
              }

              boolean isGstValid =
                  request.getInvoiceData().stream()
                      .anyMatch(
                          inv ->
                              inv.getRawData() != null
                                  && anchorMaster
                                      .getGst()
                                      .equalsIgnoreCase(inv.getRawData().getGst()));

              if (!isGstValid) {
                return Mono.error(
                    new DrawdownValidationException(
                        "GST in request does not match Anchor Master records"));
              }

              DrawdownInternalRequest.PaymentDetails payment =
                  DrawdownInternalRequest.PaymentDetails.builder().paymentTypeId(1).build();
              return Mono.just(DrawdownInternalRequest.from(request, null, payment));
            });
  }

  public Mono<DrawdownInternalRequest> createDrawdownInternalRequest(DrawdownRequest request) {
    if (request == null) {
      return Mono.error(new DrawdownValidationException("Request cannot be null"));
    }

    DrawdownInternalRequest.PaymentDetails payment =
        DrawdownInternalRequest.PaymentDetails.builder().paymentTypeId(1).build();
    return Mono.just(DrawdownInternalRequest.from(request, null, payment));
  }

  /**
   * Updates drawdown status with appropriate timestamp handling. Sets finalStatusAt for final
   * statuses (SUCCESS, FAILED, BRE_REJECTED, OPS_REJECTED). Sets opsApprovalPendingAt when status
   * changes to OPS_APPROVAL_PENDING.
   */
  public Mono<Drawdown> updateDrawdownStatusWithTimestamp(
      Drawdown drawdown, String transactionId, Drawdown.DrawdownStatus status) {

    drawdown.setStatus(status);

    if (StringUtils.isNotBlank(transactionId)) {
      drawdown.setTransactionId(transactionId);
    }

    LocalDateTime now = LocalDateTime.now();

    // Set opsApprovalPendingAt when entering OPS_APPROVAL_PENDING
    if (status == Drawdown.DrawdownStatus.OPS_APPROVAL_PENDING) {
      drawdown.setOpsApprovalPendingAt(now);
    }

    // Set finalStatusAt for final statuses
    if (status.isFinalStatus()) {
      drawdown.setFinalStatusAt(now);
    }

    return drawdownRepository.save(drawdown);
  }
}
