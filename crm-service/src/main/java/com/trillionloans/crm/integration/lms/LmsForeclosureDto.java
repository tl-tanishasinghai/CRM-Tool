package com.trillionloans.crm.integration.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LmsForeclosureDto(
    Double netForeclosureAmount,
    Double amount,
    Double principalPortion,
    Double interestPortion,
    Double feeChargesPortion,
    Double penaltyChargesPortion,
    Double outstandingLoanBalance) {}
