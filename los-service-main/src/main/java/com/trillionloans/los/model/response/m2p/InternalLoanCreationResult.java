package com.trillionloans.los.model.response.m2p;

import lombok.Getter;

public record InternalLoanCreationResult(
    @Getter M2pLoanCreationResponseDTO m2pLoanCreationResponseDTO, boolean isFallback) {}
