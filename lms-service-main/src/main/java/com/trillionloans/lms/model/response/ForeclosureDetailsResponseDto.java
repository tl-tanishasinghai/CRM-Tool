package com.trillionloans.lms.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForeclosureDetailsResponseDto {
  private Type type;
  private List<Integer> date;
  private List<Integer> valueDate;
  private CurrencyDTO currency;
  private Double amount;
  private Double netForeclosureAmount;
  private Double principalPortion;
  private Double interestPortion;
  private Double feeChargesPortion;
  private Double penaltyChargesPortion;
  private Double outstandingLoanBalance;
  private Boolean manuallyReversed;
  private Boolean isRepaymentAtDisbursement;
  private List<PaymentTypeOption> paymentTypeOptions;
  private Boolean isGlimLoan;
  private Double excessAmountPaymentPortion;
  private Boolean isAllowCompoundingOnEod;
  private Map<String, Object> templateAdditionalDetails;
  private Double rebateAmount;
  private List<Object> foreclosureChargesDetails;
  private List<Object> penaltyChargesDetails;
  private List<Object> feeChargesDetails;
  private List<ChargeDiscountType> chargeDiscountTypes;
  private Double foreClosureChargesPortion;
  private List<Object> loanChargeTaxDetails;
  private LoanForeclosureAmountComponents loanForeclosureAmountComponents;

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Type {
    private Long id;
    private String code;
    private String value;
    private Boolean disbursement;
    private Boolean repaymentAtDisbursement;
    private Boolean repayment;
    private Boolean contra;
    private Boolean waiveInterest;
    private Boolean waiveCharges;
    private Boolean accrual;
    private Boolean accrualReverse;
    private Boolean writeOff;
    private Boolean recoveryRepayment;
    private Boolean initiateTransfer;
    private Boolean approveTransfer;
    private Boolean withdrawTransfer;
    private Boolean rejectTransfer;
    private Boolean chargePayment;
    private Boolean refund;
    private Boolean refundForActiveLoans;
    private Boolean addSubsidy;
    private Boolean revokeSubsidy;
    private Boolean brokenPeriodInterestPosting;
    private Boolean accrualSuspense;
    private Boolean accrualWrittenOff;
    private Boolean accrualSuspenseReverse;
    private Boolean accrualIRDPosting;
    private Boolean prudentialWriteoff;
    private Boolean incomePosting;
    private Boolean upfrontInterestCollection;
    private Boolean isAdditionalInterestPosting;
    private Boolean isLoanReversalAmount;
    private Boolean cashbasedAccrualCharge;
    private Boolean cashbasedAccrualRealization;
    private Boolean cashbasedAccrualWriteoff;
    private Boolean cashbasedAccrualRealizationReverse;
    private Boolean cashbasedAccrualWriteoffReverse;
    private Boolean predisbursementChargePayment;
    private Boolean deductionFromDisbursement;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CurrencyDTO {
    private String code;
    private String name;
    private Integer decimalPlaces;
    private Integer inMultiplesOf;
    private String displaySymbol;
    private String nameCode;
    private String displayLabel;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PaymentTypeOption {
    private Long id;
    private String name;
    private Boolean isCashPayment;
    private Integer position;
    private PaymentMode paymentMode;
    private ApplicableOn applicableOn;
    private ServiceProvider serviceProvider;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentMode {
      private Long id;
      private String code;
      private String value;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicableOn {
      private Long id;
      private String code;
      private String value;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceProvider {
      private Long id;
      private Boolean isActive;
      private Boolean mandatory;
    }
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ChargeDiscountType {
    private Long id;
    private String code;
    private String value;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class LoanForeclosureAmountComponents {
    private Double principal;
    private Double principalOverdue;
    private Double interestOverdue;
    private Double brokenPeriodInterest;
    private Double interestDue;
  }
}
