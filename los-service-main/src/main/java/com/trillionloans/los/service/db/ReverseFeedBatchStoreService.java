package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;

import com.trillionloans.los.model.entity.ReverseFeedBatchEntity;
import com.trillionloans.los.repository.ReverseFeedBatchRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * service class for handling operations related to ReverseFeedBatch entities. provides methods to
 * save, update, and find reverse feed batches.
 */
@Service
@Slf4j
@AllArgsConstructor
public class ReverseFeedBatchStoreService {

  private final ReverseFeedBatchRepository reverseFeedBatchRepository;

  /** saves a new reverse feed batch. */
  public Mono<ReverseFeedBatchEntity> save(ReverseFeedBatchEntity batch) {
    log.info("[REVERSE_FEED_BATCH] saving new batch with batchId: {}", batch.getBatchId());

    batch.setCreatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
    batch.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));

    return reverseFeedBatchRepository
        .save(batch)
        .doOnSuccess(
            saved ->
                log.info("[REVERSE_FEED_BATCH] successfully saved batch: {}", saved.getBatchId()))
        .doOnError(
            error ->
                log.error(
                    "[REVERSE_FEED_BATCH] error saving batch: {}", batch.getBatchId(), error));
  }

  /** updates an existing reverse feed batch. */
  public Mono<ReverseFeedBatchEntity> update(ReverseFeedBatchEntity batch) {
    log.info("[REVERSE_FEED_BATCH] updating batch: {}", batch.getBatchId());
    batch.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
    return reverseFeedBatchRepository
        .save(batch)
        .doOnSuccess(
            saved ->
                log.info("[REVERSE_FEED_BATCH] successfully updated batch: {}", saved.getBatchId()))
        .doOnError(
            error ->
                log.error(
                    "[REVERSE_FEED_BATCH] error updating batch: {}", batch.getBatchId(), error));
  }

  /** finds a reverse feed batch by its batch id. */
  public Mono<ReverseFeedBatchEntity> findByBatchId(UUID batchId) {
    return reverseFeedBatchRepository.findByBatchId(batchId);
  }

  /** finds all batches with pagination. */
  public Flux<ReverseFeedBatchEntity> findAllWithPagination(int page, int limit) {
    int offset = page * limit;
    return reverseFeedBatchRepository.findAllWithPagination(limit, offset);
  }

  /** counts total batches. */
  public Mono<Long> countAll() {
    return reverseFeedBatchRepository.countAll();
  }

  public Flux<ReverseFeedBatchEntity> findByDateRangePaginated(
      LocalDateTime fromDate, LocalDateTime toDate, int page, int limit) {
    int offset = page * limit;
    return reverseFeedBatchRepository.findByDateRangePaginated(fromDate, toDate, limit, offset);
  }

  public Mono<Long> countByDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
    return reverseFeedBatchRepository.countByDateRange(fromDate, toDate);
  }

  public Flux<ReverseFeedBatchEntity> findByStatusesAndDateRangePaginated(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate, int page, int limit) {
    int offset = page * limit;
    return reverseFeedBatchRepository.findByStatusesAndDateRangePaginated(
        statuses, fromDate, toDate, limit, offset);
  }

  public Mono<Long> countByStatusesAndDateRange(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate) {
    return reverseFeedBatchRepository.countByStatusesAndDateRange(statuses, fromDate, toDate);
  }
}
