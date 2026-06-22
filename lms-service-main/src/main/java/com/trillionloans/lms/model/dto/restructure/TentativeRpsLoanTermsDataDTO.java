package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Loan terms data within tentative RPS response.
 *
 * @author Pawan Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Loan terms data")
public class TentativeRpsLoanTermsDataDTO {

  @Schema(description = "Principal amount")
  private Double principal;

  @Schema(description = "Number of repayments")
  private Integer numberOfRepayments;

  @Schema(description = "Period frequency type")
  private TentativeRpsPeriodFrequencyTypeDTO periodFrequencyType;

  @Schema(description = "Repay every")
  private Integer repayEvery;

  @Schema(description = "Interest rate")
  private Double interestRate;

  @Schema(description = "Calculated EMI amount")
  private Double calculatedEmiAmount;

  @Schema(description = "Principal grace")
  private Integer principalGrace;

  @Schema(description = "Interest payment grace")
  private Integer interestPaymentGrace;

  @Schema(description = "Interest free grace")
  private Integer interestFreeGrace;

  @Schema(description = "Broken period interest")
  private Double brokenPeriodInterest;

  @Schema(description = "EMI amount")
  private Double emi;

  @Schema(description = "Calculated repayments starting from date [year, month, day]")
  private List<Integer> calculatedRepaymentsStartingFromDate;
}
