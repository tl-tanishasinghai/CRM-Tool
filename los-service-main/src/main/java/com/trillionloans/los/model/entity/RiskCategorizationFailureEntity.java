package com.trillionloans.los.model.entity;

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
@Table("client_risk_categorization_failure")
public class RiskCategorizationFailureEntity {
  @Id private Integer id;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("status")
  private String status;

  @Column("last_updated_at")
  private String lastUpdatedAt;

  @Column("created_at")
  private String createdAt;
}
