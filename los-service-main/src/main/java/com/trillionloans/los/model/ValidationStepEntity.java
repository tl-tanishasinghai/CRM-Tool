package com.trillionloans.los.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("client_validation_funnel_steps")
public class ValidationStepEntity {
  @Id
  @Column("id")
  private Long id;

  @Column("client_id")
  private String clientId;

  @Column("product_code")
  private String productCode;

  @Column("step_name")
  private String stepName;

  @Column("vendor")
  private String vendor;

  @Column("request")
  private String request;

  @Column("response")
  private String response;

  @Column("status")
  private String status;

  @Column("service_status")
  private String serviceStatus;
}
