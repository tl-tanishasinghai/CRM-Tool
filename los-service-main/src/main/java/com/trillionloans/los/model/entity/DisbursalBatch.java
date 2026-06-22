package com.trillionloans.los.model.entity;

import com.trillionloans.los.constant.BatchStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "disbursal_batch")
public class DisbursalBatch {

  @Id
  @Column("id")
  private UUID batchId;

  @Column("batch_status")
  private BatchStatus batchStatus;

  @Column("net_amount")
  private Double netAmount;

  @Column("total_records")
  private Integer totalRecords;

  @Column("hydrated_records")
  private Integer hydratedRecords;

  @Column("file_checksum")
  private String fileChecksum;

  @Column("error_details")
  private String errorDetails;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("created_by")
  private String createdBy;

  @Version
  @Column("version")
  private Integer version;

  /** Soft delete flag */
  @Column("is_deleted")
  private Boolean isDeleted;
}
