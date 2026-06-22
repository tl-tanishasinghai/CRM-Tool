package com.trillionloans.los.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "business_loan_document_evaluation")
public class BusinessLoanDocumentEvaluation {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("tag")
  private String tag;

  @Column("is_document_uploaded")
  private Boolean isDocumentUploaded;

  @Column("is_name_matched")
  private Boolean isNameMatched;

  @Column("is_address_matched")
  private Boolean isAddressMatched;

  @Column("name_match_score")
  private BigDecimal nameMatchScore;

  @Column("address_match_score")
  private BigDecimal addressMatchScore;

  @Column("evaluation_status")
  private String evaluationStatus;

  @Column("evaluated_at")
  private LocalDateTime evaluatedAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
