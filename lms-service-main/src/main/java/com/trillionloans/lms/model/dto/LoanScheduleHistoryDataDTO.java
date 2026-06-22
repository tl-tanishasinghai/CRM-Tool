package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing schedule history data for a loan in the collection
 * module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Loan Schedule History Data DTO for collection module")
public class LoanScheduleHistoryDataDTO {

  private int historyVersion;
  private List<Integer> createdDate;
}
