package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LoanApplicationMiscellaneousDetails;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanApplicationMiscellaneousDetailsRepository
    extends R2dbcRepository<LoanApplicationMiscellaneousDetails, Long> {

  Mono<LoanApplicationMiscellaneousDetails> findByLoanApplicationId(Integer loanApplicationId);
}
