package com.trillionloans.lms.service;

import static com.trillionloans.lms.constant.StringConstants.CREDIT_LINE_REPAYMENT_LOG_HEADER;
import static com.trillionloans.lms.util.CreditLineUtil.isCreditLineProduct;

import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.entity.CreditLineMarkRepaymentRecord;
import com.trillionloans.lms.model.request.CreditLineMarkRepaymentRequest;
import com.trillionloans.lms.model.request.M2PCreditLineMarkRepaymentRequest;
import com.trillionloans.lms.model.response.CreditLineMarkRepaymentResponse;
import com.trillionloans.lms.repository.CreditLineMarkRepaymentRepository;
import com.trillionloans.lms.util.CreditLineUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditLineRepaymentService {
  private final M2PApi m2PApi;
  private final CreditLineMarkRepaymentRepository repository;

  public Mono<CreditLineMarkRepaymentResponse> markRepayment(
      String lineId, CreditLineMarkRepaymentRequest request, String productCode) {

    return Mono.defer(
        () -> {
          log.info(
              "[{}] Credit line mark repayment request received for lineId: {}, product: {}",
              CREDIT_LINE_REPAYMENT_LOG_HEADER,
              lineId,
              productCode);

          if (!isCreditLineProduct(productCode)) {
            log.error(
                "[{}] Not a valid credit line product: {}",
                CREDIT_LINE_REPAYMENT_LOG_HEADER,
                productCode);

            return Mono.error(
                new BaseException(
                    "The provided product code is not a valid credit line product.",
                    null,
                    HttpStatus.BAD_REQUEST));
          }

          // M2P V3 requires root-level amount = sum of all emiDetails amounts; compute and set
          // before call
          if (request.getEmiDetails() != null) {
            BigDecimal total =
                request.getEmiDetails().stream()
                    .map(
                        e ->
                            e.getAmount() != null
                                ? BigDecimal.valueOf(e.getAmount())
                                : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            request.setAmount(total.doubleValue());
          }

          final M2PCreditLineMarkRepaymentRequest m2pRequest;
          try {
            m2pRequest = M2PCreditLineMarkRepaymentRequest.from(request);
          } catch (IllegalArgumentException e) {
            return Mono.error(
                new BaseException(e.getMessage(), null, HttpStatus.BAD_REQUEST));
          }

          return m2PApi
              .markRepayment(lineId, m2pRequest)
              .doOnSuccess(response -> saveRepaymentRecordsInBackground(lineId, request, response))
              .doOnError(
                  error ->
                      log.error(
                          "[{}] Process failed for lineId: {}. Error: {}",
                          CREDIT_LINE_REPAYMENT_LOG_HEADER,
                          lineId,
                          error.getMessage()));
        });
  }

  public Flux<CreditLineUtil.RepaymentGroupResponse> getRepaymentTransactions(
      String lineId, String drawdownId, String transactionId, String productCode) {
    if (!isCreditLineProduct(productCode)) {
      log.error(
          "[{}] Not a valid credit line product: {}",
          CREDIT_LINE_REPAYMENT_LOG_HEADER,
          productCode);

      return Flux.error(
          new BaseException(
              "The provided product code is not a valid credit line product.",
              null,
              HttpStatus.BAD_REQUEST));
    }

    // 1. Fetch from db
    Flux<CreditLineMarkRepaymentRecord> localSource;
    if (transactionId != null && !transactionId.isBlank()) {
      localSource = repository.findByLineIdAndTransactionId(lineId, transactionId);
    } else if (drawdownId != null && !drawdownId.isBlank()) {
      localSource = repository.findByLineIdAndEmiTransactionId(lineId, drawdownId);
    } else {
      localSource = repository.findByLineId(lineId);
    }

    return Mono.zip(
            localSource.collectList(),
            m2PApi.getCreditLineAllTransactionDetails(lineId).collectList())
        .flatMapMany(
            tuple -> {
              List<CreditLineMarkRepaymentRecord> localRecords = tuple.getT1();
              List<Object> apiTransactions = tuple.getT2();

              // Create a lookup map for M2P API data by transactionIdentifier
              Map<String, Object> apiMap =
                  apiTransactions.stream()
                      .collect(
                          Collectors.toMap(
                              CreditLineUtil::extractIdentifier,
                              txn -> txn,
                              (existing, replacement) -> existing));

              // Group local records by Drawdown ID
              Map<String, List<CreditLineMarkRepaymentRecord>> groupedByDrawdown =
                  localRecords.stream()
                      .collect(
                          Collectors.groupingBy(
                              CreditLineMarkRepaymentRecord::getEmiTransactionId));

              return Flux.fromIterable(groupedByDrawdown.entrySet())
                  .map(
                      entry -> {
                        String emiId = entry.getKey();
                        List<CreditLineMarkRepaymentRecord> records = entry.getValue();

                        BigDecimal total =
                            records.stream()
                                .map(CreditLineMarkRepaymentRecord::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Map local repayments to DTO and enrich each with M2P details
                        List<CreditLineUtil.RepaymentDetailDTO> details =
                            records.stream()
                                .map(
                                    r ->
                                        new CreditLineUtil.RepaymentDetailDTO(
                                            r.getTransactionId(),
                                            r.getAmount(),
                                            r.getTransactionTime(),
                                            r.getReferenceNumber(),
                                            apiMap.get(r.getTransactionId())
                                            // txn
                                            ))
                                .toList();

                        // Create the final Group Response enriched with M2P details for the
                        // drawdown
                        return new CreditLineUtil.RepaymentGroupResponse(
                            emiId,
                            apiMap.get(emiId), // Enrichment for drawdown txn
                            total,
                            records.size(),
                            details);
                      });
            })
        // Final filter in case user requested a specific drawdown/transaction via query params
        .filter(group -> CreditLineUtil.applyResultFilters(group, drawdownId, transactionId));
  }

  public Flux<Object> getTransactionsDetails(
      String lineId, String productCode, boolean activeTxns) {
    if (!isCreditLineProduct(productCode)) {
      log.error(
          "[{}] Not a valid credit line product: {}",
          CREDIT_LINE_REPAYMENT_LOG_HEADER,
          productCode);

      return Flux.error(
          new BaseException(
              "The provided product code is not a valid credit line product.",
              null,
              HttpStatus.BAD_REQUEST));
    }

    if (activeTxns) {
      return m2PApi.getCreditLineActiveTransactionDetails(lineId);
    } else {
      return m2PApi.getCreditLineAllTransactionDetails(lineId);
    }
  }

  private void saveRepaymentRecordsInBackground(
      String lineId,
      CreditLineMarkRepaymentRequest request,
      CreditLineMarkRepaymentResponse response) {

    Mono.fromCallable(() -> CreditLineUtil.createRepaymentEntities(lineId, request, response))
        .flatMapMany(
            entities -> {
              if (entities.isEmpty()) {
                log.warn(
                    "[{}] No EMI details found to save for lineId: {}",
                    CREDIT_LINE_REPAYMENT_LOG_HEADER,
                    lineId);
                return Flux.empty();
              }
              return repository.saveAll(entities);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnNext(
            saved ->
                log.info(
                    "[{}] Successfully saved repayment record: transactionId: {} lineId: {}",
                    CREDIT_LINE_REPAYMENT_LOG_HEADER,
                    saved.getEmiTransactionId(),
                    lineId))
        .doOnError(
            error ->
                log.error(
                    "[{}] Failed to save repayment records for lineId: {}. Error: {}",
                    CREDIT_LINE_REPAYMENT_LOG_HEADER,
                    lineId,
                    error.getMessage()))
        .subscribe();
  }
}
