package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LoanLevelClientDetail;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface LoanLevelClientDetailRepository
    extends R2dbcRepository<LoanLevelClientDetail, Long> {

  @Query("SELECT * FROM loan_level_client_details WHERE loan_application_id = :loanApplicationId")
  Mono<LoanLevelClientDetail> findByLoanApplicationId(String loanApplicationId);

  @Query(
      "SELECT * FROM loan_level_client_details WHERE client_id = :clientId AND product_code ="
          + " :productCode ORDER BY created_at DESC LIMIT 1")
  Mono<LoanLevelClientDetail> findLatestByClientIdAndProductCode(
      String clientId, String productCode);
}
