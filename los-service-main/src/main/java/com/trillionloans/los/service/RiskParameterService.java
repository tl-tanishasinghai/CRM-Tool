package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.NIRA_PRODUCT;
import static com.trillionloans.los.constant.StringConstants.PORTFOLIO_BALANCING;
import static com.trillionloans.los.constant.StringConstants.RISK_PARAMETERS_NOT_FOUND;
import static com.trillionloans.los.constant.StringConstants.SAVEIN_PRODUCT;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG;
import static com.trillionloans.los.util.PortfolioBalancingMetricsUtil.aggregateMetrics;
import static com.trillionloans.los.util.PortfolioBalancingMetricsUtil.calculateMetricValue;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.NexusApi;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.ActiveLoanDTO;
import com.trillionloans.los.model.dto.internal.PortfolioMetricDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.PortfolioDistributionMetric;
import com.trillionloans.los.repository.PortfolioDistributionMetricsRepository;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.RedisCacheService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
@Slf4j
@RequiredArgsConstructor
public class RiskParameterService {
  private final M2PWrapperApi m2PWrapperApi;
  private final NexusApi nexusApi;
  private final PortfolioDistributionMetricsRepository portfolioDistributionMetricsRepository;
  private final RedisCacheService redisCacheService;
  private final ProductConfigMasterService productConfigMasterService;

  public Mono<Void> incrementPortfolioRiskParameters(
      String loanApplicationId, String productCode, Integer disbursedAmount) {

    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);

