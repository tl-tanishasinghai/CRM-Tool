package com.trillionloans.los.model.entity;

import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDateTime;
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
@Table("los_bre")
public class BreStatus {

  @Id
  @Column("id")
  private Long id;

  @Column("external_id")
  private String externalId;

  @Column("bre_type")
  private String breType;

  @Column("request")
  private Json request;

  @Column("response")
  private Json response;

  @Column("stage")
  private String stage;

  @Column("status")
  private String status;

  @Column("callback_id")
  private Long callbackId;

  @Column("is_active")
  private boolean isActive;

  @Column("product_code")
  private String productCode;

  @Column("scienaptic_status")
  private String scienapticStatus;

  @Column("retry_count")
  private Long retryCount;

  @Column("rejected_count")
  private Long rejectedCount;

  @Column("description")
  private String description;

  // This column stores the time when the entity was created
  @Column("created_at")
  private LocalDateTime createdAt;

  // This column stores the time when the entity was last updated
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
