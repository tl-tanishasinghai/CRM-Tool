package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LoanInsuranceDetailsEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanInsuranceDetailsRepository
    extends ReactiveCrudRepository<LoanInsuranceDetailsEntity, Long> {

  Mono<LoanInsuranceDetailsEntity> findFirstByLoanApplicationIdOrderByIdDesc(
      Integer loanApplicationId);
}
