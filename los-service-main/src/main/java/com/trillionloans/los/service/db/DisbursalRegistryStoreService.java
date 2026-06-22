package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.EXISTING_TRANSACTION_FOUND;
import static com.trillionloans.los.constant.StringConstants.INITIATING_SAVE_UPDATE;

import com.trillionloans.los.constant.DisbursalStatus;
import com.trillionloans.los.model.entity.DisbursalRegistryEntity;
import com.trillionloans.los.repository.DisbursalRegistryRepository;
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
 * service class for handling operations related to DisbursalRegistry entities. provides methods to
 * update disbursal registry records for reverse feed processing.
 *
 * <p>unified naming convention: - reference_id1: loan_application_id (pl/cl) or transaction_id
 * (other products) - reference_id2: loan_account_number (pl/cl) or line_id (other products)
 */
@Service
@Slf4j
@AllArgsConstructor
public class DisbursalRegistryStoreService {

  private static final String LOG_HEADER = "[DISBURSAL_REGISTRY]";

  private final DisbursalRegistryRepository disbursalRegistryRepository;

  /**
   * Finds a registry entry by reference_id2 (loan_account_number / line_id) and is_deleted flag.
   */
  public Mono<DisbursalRegistryEntity> findByReferenceId2(String referenceId2, Boolean isDeleted) {
    return disbursalRegistryRepository.findByReferenceId2AndIsDeleted(referenceId2, isDeleted);
  }

  public Mono<DisbursalRegistryEntity> findByReferenceId1(String referenceId1, Boolean isDeleted) {
    return disbursalRegistryRepository.findByReferenceId1AndIsDeleted(referenceId1, isDeleted);
  }

  public Mono<DisbursalRegistryEntity> findByReferenceId1AndReferenceId2(
      String referenceId1, String referenceId2, Boolean isDeleted) {
    return disbursalRegistryRepository.findByReferenceId1AndReferenceId2AndIsDeleted(
        referenceId1, referenceId2, isDeleted);
  }

  /**
   * Finds a registry entry by reference_id1, disburse_status and is_deleted flag.
   *
   * @param referenceId1 loan_application_id (PL/CL) OR transaction_id (other products)
   * @param disburseStatus the expected disbursal status
   * @param isDeleted soft delete flag
   */
  public Mono<DisbursalRegistryEntity> findByReferenceId1AndDisburseStatus(
      String referenceId1, DisbursalStatus disburseStatus, Boolean isDeleted) {
    log.info(
        "{} finding registry entry for referenceId1: {}, status: {}, isDeleted: {}",
        LOG_HEADER,
        referenceId1,
        disburseStatus,
        isDeleted);
    return disbursalRegistryRepository.findByReferenceId1AndDisburseStatusAndIsDeleted(
        referenceId1, disburseStatus, isDeleted);
  }

  /** updates an existing disbursal registry entry. */
  public Mono<DisbursalRegistryEntity> update(DisbursalRegistryEntity entity) {
    log.info(
        "{} updating registry entry for referenceId1: {}", LOG_HEADER, entity.getReferenceId1());
    entity.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
    return disbursalRegistryRepository
        .save(entity)
        .doOnSuccess(
            saved ->
                log.info(
                    "{} successfully updated registry entry for referenceId1: {}",
                    LOG_HEADER,
                    saved.getReferenceId1()))
        .doOnError(
            error ->
                log.error(
                    "{} error updating registry entry for referenceId1: {}",
                    LOG_HEADER,
                    entity.getReferenceId1(),
                    error));
  }

