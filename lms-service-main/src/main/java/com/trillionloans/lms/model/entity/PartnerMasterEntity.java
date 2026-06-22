package com.trillionloans.lms.model.entity;

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
@Table("partner_master")
public class PartnerMasterEntity {

  @Id private long id;

  @Column("partner_id")
  private String partnerId;

  @Column("partner_name")
  private String partnerName;

  @Column("product_code")
  private String productCode;

  @Column("product_name")
  private String productName;

  @Column("product_type")
  private String productType;

  @Column("partner_code")
  private String partnerCode;

  @Column("status")
  private String status;
}
