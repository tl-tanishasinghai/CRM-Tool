package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.ReverseFeedBatchEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReverseFeedBatchRepository extends R2dbcRepository<ReverseFeedBatchEntity, Long> {

  Mono<ReverseFeedBatchEntity> findByBatchId(UUID batchId);

  @Query("SELECT * FROM reverse_feed_batch ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<ReverseFeedBatchEntity> findAllWithPagination(int limit, int offset);

  @Query("SELECT COUNT(*) FROM reverse_feed_batch")
  Mono<Long> countAll();

  @Query(
      "SELECT * FROM reverse_feed_batch"
          + " WHERE created_at BETWEEN :fromDate AND :toDate"
          + " ORDER BY created_at DESC"
          + " LIMIT :limit OFFSET :offset")
  Flux<ReverseFeedBatchEntity> findByDateRangePaginated(
      LocalDateTime fromDate, LocalDateTime toDate, int limit, int offset);

  @Query(
      "SELECT COUNT(*) FROM reverse_feed_batch" + " WHERE created_at BETWEEN :fromDate AND :toDate")
  Mono<Long> countByDateRange(LocalDateTime fromDate, LocalDateTime toDate);

  @Query(
      "SELECT * FROM reverse_feed_batch"
          + " WHERE status IN (:statuses)"
          + " AND created_at BETWEEN :fromDate AND :toDate"
          + " ORDER BY created_at DESC"
          + " LIMIT :limit OFFSET :offset")
  Flux<ReverseFeedBatchEntity> findByStatusesAndDateRangePaginated(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate, int limit, int offset);

  @Query(
      "SELECT COUNT(*) FROM reverse_feed_batch"
          + " WHERE status IN (:statuses)"
          + " AND created_at BETWEEN :fromDate AND :toDate")
  Mono<Long> countByStatusesAndDateRange(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate);
}
