package com.trillionloans.lms.model.dto.internal;

import io.r2dbc.postgresql.codec.Json;

public record PartnerConfigRow(
    String partnerId, String partnerCode, String productCode, String status, Json product_json) {}
