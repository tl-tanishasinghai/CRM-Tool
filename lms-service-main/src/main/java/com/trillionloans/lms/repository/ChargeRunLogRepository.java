package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.ChargeRunLogEntity;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface ChargeRunLogRepository extends R2dbcRepository<ChargeRunLogEntity, Long> {

  Mono<ChargeRunLogEntity> findByRunDate(LocalDate runDate);
}
