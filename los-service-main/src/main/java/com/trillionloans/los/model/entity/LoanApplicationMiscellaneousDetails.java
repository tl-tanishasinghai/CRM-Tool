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
@Table(name = "loan_application_miscellaneous_details")
public class LoanApplicationMiscellaneousDetails {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private Integer loanApplicationId;

  @Column("client_id")
  private Integer clientId;

  @Column("product_code")
  private String productCode;

  @Column("details")
  private String details;

  @Column("created_at")
  private LocalDateTime createdAt;
}
