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
@Table(name = "loan_type_classification")
public class LoanTypeClassification {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("product_code")
  private String productCode;

  @Column("final_status")
  private String finalStatus;

  @Column("lsp_status")
  private String lspStatus;

  @Column("trillion_status")
  private String trillionStatus;

  /** Loan account number (LAN) from disbursement callback; matches DB type {@code INTEGER}. */
  @Column("lan")
  private Integer lan;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
