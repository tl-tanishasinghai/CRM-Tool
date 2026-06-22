package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.ReverseFeedBatchItemEntity;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReverseFeedBatchItemRepository
    extends R2dbcRepository<ReverseFeedBatchItemEntity, Long> {

  Flux<ReverseFeedBatchItemEntity> findByBatchIdAndSyncStatus(UUID batchId, String syncStatus);

  Mono<Long> countByBatchIdAndSyncStatus(UUID batchId, String syncStatus);

  Flux<ReverseFeedBatchItemEntity> findByBatchId(UUID batchId);
}
