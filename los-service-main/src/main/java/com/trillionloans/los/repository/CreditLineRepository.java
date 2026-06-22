package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.CreditLineEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CreditLineRepository extends R2dbcRepository<CreditLineEntity, Long> {

  Mono<CreditLineEntity> findByLeadId(String leadId);

  Mono<CreditLineEntity> findByLeadIdAndProductCode(String leadId, String productCode);

  /**
   * Finds the latest credit line for a given leadId and productCode, ordered by createdAt
   * descending. This handles cases where multiple credit lines exist for the same lead and product.
   *
   * @param leadId the lead ID
   * @param productCode the product code
   * @return Mono containing the latest credit line entity, or empty if not found
   */
  Mono<CreditLineEntity> findFirstByLeadIdAndProductCodeOrderByCreatedAtDesc(
      String leadId, String productCode);

  Mono<CreditLineEntity> findByM2pCreditLineId(String m2pCreditLineId);
}
