package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.ChargeAuditLogHistoryEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface ChargeAuditLogHistoryRepository
    extends R2dbcRepository<ChargeAuditLogHistoryEntity, Long> {

  @Query(
      """
      INSERT INTO charge_audit_log_history
        (external_id,
         run_id,
         charge_name,
         product_code,
         charge_posted_date,
         m2p_charge_type_id,
         outstanding,
         base,
         gst,
         total,
         post_status,
         post_ref,
         message,
         created_at)
      VALUES
        (:externalId,
         :runId,
         :chargeName,
         :productCode,
         :chargePostedDate,
         :m2pChargeTypeId,
         :outstanding,
         :base,
         :gst,
         :total,
         :postStatus,
         :postRef,
         :message,
         now())
      RETURNING id
      """)
  Mono<Long> insertHistory(
      String externalId,
      Long runId,
      String chargeName,
      String productCode,
      LocalDate chargePostedDate,
      Long m2pChargeTypeId,
      BigDecimal outstanding,
      BigDecimal base,
      BigDecimal gst,
      BigDecimal total,
      String postStatus,
      String postRef,
      String message);
}
