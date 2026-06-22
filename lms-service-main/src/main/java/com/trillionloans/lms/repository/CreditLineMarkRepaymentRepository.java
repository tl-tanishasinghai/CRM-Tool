package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.CreditLineMarkRepaymentRecord;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CreditLineMarkRepaymentRepository
    extends R2dbcRepository<CreditLineMarkRepaymentRecord, Long> {

  // 1. Fetch all repayments for a specific credit line
  Flux<CreditLineMarkRepaymentRecord> findByLineId(String lineId);

  Flux<CreditLineMarkRepaymentRecord> findByLineIdAndEmiTransactionId(
      String lineId, String emiTransactionId);

  Flux<CreditLineMarkRepaymentRecord> findByLineIdAndTransactionId(
      String lineId, String transactionId);

  // 2. Find specific record by the M2P/Provider transaction ID
  Mono<CreditLineMarkRepaymentRecord> findByTransactionId(String transactionId);

  // 3. Find specific record by the EMI/Drawdown transaction ID
  Flux<CreditLineMarkRepaymentRecord> findByEmiTransactionId(String emiTransactionId);

  // 4. Find by external reference number (useful for payment reconciliation)
  Mono<CreditLineMarkRepaymentRecord> findByReferenceNumber(String referenceNumber);

  // 5. Fetch repayments within a specific time range (Epoch Millis)
  Flux<CreditLineMarkRepaymentRecord> findByLineIdAndTransactionTimeBetween(
      String lineId, Long startTime, Long endTime);

  // 6. Get the latest transaction for a line (useful for your "last transaction" validation)
  Mono<CreditLineMarkRepaymentRecord> findFirstByLineIdOrderByTransactionTimeDesc(String lineId);

  // 7. Find all repayments of a certain type (e.g., UPI vs Bank Transfer)
  Flux<CreditLineMarkRepaymentRecord> findByPaymentTypeId(Integer paymentTypeId);
}
