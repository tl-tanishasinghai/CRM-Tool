package com.trillionloans.los.model.entity;

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
@Table("qc_check")
public class QcCheckEntity {
  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("client_id")
  private String clientId;

  @Column("check_type")
  private String checkType;

  @Column("actual_threshold_value")
  private String breValue;

  @Column("loan_value")
  private String loanValue;

  @Column("conflict_field")
  private String conflictField;

  @Column("product_code")
  private String productCode;

  @Column("created_at")
  private LocalDateTime createdAt;
}
