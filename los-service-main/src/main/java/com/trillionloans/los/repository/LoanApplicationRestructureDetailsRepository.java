package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LoanApplicationRestructureDetailsEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanApplicationRestructureDetailsRepository
    extends R2dbcRepository<LoanApplicationRestructureDetailsEntity, Long> {

  @Query(
      "SELECT * FROM loan_application_restructure_details"
          + " WHERE lead = :lead AND eligibility = :eligibility AND restructure = :restructure"
          + " LIMIT 1")
  Mono<LoanApplicationRestructureDetailsEntity> findByLeadAndEligibilityAndRestructure(
      @Param("lead") Long lead,
      @Param("eligibility") Boolean eligibility,
      @Param("restructure") String restructure);
}
