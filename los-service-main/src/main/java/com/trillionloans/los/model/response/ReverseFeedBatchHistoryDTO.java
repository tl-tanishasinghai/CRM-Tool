package com.trillionloans.los.model.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** dto for reverse feed batch history item. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseFeedBatchHistoryDTO {

  private String batchId;

  private String fileName;

  private String status;

  private Integer totalRecords;

  private Integer successCount;

  private Integer failedCount;

  private String uploadedBy;

  private LocalDateTime uploadedAt;

  private LocalDateTime completedAt;
}
