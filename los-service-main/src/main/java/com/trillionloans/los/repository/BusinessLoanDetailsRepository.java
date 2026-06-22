package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.BusinessLoanDetails;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BusinessLoanDetailsRepository extends R2dbcRepository<BusinessLoanDetails, Long> {

  Mono<BusinessLoanDetails> findByLoanApplicationId(String loanApplicationId);
}
