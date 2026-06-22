package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * entity representing a reverse feed batch upload. tracks metadata about each bank response file
 * uploaded for processing.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("reverse_feed_batch")
public class ReverseFeedBatchEntity {

  @Id private Long id;

  @Column("batch_id")
  private UUID batchId;

  @Column("file_name")
  private String fileName;

  @Column("total_records")
  private Integer totalRecords;

  @Column("success_count")
  private Integer successCount;

  @Column("failed_count")
  private Integer failedCount;

  @Column("pending_count")
  private Integer pendingCount;

  @Column("status")
  private String status;

  @Column("uploaded_by")
  private String uploadedBy;

  @Column("uploaded_at")
  private LocalDateTime uploadedAt;

  @Column("completed_at")
  private LocalDateTime completedAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
