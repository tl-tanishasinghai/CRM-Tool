package com.trillionloans.crm.integration.los;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LosLoanApplicationDto(
    BigInteger loanApplicationId,
    String status,
    String productCode,
    String officeName,
    String submittedOnDate) {}
