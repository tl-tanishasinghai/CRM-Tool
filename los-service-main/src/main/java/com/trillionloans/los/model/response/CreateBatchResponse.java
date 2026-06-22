package com.trillionloans.los.model.response;

import com.trillionloans.los.constant.BatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Create Batch response")
public class CreateBatchResponse {
  private UUID batchId;
  private BatchStatus batchStatus;
  private String message;
}
