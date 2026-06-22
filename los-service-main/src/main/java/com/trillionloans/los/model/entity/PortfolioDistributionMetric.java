package com.trillionloans.los.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("portfolio_distribution_metrics")
public class PortfolioDistributionMetric {
  @Id private Long id;

  @Column("metric_key")
  private String metricKey;

  @Column("metric_value")
  private BigDecimal metricValue;

  @Column("last_updated_at")
  private LocalDateTime lastUpdatedAt;
}
