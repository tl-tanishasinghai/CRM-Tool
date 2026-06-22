package com.trillionloans.los.service.db;

import com.trillionloans.los.model.ValidationStepEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientValidationFunnelStepsRepository
    extends ReactiveCrudRepository<ValidationStepEntity, Long> {
  Mono<Void> deleteByClientId(String clientId);

  Flux<ValidationStepEntity> findByClientIdAndProductCode(String clientId, String productCode);

  Mono<ValidationStepEntity> findByClientIdAndProductCodeAndStepNameAndVendor(
      String clientId, String productCode, String stepName, String vendor);

  Flux<ValidationStepEntity> findByStepName(String stepName);

  @Query(
      """
      INSERT INTO client_validation_funnel_steps (
          client_id, product_code, step_name, vendor, status, service_status, request, response
      )
      VALUES (
          :clientId, :productCode, :stepName, :vendor, :status, :serviceStatus, :request, :response
      )
      ON CONFLICT (client_id, product_code, step_name, vendor)
      DO UPDATE SET
          status = EXCLUDED.status,
          service_status = EXCLUDED.service_status,
                    request = EXCLUDED.request,
          response = EXCLUDED.response
      RETURNING *;
      """)
  Mono<ValidationStepEntity> upsertStep(
      String clientId,
      String productCode,
      String stepName,
      String vendor,
      String status,
      String serviceStatus,
      String request,
      String response);
}
