package com.trillionloans.los.model.entity;

import io.r2dbc.postgresql.codec.Json;
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
@Table("product_config_master")
public class ProductConfigMasterEntity {
  @Id private long id;

  @Column("product_code")
  private String productCode;

  // partnerCode in ProductConfigMasterEntity and partnerName in PartnerMasterEntity
  // are for different purposes. Values of both the fields may or may not be same
  @Column("partner_code")
  private String partnerCode;

  @Column("product_json")
  private Json productControl;
}
