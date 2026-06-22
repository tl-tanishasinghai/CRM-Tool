package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.LoanRestructureNotificationTrackingEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanRestructureNotificationTrackingRepository
    extends R2dbcRepository<LoanRestructureNotificationTrackingEntity, Long> {

  Mono<LoanRestructureNotificationTrackingEntity> findTopByRestructureDetailsIdOrderByIdDesc(
      Long restructureDetailsId);
}
