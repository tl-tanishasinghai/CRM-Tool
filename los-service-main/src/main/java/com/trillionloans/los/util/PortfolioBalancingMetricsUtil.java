package com.trillionloans.los.util;

import static com.trillionloans.los.constant.StringConstants.HIGH;
import static com.trillionloans.los.constant.StringConstants.LARGE;
import static com.trillionloans.los.constant.StringConstants.LOW;
import static com.trillionloans.los.constant.StringConstants.MEDIUM;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_A_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_B_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_C_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_D_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_E_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_F_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_G_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_COHORT_H_AMOUNT;
import static com.trillionloans.los.constant.StringConstants.NIRA_PRODUCT;
import static com.trillionloans.los.constant.StringConstants.NIRA_UNKNOWN_METRIC;
import static com.trillionloans.los.constant.StringConstants.SAVEIN_PRODUCT;
import static com.trillionloans.los.constant.StringConstants.SAVE_IN_NON_NTC_COUNT;
import static com.trillionloans.los.constant.StringConstants.SAVE_IN_NTC_COUNT;
import static com.trillionloans.los.constant.StringConstants.SAVE_IN_UNKNOWN_METRIC;
import static com.trillionloans.los.constant.StringConstants.SMALL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.ActiveLoanDTO;
import com.trillionloans.los.model.dto.RiskParametersDTO;
import com.trillionloans.los.model.dto.internal.PortfolioMetricDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.response.NexusRiskBulkResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

