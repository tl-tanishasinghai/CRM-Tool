package com.trillionloans.los.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** response dto for reverse feed upload api. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseFeedUploadResponseDTO {

  private String batchId;

  private String status;

  private String message;

  private Integer totalRecords;

  private Integer successCount;

  private Integer failedCount;
}
