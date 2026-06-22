package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.DisbursalBatch;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DisbursalBatchRepository extends R2dbcRepository<DisbursalBatch, UUID> {

  Flux<DisbursalBatch> findByBatchStatusAndCreatedAtBetween(
      String batchStatus, LocalDateTime fromDate, LocalDateTime toDate);

  @Query(
      "SELECT * FROM disbursal_batch"
          + " WHERE batch_status IN (:statuses)"
          + " AND created_at BETWEEN :fromDate AND :toDate"
          + " ORDER BY created_at DESC"
          + " LIMIT :limit OFFSET :offset")
  Flux<DisbursalBatch> findByStatusesAndDateRangePaginated(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate, int limit, int offset);

  @Query(
      "SELECT COUNT(*) FROM disbursal_batch"
          + " WHERE batch_status IN (:statuses)"
          + " AND created_at BETWEEN :fromDate AND :toDate")
  Mono<Long> countByStatusesAndDateRange(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate);

  @Query(
      "SELECT * FROM disbursal_batch"
          + " WHERE created_at BETWEEN :fromDate AND :toDate"
          + " ORDER BY created_at DESC"
          + " LIMIT :limit OFFSET :offset")
  Flux<DisbursalBatch> findByDateRangePaginated(
      LocalDateTime fromDate, LocalDateTime toDate, int limit, int offset);

  @Query("SELECT COUNT(*) FROM disbursal_batch" + " WHERE created_at BETWEEN :fromDate AND :toDate")
  Mono<Long> countByDateRange(LocalDateTime fromDate, LocalDateTime toDate);
}
