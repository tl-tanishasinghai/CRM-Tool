package com.trillionloans.lms.model;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record DailyRateSlab(int dpdFrom, int dpdTo, BigDecimal dailyRate, String description) {}
