package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.ClientConsentEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ClientConsentRepository extends R2dbcRepository<ClientConsentEntity, Long> {

  Mono<ClientConsentEntity> findTopByClientIdOrderByCreatedAtDesc(String clientId);
}
