package com.trillionloans.los.model.entity;

import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("loan_insurance_details")
public class LoanInsuranceDetailsEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private Integer loanApplicationId;

  @Column("client_id")
  private Integer clientId;

  @Column("product_id")
  private String productId;

  @Column("premium_grid")
  private Json premiumGrid;

  @Column("is_charge_added")
  private Boolean isChargeAdded;

  @Column("is_opted")
  private Boolean isOpted;

  @Column("premium_amount")
  private Double premiumAmount;

  @Column("disbursed_approved_amount")
  private Integer disbursedApprovedAmount;

  @Column("sum_insured")
  private Double sumInsured;

  @Column("policy_no")
  private String policyNumber;

  @Column("doc_url")
  private String docUrl;

  @Column("m2p_doc_id")
  private Integer m2pDocId;

  @Column("status")
  private String status;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
