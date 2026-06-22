package com.trillionloans.los.service.db;

import com.trillionloans.los.model.ClientValidationFunnelStatusEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ClientValidationFunnelStatusRepository
    extends ReactiveCrudRepository<ClientValidationFunnelStatusEntity, String> {
  Mono<ClientValidationFunnelStatusEntity> findByClientId(String clientId);

  Mono<ClientValidationFunnelStatusEntity> findByClientIdAndProductCode(
      String clientId, String productCode);

  @Query(
      """
          INSERT INTO client_validation_funnel_status (client_id, product_code, final_status)
          VALUES (:clientId, :productCode, :finalStatus)
          ON CONFLICT (client_id, product_code)
          DO UPDATE SET
              final_status = EXCLUDED.final_status
          RETURNING *;
      """)
  Mono<ClientValidationFunnelStatusEntity> upsertFunnelStatus(
      String clientId, String productCode, String finalStatus);

  @Query(
      """
          SELECT * FROM client_validation_funnel_status
          WHERE client_id = :clientId AND product_code = :productCode
          ORDER BY id DESC
          LIMIT 1;
      """)
  Mono<ClientValidationFunnelStatusEntity> findLatestByClientIdAndProductCode(
      String clientId, String productCode);
}
