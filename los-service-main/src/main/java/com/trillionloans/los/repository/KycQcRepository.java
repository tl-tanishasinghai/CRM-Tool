package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.KycQcEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface KycQcRepository extends R2dbcRepository<KycQcEntity, Long> {

  @Query(
      """
      SELECT * FROM kyc_qc
      WHERE loan_application_id = :loanId
        AND final_name_match_status IN ('VERIFIED', 'REJECTED')
        AND final_face_match_status IN ('VERIFIED', 'REJECTED')
        AND is_deleted = false
      LIMIT 1
      """)
  Mono<KycQcEntity> findByLoanIdWithFinalizedStatuses(String loanId);
}
