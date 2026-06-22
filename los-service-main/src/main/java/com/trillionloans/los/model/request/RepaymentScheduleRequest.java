package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Repayment Schedule request body")
public class RepaymentScheduleRequest {

  @NotNull(message = "[rps] isSubsidyApplicable is required")
  private Boolean isSubsidyApplicable;

  private String clientId;

  private Integer productId;

  private Boolean isTopup;

  private List<Object> disbursementData;

  @NotNull(message = "[rps] fundId is required")
  private Integer fundId;

  @NotNull(message = "[rps] principal is required")
  private Integer principal;

  @NotNull(message = "[rps] numberOfRepayments is required")
  private Integer numberOfRepayments;

  private String externalId;

  @NotNull(message = "[rps] repaymentEvery is required")
  private Integer repaymentEvery;

  @NotNull(message = "[rps] loanTermFrequencyType is required")
  private Integer loanTermFrequencyType;

  @NotNull(message = "[rps] repaymentFrequencyType is required")
  private Integer repaymentFrequencyType;

  @NotNull(message = "[rps] amortizationType is required")
  private Integer amortizationType;

  @NotNull(message = "[rps] interestType is required")
  private Integer interestType;

  @NotNull(message = "[rps] interestCalculationPeriodType is required")
  private Integer interestCalculationPeriodType;

  @NotNull(message = "[rps] allowPartialPeriodInterestCalcualtion is required")
  private Boolean allowPartialPeriodInterestCalcualtion;

  @Valid private List<ChargeDetailsDTO> charges;

  private Integer officeId;
  private String repaymentFrequencyDayOfWeekType;

  @NotNull(message = "[rps] interestRatePerPeriod is required")
  private Double interestRatePerPeriod;

  @NotNull(message = "[rps] loanTermFrequency is required")
  private Integer loanTermFrequency;

  private List<Double> overdueCharges;
  private String locale;
  private String dateFormat;
  private String loanType;

  @NotBlank(message = "[rps] expectedDisbursementDate is required")
  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[rps] Invalid expectedDisbursementDate. Use dd-mm-yyyy format")
  private String expectedDisbursementDate;

  @NotBlank(message = "[rps] submittedOnDate is required")
  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[rps] Invalid submittedOnDate. Use dd-mm-yyyy format")
  private String submittedOnDate;

  @NotBlank(message = "[rps] repaymentsStartingFromDate is required")
  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[rps] Invalid repaymentsStartingFromDate. Use dd-mm-yyyy format")
  private String repaymentsStartingFromDate;

  private List<Integer> repeatsOnDayOfMonth;
  private List<Object> userOverriddenTerms;
  private Integer graceOnPrincipalPayment;

  @NotBlank(message = "[rps] transactionProcessingStrategyId is required")
  private String transactionProcessingStrategyId;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ChargeDetailsDTO {
    @NotNull(message = "[rps] amount is required")
    @Pattern(
        regexp = "^\\d+(\\.\\d{1,3})?$",
        message =
            "[rps] Invalid amount. Positive number with up to 3 digits allowed after decimal.")
    private String amount;

    @NotNull(message = "[rps] chargeId is required")
    private Integer chargeId;

    @NotNull(message = "[rps] canAddChargeToPrincipalForComputation is required")
    private Boolean canAddChargeToPrincipalForComputation;

    @NotNull(message = "[rps] canLendCharge is required")
    private Boolean canLendCharge;
  }
}
