package com.trillionloans.los.model.entity;

import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("customer_data_variance")
public class CustomerDataVariance {

  @Id private Long id;

  @Column("client_id")
  private String clientId;

  @Column("changed_fields")
  private Json changedFields;

  @Column("detected_at")
  private LocalDateTime detectedAt;
}
