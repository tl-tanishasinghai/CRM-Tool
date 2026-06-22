package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;

import com.trillionloans.los.model.entity.ReverseFeedBatchItemEntity;
import com.trillionloans.los.repository.ReverseFeedBatchItemRepository;
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
 * service class for handling operations related to ReverseFeedBatchItem entities. provides methods
 * to save, update, and find reverse feed batch items.
 */
@Service
@Slf4j
@AllArgsConstructor
public class ReverseFeedBatchItemStoreService {

  private final ReverseFeedBatchItemRepository reverseFeedBatchItemRepository;

  /** saves multiple reverse feed batch items. */
  public Flux<ReverseFeedBatchItemEntity> saveAll(List<ReverseFeedBatchItemEntity> items) {
    log.info("[REVERSE_FEED_ITEM] saving {} items", items.size());

    LocalDateTime now = LocalDateTime.now(ZoneId.of(ASIA_KOLKATA));
    items.forEach(
        item -> {
          item.setCreatedAt(now);
          item.setUpdatedAt(now);
        });

    return reverseFeedBatchItemRepository
        .saveAll(items)
        .doOnComplete(() -> log.info("[REVERSE_FEED_ITEM] successfully saved all items"))
        .doOnError(error -> log.error("[REVERSE_FEED_ITEM] error saving items", error));
  }

  /** updates an existing reverse feed batch item. */
  public Mono<ReverseFeedBatchItemEntity> update(ReverseFeedBatchItemEntity item) {
    log.info("[REVERSE_FEED_ITEM] updating item for referenceId1: {}", item.getReferenceId1());

    item.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));

    return reverseFeedBatchItemRepository
        .save(item)
        .doOnSuccess(
            saved ->
                log.info(
                    "[REVERSE_FEED_ITEM] successfully updated item for referenceId1: {}",
                    saved.getReferenceId1()))
        .doOnError(
            error ->
                log.error(
                    "[REVERSE_FEED_ITEM] error updating item for referenceId1: {}",
                    item.getReferenceId1(),
                    error));
  }

  /** finds items by batch id and sync status. */
  public Flux<ReverseFeedBatchItemEntity> findByBatchIdAndSyncStatus(
      UUID batchId, String syncStatus) {
    return reverseFeedBatchItemRepository.findByBatchIdAndSyncStatus(batchId, syncStatus);
  }

  /** counts items by batch id and sync status. */
  public Mono<Long> countByBatchIdAndSyncStatus(UUID batchId, String syncStatus) {
    return reverseFeedBatchItemRepository.countByBatchIdAndSyncStatus(batchId, syncStatus);
  }

  /** finds all items by batch id. */
  public Flux<ReverseFeedBatchItemEntity> findByBatchId(UUID batchId) {
    return reverseFeedBatchItemRepository.findByBatchId(batchId);
  }

  /** marks an item as successfully synced. m2pResponse should be set on item before calling. */
  public Mono<ReverseFeedBatchItemEntity> markSuccess(ReverseFeedBatchItemEntity item) {
    item.setSyncStatus("SUCCESS");
    item.setProcessedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
    return update(item);
  }

  /** marks an item as failed. m2pResponse should be set on item before calling if available. */
  public Mono<ReverseFeedBatchItemEntity> markFailed(
      ReverseFeedBatchItemEntity item, String errorMessage) {
    item.setSyncStatus("FAILED");
    item.setErrorMessage(errorMessage);
    item.setProcessedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
    return update(item);
  }
}
