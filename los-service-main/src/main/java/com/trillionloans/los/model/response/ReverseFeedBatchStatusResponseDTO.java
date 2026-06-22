package com.trillionloans.los.model.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** response dto for reverse feed batch status api. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseFeedBatchStatusResponseDTO {

  private String batchId;

  private String status;

  private String fileName;

  private Integer totalRecords;

  private Integer successCount;

  private Integer failedCount;

  private Integer pendingCount;

  private Integer percentage;

  private Boolean isDownloadable;

  private String uploadedBy;

  private LocalDateTime uploadedAt;

  private LocalDateTime completedAt;
}
