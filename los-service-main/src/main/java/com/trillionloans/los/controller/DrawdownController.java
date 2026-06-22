package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.model.request.DrawdownApproveRequest;
import com.trillionloans.los.model.request.DrawdownRejectRequest;
import com.trillionloans.los.model.request.DrawdownRequest;
import com.trillionloans.los.model.request.PreviewRpsRequest;
import com.trillionloans.los.model.response.DrawdownPartnerCallbackDTO;
import com.trillionloans.los.model.response.DrawdownResponse;
import com.trillionloans.los.model.response.LineDrawdownDocumentsDto;
import com.trillionloans.los.model.response.m2p.TransactionDetailsDTO;
import com.trillionloans.los.service.drawdownorchestrator.DrawdownDocumentService;
import com.trillionloans.los.service.drawdownorchestrator.DrawdownOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/drawdown")
@RequiredArgsConstructor
public class DrawdownController {
  private final DrawdownOrchestrator drawdownOrchestrator;
  private final DrawdownDocumentService drawdownDocumentService;

  @PostMapping("/{lineId}")
  public Mono<DrawdownResponse> processDrawdown(
      @RequestBody Mono<DrawdownRequest> request,
      @RequestHeader(name = PARTNER_ID) String partnerId,
      @RequestHeader(name = PRODUCT_CODE) String productCode,
      @PathVariable("lineId") String lineId) {

    return request.flatMap(
        reqBody -> {
          reqBody.setPartnerId(partnerId);
          return drawdownOrchestrator.processSingleDrawdown(
              reqBody, lineId, partnerId, productCode);
        });
  }

  /**
   * Preview RPS schedule without creating a loan. Request body is passed through to M2P
   * get-rps-without-loan; response is generic (upstream service defines typed contract).
   *
   * @param lineId credit line ID
   * @param request preview RPS request (same format as M2P)
   * @param partnerId partner ID (from header)
   * @param productCode product code (from header)
   * @return generic RPS preview response
   */
  @PostMapping("/{lineId}/preview-rps")
  public Mono<Object> previewRpsSchedule(
      @PathVariable("lineId") String lineId,
      @RequestBody Mono<PreviewRpsRequest> request,
      @RequestHeader(name = PARTNER_ID) String partnerId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {

    log.info(
        "[DRAWDOWN_CONTROLLER] Preview RPS request for lineId: {}, partnerId: {}",
        lineId,
        partnerId);

    return request.flatMap(reqBody -> drawdownOrchestrator.previewRpsSchedule(reqBody, lineId));
  }

  /**
   * Approves a drawdown that is in OPS_APPROVAL_PENDING state.
   *
   * @param transactionId the M2P transaction ID of the drawdown to approve
   * @param request the approval request containing payment details
   * @param productCode the product code for callback configuration
   * @return the partner callback DTO containing approval details
   */
  @PostMapping("/approve/{transactionId}")
  public Mono<DrawdownPartnerCallbackDTO> approveDrawdown(
      @PathVariable("transactionId") String transactionId,
      @RequestBody @Valid Mono<DrawdownApproveRequest> request,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {

    log.info("[DRAWDOWN_CONTROLLER] Received approve request for transactionId: {}", transactionId);

    return request.flatMap(
        reqBody -> drawdownOrchestrator.approveDrawdown(transactionId, reqBody, productCode));
  }

  /**
   * Rejects a drawdown that is in OPS_APPROVAL_PENDING state.
   *
   * @param transactionId the M2P transaction ID of the drawdown to reject
   * @param request the rejection request containing notes and timestamp
   * @param productCode the product code for callback configuration
   * @return the partner callback DTO containing rejection details
   */
  @PostMapping("/reject/{transactionId}")
  public Mono<DrawdownPartnerCallbackDTO> rejectDrawdown(
      @PathVariable("transactionId") String transactionId,
      @RequestBody @Valid Mono<DrawdownRejectRequest> request,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {

    log.info("[DRAWDOWN_CONTROLLER] Received reject request for transactionId: {}", transactionId);

    return request.flatMap(
        reqBody -> drawdownOrchestrator.rejectDrawdown(transactionId, reqBody, productCode));
  }

  /**
   * Gets the current status of a drawdown by transactionId. Useful when a callback is missed.
   *
   * @param transactionId the M2P transaction ID
   * @param lineId
   * @return the drawdown status DTO (same format as partner callback)
   */
  @GetMapping("/{lineId}/status/{transactionId}")
  public Mono<DrawdownPartnerCallbackDTO> getDrawdownStatus(
      @PathVariable("transactionId") String transactionId, @PathVariable("lineId") String lineId) {

    log.info("[DRAWDOWN_CONTROLLER] Received status request for transactionId: {}", transactionId);

    return drawdownOrchestrator.getDrawdownStatus(transactionId, lineId);
  }

  /**
   * Gets a drawdown by its client-provided external ID (idempotency key). Scoped to partner - each
   * partner can only fetch their own drawdowns.
   *
   * @param externalId the client-provided external ID
   * @param lineId the credit line ID
   * @param partnerId the partner ID (from header)
   * @return the drawdown status DTO (same format as partner callback)
   */
  @GetMapping("/{lineId}/external/{externalId}")
  public Mono<DrawdownPartnerCallbackDTO> getDrawdownByExternalId(
      @PathVariable("externalId") String externalId,
      @PathVariable("lineId") String lineId,
      @RequestHeader(name = PARTNER_ID) String partnerId) {

    log.info("[DRAWDOWN_CONTROLLER] Received get request for externalId: {}", externalId);

    return drawdownOrchestrator.getDrawdownByExternalId(externalId, lineId, partnerId);
  }

  @GetMapping("/transaction-details/{lineId}")
  public Flux<TransactionDetailsDTO> getAllTransactionDetails(
      @PathVariable("lineId") String lineId) {
    return drawdownOrchestrator.getAllTransactionDetails(lineId);
  }

  @GetMapping("/transaction-details/active/{lineId}")
  public Flux<TransactionDetailsDTO> getActiveTransactionDetails(
      @PathVariable("lineId") String lineId) {
    return drawdownOrchestrator.getActiveTransactionDetails(lineId);
  }

  /**
   * Document references for drawdowns on a line: invoice attachments (per linked invoice) plus
   * drawdown agreement docs. Scoped by {@code PARTNER_ID}.
   *
   * <ul>
   *   <li>{@code drawdownId} set: one drawdown — all invoice docs for invoices in that drawdown,
   *       plus that drawdown's documents.
   *   <li>{@code drawdownId} omitted: same structure for every drawdown on the line (newest
   *       drawdowns first).
   * </ul>
   */
  @GetMapping("/{lineId}/documents")
  public Mono<LineDrawdownDocumentsDto> getDocumentsForLine(
      @PathVariable("lineId") String lineId,
      @RequestParam(name = "drawdownId", required = false) Long drawdownId,
      @RequestHeader(name = PARTNER_ID) String partnerId) {

    log.info("[DRAWDOWN_CONTROLLER] List documents lineId={}, drawdownId={}", lineId, drawdownId);

    return drawdownDocumentService.listDocumentsForLine(lineId, drawdownId, partnerId);
  }
}
