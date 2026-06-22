package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.RuleEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DynamicRuleRepository extends R2dbcRepository<RuleEntity, Long> {
  Flux<RuleEntity> findByActiveTrueOrderByPriorityAsc();

  Flux<RuleEntity> findByActiveTrueAndTypeAndProductCodeOrderByPriorityAsc(
      String type, String productCode);
}
