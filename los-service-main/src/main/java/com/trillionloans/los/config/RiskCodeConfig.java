package com.trillionloans.los.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Slf4j
@Component
@ConfigurationProperties(prefix = "m2p.risk.codes")
public class RiskCodeConfig {
  private Integer highId;
  private Integer mediumId;
  private Integer lowId;
  private Integer uhrId;

  /**
   * Checks if client has Unacceptable High Risk (UHR) from risk categorisation table response
   *
   * @param response Raw Object from m2PWrapperApi.getRiskCategorisationTable()
   * @return true if risk_cd_risk == uhrId, false otherwise
   */
  public boolean isUnacceptableHighRisk(Object response) {
    if (!(response instanceof List<?> list) || list.isEmpty()) {
      return false;
    }

    Object first = list.get(0);
    if (!(first instanceof Map<?, ?> map)) {
      return false;
    }

    Object riskCodeObj = map.get("risk_cd_risk");
    if (riskCodeObj == null) {
      return false;
    }

    Integer uhrId = getUhrId();
    if (uhrId == null) {
      return false;
    }

    return uhrId.toString().equals(riskCodeObj.toString());
  }

  public boolean isLowRisk(Object response) {
    if (!(response instanceof List<?> list) || list.isEmpty()) {
      return false;
    }

    Object first = list.get(0);
    if (!(first instanceof Map<?, ?> map)) {
      return false;
    }

    Object riskCodeObj = map.get("risk_cd_risk");
    if (riskCodeObj == null || getLowId() == null) {
      return false;
    }

    return getLowId().toString().equals(riskCodeObj.toString());
  }

  public String getRiskCategory(Object response) {
    if (!(response instanceof List<?> list) || list.isEmpty()) {
      return "UNKNOWN";
    }

    Object first = list.get(0);
    if (!(first instanceof Map<?, ?> map)) {
      return "UNKNOWN";
    }

    Object riskCodeObj = map.get("risk_cd_risk");
    if (riskCodeObj == null) {
      return "UNKNOWN";
    }

    String riskCode = riskCodeObj.toString();

    if (getLowId() != null && getLowId().toString().equals(riskCode)) {
      return "Low";
    }
    if (getHighId() != null && getHighId().toString().equals(riskCode)) {
      return "High";
    }
    if (getMediumId() != null && getMediumId().toString().equals(riskCode)) {
      return "Medium";
    }
    if (getUhrId() != null && getUhrId().toString().equals(riskCode)) {
      return "UHR";
    }

    return "UNKNOWN";
  }
}
