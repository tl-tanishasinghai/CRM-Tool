package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.PartnerMasterEntity;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PartnerMasterRepository extends R2dbcRepository<PartnerMasterEntity, Long> {
  Mono<PartnerMasterEntity> findByPartnerIdAndStatus(String partnerId, String status);

  Mono<PartnerMasterEntity> findByPartnerId(String partnerId);

  Mono<PartnerMasterEntity> findByProductCode(String productCode);

  Mono<PartnerMasterEntity> findByProductCodeAndIsRemitXEnabled(
      String productCode, Boolean isRemitXEnabled);

  Flux<PartnerMasterEntity> findByIsRemitXEnabled(Boolean isRemitXEnabled);

  Flux<PartnerMasterEntity> findByProductCodeInAndIsRemitXEnabled(
      List<String> productCodes, Boolean isRemitXEnabled);

  Mono<PartnerMasterEntity> findByM2pProductId(String m2pProductId);

  @Query("SELECT partner_id FROM partner_master WHERE product_code = :productCode")
  Mono<String> findPartnerIdByProductCode(@Param("productCode") String productCode);
}
