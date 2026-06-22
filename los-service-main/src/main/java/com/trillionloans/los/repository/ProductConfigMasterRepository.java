package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.ProductConfigMasterEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ProductConfigMasterRepository
    extends R2dbcRepository<ProductConfigMasterEntity, Long> {
  Mono<ProductConfigMasterEntity> findProductConfigMasterByProductCode(String productCode);

  Mono<Void> deleteByProductCode(String productCode);
}
