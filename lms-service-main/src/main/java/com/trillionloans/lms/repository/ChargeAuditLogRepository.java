package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.ChargeAuditLogEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChargeAuditLogRepository extends R2dbcRepository<ChargeAuditLogEntity, Long> {

  Mono<Boolean> existsByExternalIdAndPostStatus(String externalId, String postStatus);

  @Query(
      """
  SELECT EXISTS (
    SELECT 1
    FROM charge_audit_log
    WHERE external_id = :externalId
      AND (
        post_status = 'POSTED'
        OR (post_status = 'SKIPPED' AND (post_ref = 'DUPLICATE' OR post_ref ILIKE 'HTTP 403 DUPLICATE'))
      )
  )
""")
  Mono<Boolean> existsAsTerminalDuplicate(String externalId);

  @Query(
      """
      SELECT *
      FROM charge_audit_log
      WHERE loan_id = :loanId
      ORDER BY id
      """)
  Flux<ChargeAuditLogEntity> findByLoanId(Long loanId);

  @Query(
      """
      INSERT INTO charge_audit_log
        (external_id, run_id, loan_id, installment_no, short_code, charge_name, product_code,
         charge_date, charge_posted_date,
         m2p_charge_type_id, outstanding, base, gst, total,
         post_status, post_ref, message, created_at, updated_at)
      VALUES
        (:externalId, :runId, :loanId, :installmentNo, :shortCode, :chargeName, :productCode,
         :chargeDate, :chargePostedDate,
         :m2pChargeTypeId, :outstanding, :base, :gst, :total,
         :postStatus, :postRef, :message, now(), now())
      ON CONFLICT (external_id) DO UPDATE
      SET run_id             = EXCLUDED.run_id,
          post_status        = EXCLUDED.post_status,
          post_ref           = CASE
                                 WHEN charge_audit_log.post_status = 'SKIPPED'
                                      AND charge_audit_log.post_ref = 'POSTING_DISABLED'
                                    THEN 'POSTING_DISABLED_DUPLICATE'
                                 ELSE EXCLUDED.post_ref
                               END,
          message            = EXCLUDED.message,
          outstanding        = EXCLUDED.outstanding,
          base               = EXCLUDED.base,
          gst                = EXCLUDED.gst,
          total              = EXCLUDED.total,
          charge_date        = EXCLUDED.charge_date,
          charge_posted_date = EXCLUDED.charge_posted_date,
          m2p_charge_type_id = EXCLUDED.m2p_charge_type_id,
          updated_at         = now()
      RETURNING id
      """)
  Mono<Long> upsertAudit(
      String externalId,
      Long runId,
      Long loanId,
      Integer installmentNo,
      String shortCode,
      String chargeName,
      String productCode,
      LocalDate chargeDate,
      LocalDate chargePostedDate,
      Long m2pChargeTypeId,
      BigDecimal outstanding,
      BigDecimal base,
      BigDecimal gst,
      BigDecimal total,
      String postStatus,
      String postRef,
      String message);

  @Query(
      """
      UPDATE charge_audit_log
      SET run_id = :runId,
          post_status = :postStatus,
          post_ref = :postRef,
          message = :message,
          updated_at = now()
      WHERE external_id = :externalId
      """)
  Mono<Long> updateSkipStatus(
      String externalId, Long runId, String postStatus, String postRef, String message);
}
