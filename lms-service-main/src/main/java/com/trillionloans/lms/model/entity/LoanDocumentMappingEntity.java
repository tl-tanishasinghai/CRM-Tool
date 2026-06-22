package com.trillionloans.lms.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("document_mapping")
public class LoanDocumentMappingEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_account_number")
  private String loanAccountNumber;

  @Column("loan_application_number")
  private String loanApplicationNumber;

  @Column("document_path")
  private String documentPath;

  @Column("created_at")
  private LocalDateTime createdAt;
}
