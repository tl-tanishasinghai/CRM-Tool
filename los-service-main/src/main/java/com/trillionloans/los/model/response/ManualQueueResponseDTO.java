package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Paginated response for eligible loans in manual disbursal queue")
public class ManualQueueResponseDTO {

  @JsonProperty("data")
  private List<EligibleLoanDTO> data;

  @JsonProperty("totalCount")
  private Long totalCount;

  @JsonProperty("page")
  private Integer page;

  @JsonProperty("limit")
  private Integer limit;

  @JsonProperty("hasMore")
  private Boolean hasMore;
}
