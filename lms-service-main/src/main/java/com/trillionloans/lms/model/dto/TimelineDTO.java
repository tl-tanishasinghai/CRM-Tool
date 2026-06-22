package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing timeline details for the collection module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Timeline DTO for collection module")
public class TimelineDTO {

  private List<Integer> expectedDisbursementDate;
  private List<Integer> actualDisbursementDate;
}
