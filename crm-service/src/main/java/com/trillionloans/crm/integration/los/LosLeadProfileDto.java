package com.trillionloans.crm.integration.los;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LosLeadProfileDto(
    BigInteger leadId,
    String name,
    String dateOfBirth,
    Integer age,
    String address,
    String email,
    String mobileNo,
    String ucic,
    String panNumber,
    Object loanAccounts) {}
