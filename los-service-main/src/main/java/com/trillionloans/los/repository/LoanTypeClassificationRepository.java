package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LoanTypeClassification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanTypeClassificationRepository
    extends R2dbcRepository<LoanTypeClassification, Long> {

  Mono<LoanTypeClassification> findByLoanApplicationId(String loanApplicationId);
}
