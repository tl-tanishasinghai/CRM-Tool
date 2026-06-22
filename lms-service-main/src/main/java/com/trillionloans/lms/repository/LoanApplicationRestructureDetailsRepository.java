package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.LoanApplicationRestructureDetailsEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for loan_application_restructure_details table.
 *
 * @author Pawan Kumar
 */
@Repository
public interface LoanApplicationRestructureDetailsRepository
    extends R2dbcRepository<LoanApplicationRestructureDetailsEntity, Long> {

  Mono<LoanApplicationRestructureDetailsEntity> findFirstByLanOrderByIdDesc(Long lan);

  Mono<LoanApplicationRestructureDetailsEntity> findByLanAndRestructureId(
      Long lan, Long restructureId);
}
