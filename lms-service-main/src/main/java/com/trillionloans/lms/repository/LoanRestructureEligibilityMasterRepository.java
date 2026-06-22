package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.LoanRestructureEligibilityMasterEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for loan_restructure_eligibility_master table.
 *
 * @author Pawan Kumar
 */
@Repository
public interface LoanRestructureEligibilityMasterRepository
    extends R2dbcRepository<LoanRestructureEligibilityMasterEntity, Long> {

  Mono<LoanRestructureEligibilityMasterEntity> findFirstByLan(Long lan);
}
