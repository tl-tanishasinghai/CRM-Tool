package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchHistoryResponse {

  private List<BatchHistoryItemDTO> batches;

  @JsonProperty("totalCount")
  private Long totalCount;

  @JsonProperty("page")
  private Integer page;

  @JsonProperty("limit")
  private Integer limit;

  @JsonProperty("hasMore")
  private Boolean hasMore;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class BatchHistoryItemDTO {
    private UUID batchId;
    private String status;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private String createdBy;
  }
}
