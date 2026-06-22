package com.trillionloans.los.model.response;

import com.trillionloans.los.constant.BatchStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchStatusResponse {

  private UUID batchId;

  private BatchStatus batchStatus;

  private Integer totalRecords;

  private Integer hydratedRecords;

  private Integer percentage;

  private Boolean isDownloadable;
}
