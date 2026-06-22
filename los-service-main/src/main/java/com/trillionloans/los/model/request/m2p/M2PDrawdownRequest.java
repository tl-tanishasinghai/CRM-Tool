package com.trillionloans.los.model.request.m2p;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class M2PDrawdownRequest {
  private String externalId;

  private BigDecimal amount;
  private String notes;
  private Long transactionTime;

  private EmiConversionDetails emiConversionDetails;
  private List<Charge> charges;
  //  private OrderDetails orderDetails;
  private PaymentDetails paymentDetails;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EmiConversionDetails {
    private Integer numberOfRepayments;
    private Double interestRatePerPeriod;
    private String productShortName;
    private Short loanTermFrequencyType;
    private Short repaymentFrequencyType;
    private Integer repaymentEvery;
    private Integer loanTermFrequency;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Charge {
    private String chargeIdentifier;
    private BigDecimal amount;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderDetails {
    private String merchantId;
    private MerchantDetails merchantDetails;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MerchantDetails {
    private String merchantName;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentDetails {
    private String paymentType;
    private Integer paymentTypeId;
  }
}
