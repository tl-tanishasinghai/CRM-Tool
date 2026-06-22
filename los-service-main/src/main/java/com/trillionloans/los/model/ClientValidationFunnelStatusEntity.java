package com.trillionloans.los.model;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("client_validation_funnel_status")
public class ClientValidationFunnelStatusEntity {
  @Id
  @Column("id")
  private Long id;

  @Column("client_id")
  private String clientId;

  @Column("final_status")
  private String finalStatus;

  @Column("product_code")
  private String productCode;
}