    return productConfigTuple
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[ERROR][PORTFOLIO_BALANCING] no product configs found for productCode {},"
                          + " skipping loan {}.",
                      productCode,
                      loanApplicationId);
                  return Mono.empty();
                }))
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), PORTFOLIO_BALANCING);

              if (Objects.isNull(flowData)) {
                log.warn(
                    "[PORTFOLIO_BALANCING] No {} flow for productCode={}, loanApplicationId={};"
                        + " skipping portfolio increment.",
                    PORTFOLIO_BALANCING,
                    productCode,
                    loanApplicationId);
                return Mono.empty();
              }

              return getRiskParameters(loanApplicationId, productCode, flowData)
                  .flatMap(
                      riskData ->
                          calculateMetricValue(
                              riskData, BigDecimal.valueOf(disbursedAmount), flowData))
                  .flatMap(
                      metric ->
                          updateMetricInDbAndRedis(metric)
                              .doOnSuccess(
                                  v ->
                                      log.info(
                                          "[PORTFOLIO_BALANCING] database increment committed"
                                              + " successfully for metric: {}.",
                                          metric.getMetricKey()))
                              .doOnError(
                                  e ->
                                      log.error(
                                          "[PORTFOLIO_BALANCING][ERROR] increment failed for"
                                              + " metric: {}, error: {}.",
                                          metric.getMetricKey(),
                                          e.getMessage())));
            })
        .then();
  }

  public Mono<Void> decrementPortfolioRiskParameters(
      String loanApplicationId, String productCode, Integer disbursedAmount) {

    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);

    return productConfigTuple
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[ERROR][PORTFOLIO_BALANCING] no product configs found for productCode {},"
                          + " skipping loan {}.",
                      productCode,
                      loanApplicationId);
                  return Mono.empty();
                }))
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), PORTFOLIO_BALANCING);

              if (Objects.isNull(flowData)) {
                log.warn(
                    "[PORTFOLIO_BALANCING] No {} flow for productCode={}, loanApplicationId={};"
                        + " skipping portfolio decrement.",
                    PORTFOLIO_BALANCING,
                    productCode,
                    loanApplicationId);
                return Mono.empty();
              }

              return getRiskParameters(loanApplicationId, productCode, flowData)
                  .flatMap(
                      riskData ->
                          calculateMetricValue(
                              riskData, BigDecimal.valueOf(disbursedAmount), flowData))
                  .flatMap(
                      metric -> {
                        // negate the calculated values for decrement
                        metric.setMetricValue(
                            metric.getMetricValue().multiply(BigDecimal.valueOf(-1.0)));
                        return updateMetricInDbAndRedis(metric)
                            .doOnSuccess(
                                v ->
                                    log.info(
                                        "[PORTFOLIO_BALANCING] database decrement committed"
                                            + " successfully for metric: {}.",
                                        metric.getMetricKey()))
                            .doOnError(
                                e ->
                                    log.error(
                                        "[PORTFOLIO_BALANCING][ERROR] database decrement failed for"
                                            + " metric: {}, error: {}.",
                                        metric.getMetricKey(),
                                        e.getMessage()));
                      });
            })
        .then();
  }

  public Mono<Object> getRiskParameters(
      String loanApplicationId, String productCode, ProductControl.Flow flowData) {

    boolean isRiskParameterCalculationEnabled =
        Boolean.TRUE.equals(flowData.getIsRiskParameterCalculationEnabled());

    if (!isRiskParameterCalculationEnabled) {
      return Mono.empty();
    }
    return nexusApi
        .getRiskParameters(loanApplicationId, productCode)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error("[ERROR] nexus api returned empty for loan: {}", loanApplicationId);
                  return Mono.error(
                      new BaseException(
                          SOMETHING_WENT_WRONG,
                          RISK_PARAMETERS_NOT_FOUND,
                          HttpStatus.INTERNAL_SERVER_ERROR));
                }));
  }

  public Mono<Void> updateMetricInDbAndRedis(PortfolioMetricDTO metric) {

    if (metric == null || metric.getMetricKey() == null) {
      log.error("[ERROR][PORTFOLIO_BALANCING] null or empty metric, skipping database update.");
      return Mono.empty();
    }

    final String metricKey = metric.getMetricKey();

    return portfolioDistributionMetricsRepository
        .findByMetricKey(metricKey)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[PORTFOLIO_BALANCING] metric key {} not found. Creating new record"
                          + " initialized at 0.0",
                      metricKey);
                  PortfolioDistributionMetric newEntity = new PortfolioDistributionMetric();
                  newEntity.setMetricKey(metricKey);
                  newEntity.setMetricValue(BigDecimal.ZERO);
                  return Mono.just(newEntity);
                }))
        .flatMap(
            existingMetric -> {
              BigDecimal oldValue =
                  (existingMetric.getMetricValue() != null)
                      ? existingMetric.getMetricValue()
                      : BigDecimal.ZERO;
              BigDecimal delta = metric.getMetricValue();
              BigDecimal newValue = oldValue.add(delta);

              existingMetric.setMetricValue(newValue);
              existingMetric.setLastUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

              return portfolioDistributionMetricsRepository.save(existingMetric);
            })
        .flatMap(
            savedMetric -> {
              String key = "portfolio-metrics:" + savedMetric.getMetricKey();
              String value = savedMetric.getMetricValue().toPlainString();

              return redisCacheService.putKey(key, value).thenReturn(savedMetric);
            })
        .then();
  }

  private Map<String, BigDecimal> mergeMaps(
      Map<String, BigDecimal> map1, Map<String, BigDecimal> map2) {
    Map<String, BigDecimal> merged = new HashMap<>(map1);
    map2.forEach((k, v) -> merged.merge(k, v, BigDecimal::add));
    return merged;
  }

  /** fetch all portfolio-distribution metrics and put them in cache */
  public Mono<Map<String, Double>> fetchAllAndWarmCache() {
    return portfolioDistributionMetricsRepository
        .findAll()
        .collectList()
        .flatMap(this::processAndCacheMetrics);
  }

  private Mono<Map<String, Double>> processAndCacheMetrics(
      List<PortfolioDistributionMetric> entities) {
    if (entities.isEmpty()) {
      log.error(
          "[ERROR][PORTFOLIO_BALANCING] database returned no portfolio metrics for redis fallback");
      return Mono.just(Collections.emptyMap());
    }

    Map<String, Double> metricsMap = new HashMap<>();
    Map<String, String> cacheMetricsString = new HashMap<>();

    for (PortfolioDistributionMetric entity : entities) {
      String key = entity.getMetricKey();

      String redisKey = "portfolio-metrics:" + key;
      BigDecimal metricValue = entity.getMetricValue();

      metricsMap.put(key, metricValue.doubleValue());
      cacheMetricsString.put(redisKey, metricValue.toPlainString());
    }

    Mono<Void> cacheWrite =
        Flux.fromIterable(cacheMetricsString.entrySet())
            .flatMap(entry -> redisCacheService.putKey(entry.getKey(), entry.getValue()))
            .then();

    return cacheWrite.thenReturn(metricsMap);
  }

  public Mono<Void> initializePortfolio() {
    return m2PWrapperApi
        .getActiveLoans()
        .buffer(500)
        .flatMap(this::processBatchForInitialization, 1)
        .then();
  }

  private Mono<Void> processBatchForInitialization(List<ActiveLoanDTO> batch) {
    if (batch.isEmpty()) {
      return Mono.empty();
    }
    Map<String, List<ActiveLoanDTO>> partitioned =
        batch.stream().collect(Collectors.groupingBy(ActiveLoanDTO::getProductId));

    List<ActiveLoanDTO> niraActiveLoans =
        partitioned.getOrDefault(NIRA_PRODUCT, Collections.emptyList());
    List<ActiveLoanDTO> saveInActiveLoans =
        partitioned.getOrDefault(SAVEIN_PRODUCT, Collections.emptyList());

    Mono<Map<String, BigDecimal>> niraMetrics =
        fetchBulkRiskParameters(niraActiveLoans, NIRA_PRODUCT)
            .defaultIfEmpty(Collections.emptyMap());
    Mono<Map<String, BigDecimal>> saveInMetrics =
        fetchBulkRiskParameters(saveInActiveLoans, SAVEIN_PRODUCT)
            .defaultIfEmpty(Collections.emptyMap());

    return Mono.zip(niraMetrics, saveInMetrics)
        .map(tuple -> mergeMaps(tuple.getT1(), tuple.getT2()))
        .flatMap(
            metrics -> {
              if (metrics.isEmpty()) {
                log.info(
                    "[PORTFOLIO_INITIALIZATION] no risk parameters returned for any product from"
                        + " thr risk database.");
                return Mono.empty();
              }

              return applyInitialSync(metrics).then();
            })
        .doOnSuccess(
            v ->
                log.info(
                    "[PORTFOLIO_INITIALIZATION] successfully finished portfolio initialization"
                        + " calculation"))
        .doOnError(
            e ->
                log.error(
                    "[ERROR][PORTFOLIO_INITIALIZATION] portfolio initialization calculation failed."
                        + " error: {}",
                    e.getMessage()))
        .then();
  }

  private Mono<Map<String, BigDecimal>> fetchBulkRiskParameters(
      List<ActiveLoanDTO> activeLoans, String productId) {
    return Mono.deferContextual(
        ctx -> {
          if (activeLoans.isEmpty()) {
            log.info(
                "[PORTFOLIO_INITIALIZATION] no active loan found for product_code: {}.", productId);
            return Mono.just(Collections.emptyMap());
          }
          List<String> loanApplicationIds =
              activeLoans.stream().map(ActiveLoanDTO::getLoanApplicationId).toList();
          return nexusApi
              .getRiskParametersBulk(loanApplicationIds, productId)
              .map(riskMap -> aggregateMetrics(activeLoans, riskMap, productId));
        });
  }

  public Mono<Void> applyInitialSync(Map<String, BigDecimal> finalMetrics) {
    return Flux.fromIterable(finalMetrics.entrySet())
        .flatMap(
            entry -> {
              String metricKey = entry.getKey();
              PortfolioDistributionMetric portfolioDistributionMetric =
                  PortfolioDistributionMetric.builder()
                      .metricKey(metricKey)
                      .metricValue(entry.getValue())
                      .lastUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                      .build();

              return portfolioDistributionMetricsRepository
                  .save(portfolioDistributionMetric)
                  .doOnSuccess(
                      m ->
                          log.info(
                              "[PORTFOLIO_INITIALIZATION] metric {} with value {} committed to"
                                  + " database.",
                              m.getMetricKey(),
                              m.getMetricValue()))
                  .doOnError(
                      e ->
                          log.error(
                              "[ERROR][PORTFOLIO_INITIALIZATION] database persistence failed"
                                  + " for metric {}.",
                              portfolioDistributionMetric.getMetricKey(),
                              e))
                  .flatMap(
                      savedMetric -> {
                        return redisCacheService
                            .putKey(
                                "portfolio-metrics:" + savedMetric.getMetricKey(),
                                savedMetric.getMetricValue().toPlainString())
                            .thenReturn(savedMetric);
                      });
            })
        .then();
  }
}