  /**
   * updates an existing disbursal registry entity by referenceId1. updates disburse_type,
   * disburse_status, and optionally sets secondary_failure_reason if provided.
   *
   * @param disbursalRegistryEntity entity with referenceId1, disburseType, and disburseStatus
   * @param reason optional reason to set as secondary_failure_reason
   * @return updated entity or empty if not found
   */
  public Mono<DisbursalRegistryEntity> updateDisbursalRegistryEntity(
      DisbursalRegistryEntity disbursalRegistryEntity, String reason) {
    log.info(INITIATING_SAVE_UPDATE, disbursalRegistryEntity.getReferenceId1());

    return disbursalRegistryRepository
        .findByReferenceId1AndIsDeleted(disbursalRegistryEntity.getReferenceId1(), false)
        .flatMap(
            existingEntity -> {
              log.info(EXISTING_TRANSACTION_FOUND, disbursalRegistryEntity.getReferenceId1());
              existingEntity.setDisburseType(disbursalRegistryEntity.getDisburseType());
              existingEntity.setDisburseStatus(disbursalRegistryEntity.getDisburseStatus());
              existingEntity.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
              if (disbursalRegistryEntity.getGrossDisbursalAmount() != null) {
                existingEntity.setGrossDisbursalAmount(
                    disbursalRegistryEntity.getGrossDisbursalAmount());
              }
              if (disbursalRegistryEntity.getNetDisbursalAmount() != null) {
                existingEntity.setNetDisbursalAmount(
                    disbursalRegistryEntity.getNetDisbursalAmount());
              }
              if (reason != null && !reason.isEmpty()) {
                existingEntity.setSecondaryFailureReason(reason);
              }
              return save(existingEntity);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "{} registry entry not found for referenceId1: {}",
                      LOG_HEADER,
                      disbursalRegistryEntity.getReferenceId1());
                  return Mono.empty();
                }));
  }

  public Mono<DisbursalRegistryEntity> save(DisbursalRegistryEntity entity) {
    log.info("{} saving registry entry for referenceId1: {}", LOG_HEADER, entity.getReferenceId1());
    entity.setCreatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
    entity.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
    return disbursalRegistryRepository
        .save(entity)
        .doOnSuccess(
            saved ->
                log.info(
                    "{} successfully saved registry entry for referenceId1: {}",
                    LOG_HEADER,
                    saved.getReferenceId1()))
        .doOnError(
            error ->
                log.error(
                    "{} error saving registry entry for referenceId1: {}",
                    LOG_HEADER,
                    entity.getReferenceId1(),
                    error));
  }

  /**
   * marks a loan as successfully disbursed in the registry. updates disburse_status to SUCCESS and
   * sets utr number.
   *
   * @param referenceId2 loan_account_number (pl/cl) or line_id (other products)
   * @param utrNumber utr number from bank
   */
  public Mono<DisbursalRegistryEntity> markDisbursementSuccess(
      String referenceId1, String referenceId2, String utrNumber) {
    log.info(
        "{} marking disbursement success for referenceId2: {}, utr: {}",
        LOG_HEADER,
        referenceId2,
        utrNumber);

    return findByReferenceId1AndReferenceId2(referenceId1, referenceId2, false)
        .flatMap(
            entity -> {
              entity.setDisburseStatus(DisbursalStatus.SUCCESS);
              entity.setUtrNumber(utrNumber);
              return update(entity);
            })
        .doOnSuccess(
            updated ->
                log.info(
                    "{} successfully marked success for referenceId2: {}",
                    LOG_HEADER,
                    referenceId2))
        .doOnError(
            error ->
                log.error(
                    "{} error marking success for referenceId2: {}",
                    LOG_HEADER,
                    referenceId2,
                    error));
  }

  /**
   * marks a loan as failed/rejected in the registry. updates disburse_status to FAILED and sets
   * failure reason.
   *
   * @param referenceId2 loan_account_number (pl/cl) or line_id (other products)
   * @param failureReason reason for failure/rejection
   */
  public Mono<DisbursalRegistryEntity> markDisbursementFailed(
      String referenceId1, String referenceId2, String failureReason) {
    log.info(
        "{} marking disbursement failed for referenceId2: {}, reason: {}",
        LOG_HEADER,
        referenceId2,
        failureReason);

    return findByReferenceId1AndReferenceId2(referenceId1, referenceId2, false)
        .flatMap(
            entity -> {
              entity.setDisburseStatus(DisbursalStatus.REJECTED);
              entity.setFailureReason(failureReason);
              return update(entity);
            })
        .doOnSuccess(
            updated ->
                log.info(
                    "{} successfully marked failed for referenceId2: {}", LOG_HEADER, referenceId2))
        .doOnError(
            error ->
                log.error(
                    "{} error marking failed for referenceId2: {}",
                    LOG_HEADER,
                    referenceId2,
                    error));
  }

  /**
   * Finds eligible registry entries by product_code and disburse_status with pagination.
   *
   * @param productCode product code filter
   * @param disburseStatus disbursal status filter
   * @param page zero-based page number
   * @param limit number of items per page
   */
  public Flux<DisbursalRegistryEntity> findByProductCodeAndDisburseStatusPaginated(
      String productCode, DisbursalStatus disburseStatus, int page, int limit) {
    int offset = page * limit;
    return disbursalRegistryRepository.findByProductCodeAndDisburseStatusPaginated(
        productCode, disburseStatus.name(), limit, offset);
  }

  /**
   * Counts registry entries by product_code and disburse_status.
   *
   * @param productCode product code filter
   * @param disburseStatus disbursal status filter
   */
  public Mono<Long> countByProductCodeAndDisburseStatus(
      String productCode, DisbursalStatus disburseStatus) {
    return disbursalRegistryRepository.countByProductCodeAndDisburseStatus(
        productCode, disburseStatus.name());
  }

  public Flux<DisbursalRegistryEntity> findByProductCodesInAndDisburseStatusPaginated(
      List<String> productCodes, DisbursalStatus disburseStatus, int page, int limit) {
    int offset = page * limit;
    return disbursalRegistryRepository.findByProductCodesInAndDisburseStatusPaginated(
        productCodes, disburseStatus.name(), limit, offset);
  }

  public Mono<Long> countByProductCodesInAndDisburseStatus(
      List<String> productCodes, DisbursalStatus disburseStatus) {
    return disbursalRegistryRepository.countByProductCodesInAndDisburseStatus(
        productCodes, disburseStatus.name());
  }

  public Flux<DisbursalRegistryEntity> findByRemitXEnabledAndDisburseStatusPaginated(
      DisbursalStatus disburseStatus, int page, int limit) {
    int offset = page * limit;
    return disbursalRegistryRepository.findByRemitXEnabledAndDisburseStatusPaginated(
        disburseStatus.name(), limit, offset);
  }

  public Mono<Long> countByRemitXEnabledAndDisburseStatus(DisbursalStatus disburseStatus) {
    return disbursalRegistryRepository.countByRemitXEnabledAndDisburseStatus(disburseStatus.name());
  }

  public Flux<DisbursalRegistryEntity> findByDisbursalStatus(DisbursalStatus disbursalStatus) {
    return disbursalRegistryRepository.findByDisburseStatusAndIsDeleted(disbursalStatus, false);
  }

  /**
   * Find all registry entries belonging to a given batch.
   *
   * @param batchId the batch UUID
   */
  public Flux<DisbursalRegistryEntity> findByBatchId(UUID batchId) {
    return disbursalRegistryRepository.findByBatchIdAndIsDeleted(batchId, false);
  }
}
