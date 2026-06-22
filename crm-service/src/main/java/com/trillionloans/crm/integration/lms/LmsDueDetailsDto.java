package com.trillionloans.crm.integration.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LmsDueDetailsDto(
    Double prinicpalDue,
    Double interestDue,
    Double feeChargesDue,
    Double penaltyChargesDue,
    Double totalDue,
    Double principalOutstanding,
    Double totalAmountOutstanding,
    Double totalAmountPaid,
    Integer currentInstallmentNumber) {}
