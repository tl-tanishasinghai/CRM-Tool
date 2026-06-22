package com.trillionloans.crm.integration.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LmsLoanDetailsDto(
    String loanAccountNumber,
    BigInteger status,
    double loanAmount,
    BigInteger tenure,
    double chargesPreDisbursal,
    double emiAmount,
    String disbursementDate,
    double netDisbursementAmount,
    @JsonProperty("product_id") BigInteger productId,
    double rateOfInterest,
    BigInteger repaymentPeriodFrequencyEnum,
    String officeName,
    BigInteger loanApplicationId,
    double totalPrincipalOutstanding,
    String productCode) {}
