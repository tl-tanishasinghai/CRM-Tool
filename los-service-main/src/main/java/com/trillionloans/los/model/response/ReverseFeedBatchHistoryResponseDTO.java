package com.trillionloans.los.model.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** paginated response dto for reverse feed batch history. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseFeedBatchHistoryResponseDTO {

  private List<ReverseFeedBatchHistoryDTO> batches;

  private Integer page;

  private Integer limit;

  private Long totalCount;

  private Boolean hasMore;
}
