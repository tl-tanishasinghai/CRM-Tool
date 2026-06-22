package com.trillionloans.los.model.entity;

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
@Table(name = "business_loan_documents")
public class BusinessLoanDocument {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("business_name")
  private String businessName;

  @Column("business_address")
  private String businessAddress;

  @Column("document_number")
  private String documentNumber;

  @Column("tag")
  private String tag;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
