package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawdownData {
  private Map<String, Object> alternateData;

  @NotNull(message = "Drawdown amount is required")
  @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
  private BigDecimal amount;

  private String notes;

  @NotNull(message = "EMI conversion details are required.")
  private EmiConversionDetails emiConversionDetails;

  private BulkDocumentsUploadRequest drawdownAgreement;

  private List<Charge> charges;

  //  private PaymentDetails paymentDetails;

  @NotNull(message = "Product short name is required")
  private String productShortName;

  @NotNull(message = "Repayment type is required")
  private String repaymentType;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class EmiConversionDetails {
    private Integer numberOfRepayments;
    private Integer repaymentEvery;
    private Double interestRatePerPeriod;
    private Short loanTermFrequencyType;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Charge {
    private String chargeIdentifier;
    private BigDecimal amount;
  }
}
