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
@Table(name = "credit_line")
public class CreditLineEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("lead_id")
  private String leadId;

  @Column("product_code")
  private String productCode;

  @Column("credit_limit")
  private BigDecimal creditLimit;

  @Column("tenure_type")
  private String tenureType;

  @Column("tenure_value")
  private Integer tenureValue;

  @Column("m2p_credit_line_id")
  private String m2pCreditLineId;

  @Column("status")
  private String status;

  @Column("limit_created_at")
  private LocalDateTime limitCreatedAt;

  @Column("limit_approved_at")
  private LocalDateTime limitApprovedAt;

  @Column("limit_activated_at")
  private LocalDateTime limitActivatedAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
