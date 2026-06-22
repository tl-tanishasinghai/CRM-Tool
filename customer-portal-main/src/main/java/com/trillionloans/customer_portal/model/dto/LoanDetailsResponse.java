package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanDetailsResponse {

  private String loanAccountNumber;
  private BigInteger status;
  private double loanAmount;
  private BigInteger tenure;
  private double chargesPreDisbursal;
  private double emiAmount;
  private String disbursementDate;
  private double netDisbursementAmount;

  @JsonProperty("product_id")
  private BigInteger productId;

  private double rateOfInterest;
  private BigInteger repaymentPeriodFrequencyEnum;
  private String officeName;
  private double chargesPostDisbursal;
  private BigInteger loanApplicationId;
  private double totalPrincipalOutstanding;
  private String productCode;
}
