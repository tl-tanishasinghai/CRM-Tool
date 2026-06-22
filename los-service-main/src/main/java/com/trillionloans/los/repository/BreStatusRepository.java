package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.BreStatus;
import java.util.List;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BreStatusRepository extends R2dbcRepository<BreStatus, Long> {
  Mono<BreStatus> findByExternalIdAndBreTypeAndIsActive(
      String externalId, String type, boolean isActive);

  Mono<BreStatus> findByExternalIdAndBreType(String externalId, String type);

  Mono<BreStatus> findByExternalIdAndStatus(String externalId, String status);

  Mono<BreStatus> findByExternalIdAndStatusAndScienapticStatus(
      String externalId, String status, String scienapticStatus);

  Mono<BreStatus> findByExternalIdAndStatusAndScienapticStatusIn(
      String externalId, String status, List<String> scienapticStatuses);

  Mono<BreStatus> findByExternalIdAndStatusAndScienapticStatusAndStage(
      String externalId, String status, String scienapticStatus, String stage);
}
