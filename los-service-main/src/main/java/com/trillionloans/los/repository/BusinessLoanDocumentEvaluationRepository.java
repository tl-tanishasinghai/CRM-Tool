package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.BusinessLoanDocumentEvaluation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BusinessLoanDocumentEvaluationRepository
    extends R2dbcRepository<BusinessLoanDocumentEvaluation, Long> {

  Mono<BusinessLoanDocumentEvaluation> findByLoanApplicationIdAndTag(
      String loanApplicationId, String tag);

  Flux<BusinessLoanDocumentEvaluation> findByLoanApplicationId(String loanApplicationId);

  @Query(
      "SELECT COUNT(*) FROM business_loan_document_evaluation "
          + "WHERE loan_application_id = :loanApplicationId "
          + "AND evaluation_status IN ('QUALIFIED', 'NOT_QUALIFIED')")
  Mono<Long> countEvaluatedDocuments(String loanApplicationId);

  @Query(
      "SELECT COUNT(*) FROM business_loan_document_evaluation "
          + "WHERE loan_application_id = :loanApplicationId "
          + "AND evaluation_status = 'QUALIFIED'")
  Mono<Long> countQualifiedDocuments(String loanApplicationId);
}
