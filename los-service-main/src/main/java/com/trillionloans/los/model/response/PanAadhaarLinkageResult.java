package com.trillionloans.los.model.response;

/**
 * Result of PAN-Aadhaar linkage check. When no linkage record exists for the loan, clientId may be
 * null and linked is true (check skipped).
 */
public record PanAadhaarLinkageResult(String clientId, boolean linked) {}
