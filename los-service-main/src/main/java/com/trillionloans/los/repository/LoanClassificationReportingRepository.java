package com.trillionloans.los.repository;

import com.trillionloans.los.model.request.m2p.LoanClassificationDetailsM2pLine;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Read-side queries for business-loan classification reporting / M2P sync. Uses {@code
 * document_number} on {@code business_loan_documents}.
 */
@Repository
@RequiredArgsConstructor
public class LoanClassificationReportingRepository {

  private static final String CLASSIFICATION_LINES_FOR_LOAN =
      """
      SELECT bldtl.business_name AS application_business_name,
             bldtl.business_address AS application_business_address,
             bldoc.tag AS document_name,
             bldoc.business_name AS document_business_name,
             bldoc.business_address AS document_business_address,
             bldoc.document_number AS document_number,
             bleval.name_match_score AS name_match_score,
             bleval.address_match_score AS address_match_score,
             bleval.is_document_uploaded AS is_uploaded,
             bleval.evaluation_status AS is_eligible
      FROM business_loan_document_evaluation bleval
      LEFT JOIN business_loan_details bldtl
        ON bleval.loan_application_id = bldtl.loan_application_id
      LEFT JOIN LATERAL (
        SELECT d.*
        FROM business_loan_documents d
        WHERE d.loan_application_id = bleval.loan_application_id
          AND d.tag = bleval.tag
        ORDER BY d.updated_at DESC NULLS LAST, d.id DESC
        LIMIT 1
      ) bldoc ON true
      WHERE bleval.loan_application_id = :loanApplicationId
      """;

  private final DatabaseClient databaseClient;

  public Flux<LoanClassificationDetailsM2pLine> findClassificationLinesForLoan(
      String loanApplicationId) {
    return databaseClient
        .sql(CLASSIFICATION_LINES_FOR_LOAN)
        .bind("loanApplicationId", loanApplicationId)
        .map(
            (row, meta) ->
                LoanClassificationDetailsM2pLine.builder()
                    .applicationBusinessName(row.get("application_business_name", String.class))
                    .applicationBusinessAddress(
                        row.get("application_business_address", String.class))
                    .documentName(row.get("document_name", String.class))
                    .documentBusinessName(row.get("document_business_name", String.class))
                    .documentBusinessAddress(row.get("document_business_address", String.class))
                    .documentNumber(row.get("document_number", String.class))
                    .nameMatchScore(toBigDecimal(row.get("name_match_score", Object.class)))
                    .addressMatchScore(toBigDecimal(row.get("address_match_score", Object.class)))
                    .isUploaded(row.get("is_uploaded", Boolean.class))
                    .isEligible(row.get("is_eligible", String.class))
                    .build())
        .all();
  }

  private static BigDecimal toBigDecimal(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof BigDecimal bd) {
      return bd;
    }
    if (raw instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    try {
      return new BigDecimal(raw.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
