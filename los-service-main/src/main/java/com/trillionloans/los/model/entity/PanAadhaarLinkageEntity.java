package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("pan_aadhaar_linkage_details")
public class PanAadhaarLinkageEntity {
  private Long id;

  @Column("product_code")
  private String productCode;

  @Column("client_id")
  private String clientId;

  @Column("loan_id")
  private String loanId;

  private String pan;

  private String aadhaar;

  private String linked;

  @Column("kyc_type")
  private String kycType;

  @Column("created_at")
  private LocalDateTime createdAt;
}
