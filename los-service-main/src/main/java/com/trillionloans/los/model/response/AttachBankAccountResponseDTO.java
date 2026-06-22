package com.trillionloans.los.model.response;

/**
 * A Data Transfer Object (DTO) representing the response for attaching a bank account. This record
 * contains the necessary information returned after a bank account is successfully attached.
 *
 * @param bankId The unique identifier of the bank account that has been attached.
 */
public record AttachBankAccountResponseDTO(String bankId) {}
