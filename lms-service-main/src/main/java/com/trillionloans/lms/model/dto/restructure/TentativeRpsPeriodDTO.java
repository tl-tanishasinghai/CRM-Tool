package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Repayment period within tentative RPS response.
 *
 * @author Pawan Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Repayment period")
public class TentativeRpsPeriodDTO {

  @Schema(description = "Period number")
  private Integer period;

  @Schema(description = "From date [year, month, day]")
  private List<Integer> fromDate;

  @Schema(description = "Due date [year, month, day]")
  private List<Integer> dueDate;

  @Schema(description = "Days in period")
  private Integer daysInPeriod;

  @Schema(description = "Principal disbursed")
  private Double principalDisbursed;

  @Schema(description = "Principal loan balance outstanding")
  private Double principalLoanBalanceOutstanding;

  @Schema(description = "Principal original due")
  private Double principalOriginalDue;

  @Schema(description = "Principal due")
  private Double principalDue;

  @Schema(description = "Principal outstanding")
  private Double principalOutstanding;

  @Schema(description = "Interest original due")
  private Double interestOriginalDue;

  @Schema(description = "Interest due")
  private Double interestDue;

  @Schema(description = "Interest outstanding")
  private Double interestOutstanding;

  @Schema(description = "Fee charges due")
  private Double feeChargesDue;

  @Schema(description = "Fee charges outstanding")
  private Double feeChargesOutstanding;

  @Schema(description = "Penalty charges due")
  private Double penaltyChargesDue;

  @Schema(description = "Total original due for period")
  private Double totalOriginalDueForPeriod;

  @Schema(description = "Total due for period")
  private Double totalDueForPeriod;

  @Schema(description = "Total paid for period")
  private Double totalPaidForPeriod;

  @Schema(description = "Total outstanding for period")
  private Double totalOutstandingForPeriod;

  @Schema(description = "Total overdue")
  private Double totalOverdue;

  @Schema(description = "Total actual cost of loan for period")
  private Double totalActualCostOfLoanForPeriod;

  @Schema(description = "Total installment amount for period")
  private Double totalInstallmentAmountForPeriod;

  @Schema(description = "Recalculated interest component")
  private Boolean recalculatedInterestComponent;

  @Schema(description = "Loan repayment period computation details")
  private Object loanRepaymentPeriodComputationDetails;
}
