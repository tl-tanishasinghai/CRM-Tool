package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing period details for the collection module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Period DTO for collection module")
public class PeriodDTO {

  private List<Integer> dueDate;
  private double principalDisbursed;
  private double principalLoanBalanceOutstanding;
  private double feeChargesDue;
  private double feeChargesPaid;
  private double totalOriginalDueForPeriod;
  private double totalDueForPeriod;
  private double totalPaidForPeriod;
  private double totalActualCostOfLoanForPeriod;
  private boolean recalculatedInterestComponent;
}
