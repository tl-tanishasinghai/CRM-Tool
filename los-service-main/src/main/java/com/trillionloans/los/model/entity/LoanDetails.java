package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("loan_details")
public class LoanDetails {
  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("is_psl")
  private Boolean isPsl;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
