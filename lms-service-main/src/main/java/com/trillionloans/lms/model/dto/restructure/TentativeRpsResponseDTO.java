package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the tentative restructured RPS response from M2P API.
 *
 * @author Pawan Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Tentative restructured repayment schedule response")
public class TentativeRpsResponseDTO {

  @Schema(description = "Currency details")
  private TentativeRpsCurrencyDTO currency;

  @Schema(description = "Loan term in days")
  private Integer loanTermInDays;

  @Schema(description = "Total principal disbursed")
  private Double totalPrincipalDisbursed;

  @Schema(description = "Total principal expected")
  private Double totalPrincipalExpected;

  @Schema(description = "Total principal paid")
  private Double totalPrincipalPaid;

  @Schema(description = "Total interest charged")
  private Double totalInterestCharged;

  @Schema(description = "Total fee charges charged")
  private Double totalFeeChargesCharged;

  @Schema(description = "Total penalty charges charged")
  private Double totalPenaltyChargesCharged;

  @Schema(description = "Total repayment expected")
  private Double totalRepaymentExpected;

  @Schema(description = "Total outstanding")
  private Double totalOutstanding;

  @Schema(description = "Loan terms data")
  private TentativeRpsLoanTermsDataDTO loanTermsData;

  @Schema(description = "Repayment periods")
  private List<TentativeRpsPeriodDTO> periods;
}
