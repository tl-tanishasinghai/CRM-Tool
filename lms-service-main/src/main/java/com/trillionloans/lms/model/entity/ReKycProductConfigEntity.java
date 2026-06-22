package com.trillionloans.lms.model.entity;

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
@Table("re_kyc_product_config")
public class ReKycProductConfigEntity {

  @Id private Long id;

  @Column("product_id")
  private Integer productId;

  @Column("product_code")
  private String productCode;

  @Column("dpd_written_off_days")
  private Integer dpdWrittenOffDays;
}
