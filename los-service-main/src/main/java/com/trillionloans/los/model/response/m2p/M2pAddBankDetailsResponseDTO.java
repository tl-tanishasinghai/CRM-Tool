package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A Data Transfer Object (DTO) representing the response for adding bank details in the M2P
 * process. This record contains the necessary information returned after successfully adding bank
 * account details.
 *
 * @param bankAccountDetailsId The unique identifier for the added bank account details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record M2pAddBankDetailsResponseDTO(
    String bankAccountDetailsId, String isBankVerified, String errorMessage) {}
