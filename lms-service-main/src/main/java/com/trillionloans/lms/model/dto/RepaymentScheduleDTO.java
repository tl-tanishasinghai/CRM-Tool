package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing repayment schedule details for the collection module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Repayment Schedule DTO for collection module")
public class RepaymentScheduleDTO {

  private CurrencyDTO currency;
  private int loanTermInDays;
  private double totalPrincipalDisbursed;
  private double totalPrincipalExpected;
  private double totalPrincipalPaid;
  private double totalInterestCharged;
  private double totalFeeChargesCharged;
  private double totalPenaltyChargesCharged;
  private double totalWaived;
  private double totalWrittenOff;
  private double totalRepaymentExpected;
  private double totalRepayment;
  private double totalPaidInAdvance;
  private double totalPaidLate;
  private double totalOutstanding;
  private double totalAdvancePayment;
  private List<PeriodDTO> periods;
}
