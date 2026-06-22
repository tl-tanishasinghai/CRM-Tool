package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LoanDetails;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface LoanTagRepository extends ReactiveCrudRepository<LoanDetails, Long> {
  // Custom query to check if a tag already exists for this loan
  Mono<Long> countByLoanApplicationId(String loanApplicationId);
}
