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
@Table(name = "loan_client_partner_map")
public class LoanClientPartnerMapEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_app_id")
  private Integer loanApplicationId;

  @Column("client_id")
  private Integer clientId;

  @Column("lan_id")
  private Integer lanId;

  @Column("credit_line_id")
  private String lineId;

  @Column("partner_id")
  private Integer partnerId;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
