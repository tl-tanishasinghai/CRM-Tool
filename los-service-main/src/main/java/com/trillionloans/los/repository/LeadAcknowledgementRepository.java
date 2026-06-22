package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LeadAcknowledgement;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LeadAcknowledgementRepository extends R2dbcRepository<LeadAcknowledgement, Long> {

  Mono<LeadAcknowledgement> findByLoanId(String loanId);
}
