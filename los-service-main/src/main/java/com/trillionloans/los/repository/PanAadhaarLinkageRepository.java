package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.PanAadhaarLinkageEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PanAadhaarLinkageRepository
    extends R2dbcRepository<PanAadhaarLinkageEntity, Long> {
  Mono<PanAadhaarLinkageEntity> findByloanId(String loanId);
}
