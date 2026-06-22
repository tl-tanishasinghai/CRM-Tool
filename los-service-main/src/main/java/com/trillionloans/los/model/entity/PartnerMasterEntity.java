package com.trillionloans.los.model.entity;

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

  @Column("status")
  private String status;

  @Column("office_name")
  private String officeName;

  @Column("product_id_m2p")
  private String m2pProductId;

  @Column("is_remitx_enabled")
  @Builder.Default
  private Boolean isRemitXEnabled = false;
}
