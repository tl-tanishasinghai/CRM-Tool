package com.trillionloans.los.model.dto.internal;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PortfolioMetricDTO {
  private String metricKey;
  private BigDecimal metricValue;
}
