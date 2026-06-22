package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.MandateRegistrationDetailsEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MandateRegistrationDetailsRepository
    extends R2dbcRepository<MandateRegistrationDetailsEntity, Long> {
  Mono<MandateRegistrationDetailsEntity> findByClientIdAndLoanIdAndMandateId(
      String clientId, String loanId, String mandateId);

  Mono<MandateRegistrationDetailsEntity> findByMandateId(String mandateId);
}
