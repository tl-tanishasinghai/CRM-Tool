package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DueDetailsResponse {
  private Long id;
  private Double prinicpalDue;
  private Double interestDue;
  private Double feeChargesDue;
  private Double penaltyChargesDue;
  private List<Object> feeChargesDueDetails;
  private List<Object> penaltyChargesDueDetails;
  private Double taxDue;
  private List<Object> taxDueDetails;
  private Double totalDue;
  private Double excessMoneyAvailable;
  private Double rebateAmount;
  private List<PaymentModeOption> paymentModeOptions;
  private List<PaymentTypeOption> paymentTypeOptions;
  private Double principalOutstanding;
  private Integer productId;
  private Integer loanOfficeId;
  private Double principalPaid;
  private Integer currentInstallmentNumber;
  private Integer remainingTenureInDays;
  private Double totalAmountOutstanding;
  private Double totalAmountPaid;
  private Double actualDue;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PaymentModeOption {
    private Integer id;
    private String code;
    private String value;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PaymentTypeOption {
    private Integer id;
    private String name;
    private String description;
    private Boolean isCashPayment;
    private Integer position;
    private Integer externalServiceId;
    private PaymentModeOption paymentMode;
    private ApplicableOn applicableOn;
    private ServiceProvider serviceProvider;
    private ValueType defaultValueDateType;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ApplicableOn {
    private Integer id;
    private String code;
    private String value;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ServiceProvider {
    private Integer id;
    private Boolean isActive;
    private Boolean mandatory;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ValueType {
    private Integer id;
    private String code;
    private String value;
  }
}
