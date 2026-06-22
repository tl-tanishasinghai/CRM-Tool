package com.trillionloans.los.model.entity;

import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDateTime;
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
@Table("callback")
public class CallbackLogEntity {

  @Id private Long id;

  @Column("product_code")
  private String productCode;

  @Column("type")
  private String type;

  @Column("request")
  private Json request;

  @Column("reference_id")
  private String referenceId;

  @Column("response")
  private Json response;

  @Column("exception")
  private String exception;

  @Column("uri")
  private String uri;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("is_retry")
  private Boolean isRetry;
}
