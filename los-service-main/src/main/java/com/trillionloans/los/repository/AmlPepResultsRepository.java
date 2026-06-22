package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.AmlPepResultsEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AmlPepResultsRepository extends R2dbcRepository<AmlPepResultsEntity, Long> {

  Mono<AmlPepResultsEntity> findFirstByClientIdAndLeadId(String clientId, String leadId);

  Mono<AmlPepResultsEntity> findByLeadId(String loanApplicationId);
}
