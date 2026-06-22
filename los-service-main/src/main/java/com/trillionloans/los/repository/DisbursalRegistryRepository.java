package com.trillionloans.los.repository;

import com.trillionloans.los.constant.DisbursalStatus;
import com.trillionloans.los.model.entity.DisbursalRegistryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DisbursalRegistryRepository
    extends R2dbcRepository<DisbursalRegistryEntity, Long> {

  /** Find by reference_id2 (loan_account_number / line_id) and is_deleted flag. */
  Mono<DisbursalRegistryEntity> findByReferenceId2AndIsDeleted(
      String referenceId2, Boolean isDeleted);

  /**
   * Find by reference_id1 (loan_application_id / transaction_id), disburse_status and is_deleted
   * flag.
   */
  Mono<DisbursalRegistryEntity> findByReferenceId1AndDisburseStatusAndIsDeleted(
      String referenceId1, DisbursalStatus disburseStatus, Boolean isDeleted);

  /** Find eligible entries by product_code and disburse_status with pagination. */
  @Query(
      "SELECT * FROM disbursal_registry"
          + " WHERE product_code = :productCode"
          + " AND disburse_status = :disburseStatus"
          + " AND is_deleted = false"
          + " ORDER BY created_at DESC"
          + " LIMIT :limit OFFSET :offset")
  Flux<DisbursalRegistryEntity> findByProductCodeAndDisburseStatusPaginated(
      String productCode, String disburseStatus, int limit, int offset);

  /** Count eligible entries by product_code and disburse_status. */
  @Query(
      "SELECT COUNT(*) FROM disbursal_registry"
          + " WHERE product_code = :productCode"
          + " AND disburse_status = :disburseStatus"
          + " AND is_deleted = false")
  Mono<Long> countByProductCodeAndDisburseStatus(String productCode, String disburseStatus);

  /** Find eligible entries by multiple product_codes and disburse_status with pagination. */
  @Query(
      "SELECT * FROM disbursal_registry"
          + " WHERE product_code IN (:productCodes)"
          + " AND disburse_status = :disburseStatus"
          + " AND is_deleted = false"
          + " ORDER BY created_at DESC"
          + " LIMIT :limit OFFSET :offset")
  Flux<DisbursalRegistryEntity> findByProductCodesInAndDisburseStatusPaginated(
      List<String> productCodes, String disburseStatus, int limit, int offset);

  /** Count eligible entries by multiple product_codes and disburse_status. */
  @Query(
      "SELECT COUNT(*) FROM disbursal_registry"
          + " WHERE product_code IN (:productCodes)"
          + " AND disburse_status = :disburseStatus"
          + " AND is_deleted = false")
  Mono<Long> countByProductCodesInAndDisburseStatus(
      List<String> productCodes, String disburseStatus);

  @Query(
      "SELECT dr.* FROM disbursal_registry dr"
          + " INNER JOIN partner_master pm ON dr.product_code = pm.product_code"
          + " WHERE pm.is_remitx_enabled = true"
          + " AND dr.disburse_status = :disburseStatus"
          + " AND dr.is_deleted = false"
          + " ORDER BY dr.created_at DESC"
          + " LIMIT :limit OFFSET :offset")
  Flux<DisbursalRegistryEntity> findByRemitXEnabledAndDisburseStatusPaginated(
      String disburseStatus, int limit, int offset);

  @Query(
      "SELECT COUNT(*) FROM disbursal_registry dr"
          + " INNER JOIN partner_master pm ON dr.product_code = pm.product_code"
          + " WHERE pm.is_remitx_enabled = true"
          + " AND dr.disburse_status = :disburseStatus"
          + " AND dr.is_deleted = false")
  Mono<Long> countByRemitXEnabledAndDisburseStatus(String disburseStatus);

  Mono<DisbursalRegistryEntity> findByReferenceId1AndIsDeleted(
      String referenceId1, Boolean isDeleted);

  Mono<DisbursalRegistryEntity> findByReferenceId1AndReferenceId2AndIsDeleted(
      String referenceId1, String referenceId2, Boolean isDeleted);

  Flux<DisbursalRegistryEntity> findByDisburseStatusAndIsDeleted(
      DisbursalStatus disbursalStatus, Boolean isDeleted);

  /** Find all registry entries belonging to a batch. */
  Flux<DisbursalRegistryEntity> findByBatchIdAndIsDeleted(UUID batchId, Boolean isDeleted);
}
