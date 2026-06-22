package com.trillionloans.los.service.drawdownorchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trillionloans.los.exception.drawdown.InvoiceAlreadyUsedException;
import com.trillionloans.los.exception.drawdown.InvoicePersistenceException;
import com.trillionloans.los.model.dto.DrawdownInternalRequest;
import com.trillionloans.los.model.dto.InvoiceValidationResult;
import com.trillionloans.los.model.entity.Drawdown;
import com.trillionloans.los.model.entity.Invoice;
import com.trillionloans.los.model.request.InvoiceData;
import com.trillionloans.los.model.response.InvoiceResponse;
import com.trillionloans.los.repository.drawdown.DrawdownInvoiceMappingRepository;
import com.trillionloans.los.repository.drawdown.InvoiceRepository;
import com.trillionloans.los.util.drawdown.DrawdownHashKeyUtil;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
  private final InvoiceRepository invoiceRepository;
  private final DrawdownInvoiceMappingRepository drawdownInvoiceMapping;

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  /** Statuses that unlock an invoice for reuse in a new drawdown. */
  private static final Set<String> STATUSES_THAT_UNLOCK_INVOICE =
      Set.of(
          Drawdown.DrawdownStatus.FAILED.name(),
          Drawdown.DrawdownStatus.BRE_REJECTED.name(),
          Drawdown.DrawdownStatus.RISK_REJECTED.name(),
          Drawdown.DrawdownStatus.OPS_REJECTED.name());

  public Mono<Tuple2<DrawdownInternalRequest, List<InvoiceResponse>>> validateAndPersistInvoices(
      DrawdownInternalRequest internalReq, String lineId) {

    return validateInvoicesForDrawdown(
            internalReq.getInvoiceData(), internalReq.getPartnerId(), internalReq.getAnchorId())
        .doOnNext(
            valid ->
                log.info(
                    "[DRAWDOWN_ORCHESTRATOR][VALIDATE_INVOICES] Invoices validation step was"
                        + " successful. {}",
                    valid))
        .doOnNext(
            any ->
                log.info(
                    "[DRAWDOWN_ORCHESTRATOR][PERSIST_INVOICES] Proceeding to persist the"
                        + " invoices."))
        .flatMap(validated -> processInvoices(validated, lineId))
        .doOnNext(
            processed -> {
              log.info(
                  "[DRAWDOWN_ORCHESTRATOR][PERSIST_INVOICES] Invoices successfully persisted. {}",
                  processed);
              log.info(
                  "[DRAWDOWN_ORCHESTRATOR][PERSIST_DRAWDOWN] Proceeding to persist the drawdown.");
            })
        .map(processedInvoices -> Tuples.of(internalReq, processedInvoices));
  }

  private Mono<List<InvoiceValidationResult>> validateInvoicesForDrawdown(
      List<InvoiceData> listOfInvoices, String partnerId, String anchorId) {

    log.info(
        "[INVOICE_VALIDATION] Starting validation for {} invoices. Partner: {}, Anchor: {}",
        listOfInvoices.size(),
        partnerId,
        anchorId);

    return Flux.range(0, listOfInvoices.size())
        .flatMap(
            index -> {
              InvoiceData invoiceData = listOfInvoices.get(index);
              return mapToInvoiceEntity(invoiceData, partnerId, anchorId)
                  .flatMap(
                      tempInvoice ->
                          invoiceRepository
                              .findByHashKey(tempInvoice.getHashKey())
                              // Case A: Invoice exists in system
                              .flatMap(
                                  persistedInvoice -> {
                                    log.info(
                                        "[INVOICE_VALIDATION] Found existing invoice. InvoiceId:"
                                            + " {}, hash: {}",
                                        persistedInvoice.getId(),
                                        tempInvoice.getHashKey());

                                    return drawdownInvoiceMapping
                                        .findDrawdownStatusesByInvoiceId(persistedInvoice.getId())
                                        .collectList()
                                        .map(
                                            statuses -> {
                                              boolean hasSuccessfulOrPending =
                                                  statuses.stream()
                                                      .anyMatch(
                                                          status ->
                                                              status == null
                                                                  || !STATUSES_THAT_UNLOCK_INVOICE
                                                                      .contains(
                                                                          status.toUpperCase()));

                                              return InvoiceValidationResult.builder()
                                                  .invoiceEntity(persistedInvoice)
                                                  .exists(true)
                                                  .hasUsedInDrawdownBefore(hasSuccessfulOrPending)
                                                  .invoiceIndex(index)
                                                  .invoiceData(invoiceData)
                                                  .build();
                                            });
                                  })
                              // Case B: Invoice does NOT exist in system
                              .switchIfEmpty(
                                  Mono.defer(
                                      () -> {
                                        log.debug(
                                            "[INVOICE_VALIDATION] New invoice detected with hash:"
                                                + " {}",
                                            tempInvoice.getHashKey());
                                        return Mono.just(
                                            InvoiceValidationResult.builder()
                                                .invoiceEntity(tempInvoice)
                                                .exists(false)
                                                .hasUsedInDrawdownBefore(false)
                                                .invoiceIndex(index)
                                                .invoiceData(invoiceData)
                                                .build());
                                      })));
            })
        .collectList()
        .doOnNext(
            results ->
                log.info(
                    "[INVOICE_VALIDATION] Completed lookup. Total processed: {}", results.size()))
        .flatMap(
            results -> {
              // 1. Collect Invoices already used in a drawdown
              List<Invoice> usedInvoices =
                  results.stream()
                      .filter(res -> res.isExists() && res.isHasUsedInDrawdownBefore())
                      .map(InvoiceValidationResult::getInvoiceEntity)
                      .toList();

              if (!usedInvoices.isEmpty()) {
                List<String> usedInvoiceIds =
                    usedInvoices.stream()
                        .map(
                            inv ->
                                Optional.ofNullable(inv.getInvoiceNumber())
                                    .orElseGet(() -> String.valueOf(inv.getId())))
                        .toList();

                log.error(
                    "[INVOICE_VALIDATION] Validation FAILED. Invoices already in use: {}",
                    usedInvoiceIds);

                return Mono.error(
                    new InvoiceAlreadyUsedException(
                        "Validation failed: The following invoices have already been used in a"
                            + " drawdown. Invoice numbers: "
                            + usedInvoiceIds));
              }

              // return clean entities
              return Mono.just(results);
            });
  }

  private Mono<List<InvoiceResponse>> processInvoices(
      List<InvoiceValidationResult> validatedResults, String lineId) {

    return Flux.fromIterable(validatedResults)
        .concatMap(
            result -> {
              Mono<Invoice> invoiceMono;
              if (result.isExists() && result.getInvoiceEntity() != null) {
                invoiceMono = Mono.just(result.getInvoiceEntity());
              } else {
                invoiceMono = saveInvoice(result.getInvoiceEntity());
              }

              return invoiceMono;
            })
        .flatMap(DrawdownUtil::mapToInvoiceResponse)
        .collectList()
        .onErrorResume(
            error -> {
              log.error(
                  "[INVOICE] Unexpected error in processing invoices. {}",
                  error.getMessage(),
                  error);
              return Mono.error(error);
            });
  }

  private Mono<Invoice> mapToInvoiceEntity(
      InvoiceData invoiceData, String partnerId, String anchorId) {
    return Mono.fromCallable(
        () -> {
          InvoiceData.RawInvoiceData rawInvoiceData = invoiceData.getRawData();

          String invoiceNumber = rawInvoiceData.getInvoiceNumber();
          String distributorId = rawInvoiceData.getDistributorId();
          LocalDate invoiceDate = rawInvoiceData.getInvoiceDate();
          BigDecimal amount = rawInvoiceData.getAmount();

          String jsonString = OBJECT_MAPPER.writeValueAsString(rawInvoiceData);
          Json metadata = Json.of(jsonString);

          String hashKey =
              DrawdownHashKeyUtil.invoiceHash(
                  partnerId, anchorId, invoiceNumber, invoiceDate, rawInvoiceData.getIdentityKey());

          return Invoice.builder()
              .partnerId(partnerId)
              .anchorId(distributorId)
              .amount(amount)
              .invoiceNumber(invoiceNumber)
              .invoiceDate(invoiceDate)
              .metadata(metadata)
              .hashKey(hashKey)
              .build();
        });
  }

  private Mono<Invoice> saveInvoice(Invoice invoice) {
    return invoiceRepository
        .save(invoice)
        .onErrorResume(
            (error) -> {
              log.error("[INVOICE][SAVE][ERROR] Error: {}", error.getMessage(), error);
              return Mono.error(new InvoicePersistenceException("Error while saving invoice."));
            });
  }
}
