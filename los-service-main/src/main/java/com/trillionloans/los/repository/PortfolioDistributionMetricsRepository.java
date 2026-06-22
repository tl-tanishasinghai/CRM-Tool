package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.PortfolioDistributionMetric;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface PortfolioDistributionMetricsRepository
    extends R2dbcRepository<PortfolioDistributionMetric, Long> {
  Mono<PortfolioDistributionMetric> findByMetricKey(String metricKey);
}