@Slf4j
public class PortfolioBalancingMetricsUtil {
  private PortfolioBalancingMetricsUtil() {}

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static Mono<PortfolioMetricDTO> calculateMetricValue(
      Object riskData, BigDecimal amount, ProductControl.Flow flowData) {

    if (Objects.isNull(flowData)) {
      return Mono.error(
          new BaseException("CONFIG_ERROR", "CONFIG_ERROR", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    String metricValueCalculationFunction = flowData.getMetricValueCalculationFunction();
    try {
      Method method =
          PortfolioBalancingMetricsUtil.class.getDeclaredMethod(
              metricValueCalculationFunction, Object.class, BigDecimal.class);

      PortfolioMetricDTO result = (PortfolioMetricDTO) method.invoke(null, riskData, amount);

      return Mono.just(result);

    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      return Mono.error(e);
    }
  }

  public static PortfolioMetricDTO calculateSaveInMetricValue(Object riskData, BigDecimal amount) {
    RiskParametersDTO risk = objectMapper.convertValue(riskData, RiskParametersDTO.class);

    String ntcFlag = Optional.ofNullable(risk.getNtcFlag()).orElse(SAVE_IN_UNKNOWN_METRIC);
    String metricKey;
    if (ntcFlag.equals("ntc")) {
      metricKey = SAVE_IN_NTC_COUNT;
    } else if (ntcFlag.equals("non_ntc")) {
      metricKey = SAVE_IN_NON_NTC_COUNT;
    } else {
      metricKey = SAVE_IN_UNKNOWN_METRIC;
    }
    BigDecimal metricValue = BigDecimal.valueOf(1.0);

    return new PortfolioMetricDTO(metricKey, metricValue);
  }

  public static PortfolioMetricDTO calculateNiraMetricValue(Object riskData, BigDecimal amount) {
    RiskParametersDTO risk = (objectMapper.convertValue(riskData, RiskParametersDTO.class));
    String metricKey = mapToNiraCohort(amount, risk);
    return new PortfolioMetricDTO(metricKey, amount);
  }

  public static String mapToNiraCohort(BigDecimal amount, RiskParametersDTO riskParameters) {
    String ticketSize;
    if (amount.compareTo(BigDecimal.valueOf(25000)) >= 0
        && amount.compareTo(BigDecimal.valueOf(50000)) < 0) {
      ticketSize = SMALL;
    } else if (amount.compareTo(BigDecimal.valueOf(50000)) >= 0) {
      ticketSize = LARGE;
    } else {
      return NIRA_UNKNOWN_METRIC;
    }

    String scoreBin;
    if (riskParameters.getNiraScore() > 0.8) {
      scoreBin = HIGH;
    } else if (riskParameters.getNiraScore() > 0.7) {
      scoreBin = MEDIUM;
    } else if (riskParameters.getNiraScore() > 0.6) {
      scoreBin = LOW;
    } else {
      return NIRA_UNKNOWN_METRIC;
    }

    if (ticketSize.equals(SMALL)) {
      if ("FRESH".equalsIgnoreCase(riskParameters.getCustomerType())) {
        if (scoreBin.equals(MEDIUM)) return NIRA_COHORT_A_AMOUNT;
        else if (scoreBin.equals(HIGH)) return NIRA_COHORT_B_AMOUNT;
      } else {
        if (scoreBin.equals(LOW)) return NIRA_COHORT_C_AMOUNT;
        else if (scoreBin.equals(MEDIUM)) return NIRA_COHORT_D_AMOUNT;
        else if (scoreBin.equals(HIGH)) return NIRA_COHORT_E_AMOUNT;
      }
    } else if (ticketSize.equals(LARGE)) {
      if ("REPEAT".equalsIgnoreCase(riskParameters.getCustomerType())) {
        if (scoreBin.equals(LOW)) return NIRA_COHORT_F_AMOUNT;
        else if (scoreBin.equals(MEDIUM)) return NIRA_COHORT_G_AMOUNT;
        else if (scoreBin.equals(HIGH)) return NIRA_COHORT_H_AMOUNT;
      }
    }

    return NIRA_UNKNOWN_METRIC;
  }

  public static Map<String, BigDecimal> aggregateMetrics(
      List<ActiveLoanDTO> batch, NexusRiskBulkResponse riskMap, String productId) {

    Map<String, BigDecimal> totals = new HashMap<>();

    for (ActiveLoanDTO loan : batch) {
      String loanApplicationId = loan.getLoanApplicationId();
      BigDecimal loanAmount = loan.getDisbursedAmount();

      if (riskMap.getRiskAttributes() == null
          || riskMap.getRiskAttributes().isEmpty()
          || !riskMap.getRiskAttributes().containsKey(loanApplicationId)) {
        log.error(
            "[ERROR][PORTFOLIO_INITIALIZATION] no risk parameters found for loanApplicationId: {}.",
            loanApplicationId);
        continue;
      }

      if (NIRA_PRODUCT.equals(productId)) {
        Object rawRiskObject = riskMap.getRiskAttributes().get(loanApplicationId);
        if (rawRiskObject != null) {
          RiskParametersDTO riskParameters =
              objectMapper.convertValue(rawRiskObject, RiskParametersDTO.class);

          String cohortKey = mapToNiraCohort(loanAmount, riskParameters);

          if (!NIRA_UNKNOWN_METRIC.equals(cohortKey)) {
            totals.merge(cohortKey, loanAmount, BigDecimal::add);
          }
        }
      } else if (SAVEIN_PRODUCT.equals(productId)) {
        Object rawRiskObject = riskMap.getRiskAttributes().get(loanApplicationId);

        if (rawRiskObject != null) {
          RiskParametersDTO risk =
              objectMapper.convertValue(rawRiskObject, RiskParametersDTO.class);

          String ntcKey;
          if (risk.getNtcFlag().equals("ntc")) {
            ntcKey = SAVE_IN_NTC_COUNT;
          } else if (risk.getNtcFlag().equals("non_ntc")) {
            ntcKey = SAVE_IN_NON_NTC_COUNT;
          } else {
            ntcKey = SAVE_IN_UNKNOWN_METRIC;
          }

          totals.merge(ntcKey, BigDecimal.valueOf(1.0), BigDecimal::add);
        }
      } else {
        log.info(
            "[PORTFOLIO_INITIALIZATION] skipping loan {}, productId {} due to unsupported product"
                + " id or missing risk data.",
            loan.getLoanApplicationId(),
            productId);
      }
    }
    return totals;
  }
}
