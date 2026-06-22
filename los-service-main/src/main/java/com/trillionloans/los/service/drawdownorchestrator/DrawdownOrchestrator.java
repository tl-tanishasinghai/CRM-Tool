package com.trillionloans.los.service.drawdownorchestrator;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.exception.drawdown.DrawdownExternalIdAlreadyExistsException;
import com.trillionloans.los.exception.drawdown.DrawdownUnsupportedProductException;
import com.trillionloans.los.model.request.DrawdownApproveRequest;
import com.trillionloans.los.model.request.DrawdownRejectRequest;
import com.trillionloans.los.model.request.DrawdownRequest;
import com.trillionloans.los.model.request.PreviewRpsRequest;
import com.trillionloans.los.model.response.DrawdownPartnerCallbackDTO;
import com.trillionloans.los.model.response.DrawdownResponse;
import com.trillionloans.los.model.response.m2p.TransactionDetailsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawdownOrchestrator {
  private final M2PWrapperApi m2PWrapperApi;
  private final InvoiceService invoiceService;
  private final DrawdownService drawdownService;

  public Mono<DrawdownResponse> processSingleDrawdown(
      DrawdownRequest request, String lineId, String partnerId, String productCode) {
    return Mono.just(request)
        .doOnNext(
            req ->
                log.info(
                    "[DRAWDOWN_ORCHESTRATOR] Drawdown request received for lineId: {},"
                        + " partnerId: {}",
                    lineId,
                    req.getPartnerId()))
        .flatMap(
            req -> {
              if (StringUtils.isNotBlank(req.getExternalId())) {
                return drawdownService
                    .findByExternalIdAndPartnerId(req.getExternalId(), partnerId)
                    .flatMap(
                        existing ->
                            Mono.<DrawdownResponse>error(
                                new DrawdownExternalIdAlreadyExistsException(
                                    "External ID already exists: " + req.getExternalId())));
              }
              return Mono.<DrawdownResponse>empty();
            })
        .switchIfEmpty(
            Mono.defer(
                () ->
                    Mono.just(request)
                        .map(req -> DrawdownUtil.determineDrawdownRequestType(productCode))
                        .flatMap(
                            processType -> {
                              log.info(
                                  "[DRAWDOWN_ORCHESTRATOR] Drawdown process type: {}", processType);

                              return switch (processType) {
                                case PROCESS_INVOICES_AND_DRAWDOWN ->
                                    processInvoiceAndDrawdown(
                                        request, lineId, partnerId, productCode);
                                case PROCESS_DRAWDOWN_ONLY ->
                                    processStandaloneDrawdown(
                                        request, lineId, partnerId, productCode);
                                case UNSUPPORTED_PRODUCT_CODE -> {
                                  log.error(
                                      "[DRAWDOWN_ORCHESTRATOR] Unsupported product: {}",
                                      productCode);
                                  yield Mono.error(
                                      new DrawdownUnsupportedProductException(
                                          "The provided product code is not supported."));
                                }
                              };
                            })));
  }

  private Mono<DrawdownResponse> processInvoiceAndDrawdown(
      DrawdownRequest request, String lineId, String partnerId, String productCode) {
    return Mono.just(request)
        .doOnNext(
            req ->
                log.info(
                    "[DRAWDOWN_ORCHESTRATOR][VALIDATE_INVOICES] Proceeding to validate the"
                        + " invoices. LineId: {}, PartnerId: {}",
                    lineId,
                    req.getPartnerId()))
        .flatMap(DrawdownUtil::validateInvoices)
        .doOnNext(
            req -> {
              log.info(
                  "[DRAWDOWN_ORCHESTRATOR][VALIDATE_INVOICES] Invoices validated successfully."
                      + " Proceeding to validate the anchor for lineId: {}, partnerId: {}",
                  lineId,
                  req.getPartnerId());
            })
        .flatMap(drawdownService::validateAnchorIdAndPopulateMerchantDetails)
        .doOnNext(
            req ->
                log.info(
                    "[DRAWDOWN_ORCHESTRATOR][VALIDATE_INVOICES] Anchor validated successfully"
                        + " for lineId: {}, partnerId: {}",
                    lineId,
                    req.getPartnerId()))
        .flatMap(invoiceReq -> invoiceService.validateAndPersistInvoices(invoiceReq, lineId))
        .flatMap(
            tuple ->
                drawdownService.initiateDrawdownExecution(
                    tuple.getT1(), tuple.getT2(), request, lineId, partnerId, productCode));
  }

  private Mono<DrawdownResponse> processStandaloneDrawdown(
      DrawdownRequest request, String lineId, String partnerId, String productCode) {
    return Mono.just(request)
        .flatMap(drawdownService::createDrawdownInternalRequest)
        .flatMap(
            internalRequest ->
                drawdownService.persistAndExecuteDrawdown(
                    internalRequest, request, lineId, partnerId, productCode));
  }

  /**
   * Preview RPS schedule for a credit line without creating a loan. Forwards request to M2P
   * get-rps-without-loan; response is generic (upstream defines typed contract).
   *
   * @param request preview RPS request (same body as upstream and M2P)
   * @param lineId credit line ID (for audit/logging)
   * @return generic RPS preview response
   */
  public Mono<Object> previewRpsSchedule(PreviewRpsRequest request, String lineId) {
    log.info("[DRAWDOWN_ORCHESTRATOR] Preview RPS request for lineId: {}", lineId);
    return m2PWrapperApi.getPreviewRpsSchedule(request);
  }

  public Flux<TransactionDetailsDTO> getAllTransactionDetails(String lineId) {
    return m2PWrapperApi.getAllTransactionsDetailsOnALine(lineId);
  }

  public Flux<TransactionDetailsDTO> getActiveTransactionDetails(String lineId) {
    return m2PWrapperApi.getActiveTransactionsDetailsOnALine(lineId);
  }

  /**
   * Approves a drawdown that is in OPS_APPROVAL_PENDING state. Delegates to DrawdownService for the
   * actual approval logic.
   *
   * @param transactionId the M2P transaction ID
   * @param request the approval request
   * @param productCode the product code for callback configuration
   * @return the partner callback DTO
   */
  public Mono<DrawdownPartnerCallbackDTO> approveDrawdown(
      String transactionId, DrawdownApproveRequest request, String productCode) {

    log.info(
        "[DRAWDOWN_ORCHESTRATOR] Processing approval for transactionId: {}, productCode: {}",
        transactionId,
        productCode);

    return drawdownService.approveDrawdown(transactionId, request, productCode);
  }

  /**
   * Rejects a drawdown that is in OPS_APPROVAL_PENDING state. Delegates to DrawdownService for the
   * actual rejection logic.
   *
   * @param transactionId the M2P transaction ID
   * @param request the rejection request
   * @param productCode the product code for callback configuration
   * @return the partner callback DTO
   */
  public Mono<DrawdownPartnerCallbackDTO> rejectDrawdown(
      String transactionId, DrawdownRejectRequest request, String productCode) {

    log.info(
        "[DRAWDOWN_ORCHESTRATOR] Processing rejection for transactionId: {}, productCode: {}",
        transactionId,
        productCode);

    return drawdownService.rejectDrawdown(transactionId, request, productCode);
  }

  /**
   * Gets the current status of a drawdown by transactionId. Useful when a callback is missed.
   *
   * @param transactionId the M2P transaction ID
   * @return the drawdown status DTO (same format as partner callback)
   */
  public Mono<DrawdownPartnerCallbackDTO> getDrawdownStatus(String transactionId, String lineId) {

    log.info("[DRAWDOWN_ORCHESTRATOR] Fetching status for transactionId: {}", transactionId);

    return drawdownService.getDrawdownStatus(transactionId, lineId);
  }

  /**
   * Gets a drawdown by its client-provided external ID (idempotency key), scoped to partner.
   *
   * @param externalId the client-provided external ID
   * @param lineId the credit line ID
   * @param partnerId the partner ID (ensures partner can only fetch their own drawdowns)
   * @return the drawdown status DTO (same format as partner callback)
   */
  public Mono<DrawdownPartnerCallbackDTO> getDrawdownByExternalId(
      String externalId, String lineId, String partnerId) {

    log.info("[DRAWDOWN_ORCHESTRATOR] Fetching drawdown by externalId: {}", externalId);

    return drawdownService.getDrawdownByExternalId(externalId, lineId, partnerId);
  }
}
