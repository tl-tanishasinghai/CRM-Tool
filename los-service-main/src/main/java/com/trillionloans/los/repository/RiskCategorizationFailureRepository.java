package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.RiskCategorizationFailureEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RiskCategorizationFailureRepository
    extends R2dbcRepository<RiskCategorizationFailureEntity, Integer> {
  Flux<RiskCategorizationFailureEntity> findByStatus(String status);

  Mono<RiskCategorizationFailureEntity> findByLoanApplicationId(String loanApplicationId);
}
