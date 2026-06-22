package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.PartnerMasterEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PartnerMasterRepository extends R2dbcRepository<PartnerMasterEntity, Long> {
  Mono<PartnerMasterEntity> findByPartnerIdAndStatus(String partnerId, String status);

  Mono<PartnerMasterEntity> findByPartnerId(String partnerId);

  Flux<PartnerMasterEntity> findAllByStatus(String status);
}
