package com.trillionloans.lms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table("charge_audit_log")
public class ChargeAuditLogEntity {

  @Id private Long id;

  @Column("run_id")
  private Long runId;

  @Column("external_id")
  private String externalId;

  @Column("loan_id")
  private Long loanId;

  @Column("installment_no")
  private Integer installmentNo;

  @Column("short_code")
  private String shortCode;

  @Column("charge_name")
  private String chargeName;

  @Column("product_code")
  private String productCode;

  @Column("charge_date")
  private LocalDate chargeDate;

  @Column("charge_posted_date")
  private LocalDate chargePostedDate;

  @Column("m2p_charge_type_id")
  private Long m2pChargeTypeId;

  @Column("outstanding")
  private BigDecimal outstanding;

  @Column("base")
  private BigDecimal base;

  @Column("gst")
  private BigDecimal gst;

  @Column("total")
  private BigDecimal total;

  @Column("post_status")
  private String postStatus;

  @Column("post_ref")
  private String postRef;

  @Column("message")
  private String message;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
