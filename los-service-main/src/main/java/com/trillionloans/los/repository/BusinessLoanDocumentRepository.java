package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.BusinessLoanDocument;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BusinessLoanDocumentRepository
    extends R2dbcRepository<BusinessLoanDocument, Long> {

  /**
   * Compare {@code loan_application_id} as text so both integer and varchar column types work in
   * PostgreSQL (avoids R2DBC type mismatch / "bad SQL grammar" on derived queries).
   */
  @Query(
      "SELECT id, loan_application_id, business_name, business_address, document_number, tag,"
          + " created_at, updated_at FROM business_loan_documents WHERE loan_application_id ="
          + " :loanApplicationId")
  Flux<BusinessLoanDocument> findByLoanApplicationId(String loanApplicationId);

  @Query(
      "SELECT id, loan_application_id, business_name, business_address, document_number, tag,"
          + " created_at, updated_at FROM business_loan_documents WHERE loan_application_id ="
          + " :loanApplicationId AND tag = :tag")
  Mono<BusinessLoanDocument> findByLoanApplicationIdAndTag(String loanApplicationId, String tag);
}
