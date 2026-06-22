package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.DobWaterfallLog;
import java.time.LocalDateTime;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface DobWaterfallResultRepository extends R2dbcRepository<DobWaterfallLog, Long> {

  Mono<DobWaterfallLog> findByClientIdAndProductCode(String clientId, String productCode);

  @Modifying
  @Query(
      """
      INSERT INTO dob_waterfall_result
      (client_id, product_code, result, pan_dob, aadhar_dob, rejection_reason, created_at)
      VALUES (
          :clientId, :productCode, :result, :panDob, :aadharDob, :rejectionReason, :createdAt
      )
      ON CONFLICT (client_id, product_code)
      DO UPDATE SET
          result = EXCLUDED.result,
          pan_dob = EXCLUDED.pan_dob,
          aadhar_dob = EXCLUDED.aadhar_dob,
          rejection_reason = EXCLUDED.rejection_reason,
          created_at = EXCLUDED.created_at
      """)
  Mono<Integer> upsertByClientIdAndProductCode(
      String clientId,
      String productCode,
      String result,
      String panDob,
      String aadharDob,
      String rejectionReason,
      LocalDateTime createdAt);
}
