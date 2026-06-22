package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.ClientCreationRequestDetail;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ClientCreationRequestRepository
    extends R2dbcRepository<ClientCreationRequestDetail, Long> {
  /** Fetch Latest Entry Based on CLIENT ID & PRODUCT KEY */
  @Query(
      "SELECT * FROM client_creation_request_details "
          + "WHERE client_id = :clientId AND product_code = :productCode "
          + "ORDER BY created_at DESC LIMIT 1")
  Mono<ClientCreationRequestDetail> findLatestByClientIdAndProductCode(
      String clientId, String productCode);
}
