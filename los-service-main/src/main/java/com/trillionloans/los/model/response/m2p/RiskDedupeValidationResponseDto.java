package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskDedupeValidationResponseDto {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // Backend returns these as JSON strings, not objects
  private String riskCategorisation;
  private String npaLoans;
  private String activeLoans;
  private String activeLimitLine;

  // Helper methods to parse the JSON strings
  public Object getParsedRiskCategorisation() {
    if (riskCategorisation == null || riskCategorisation.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(riskCategorisation, Object.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse riskCategorisation: {}", riskCategorisation, e);
      return null;
    }
  }

  public List<NpaLoan> getParsedNpaLoans() {
    if (npaLoans == null || npaLoans.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(npaLoans, new TypeReference<List<NpaLoan>>() {});
    } catch (JsonProcessingException e) {
      log.error("Failed to parse npaLoans: {}", npaLoans, e);
      return Collections.emptyList();
    }
  }

  public List<ActiveLoan> getParsedActiveLoans() {
    if (activeLoans == null || activeLoans.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(activeLoans, new TypeReference<List<ActiveLoan>>() {});
    } catch (JsonProcessingException e) {
      log.error("Failed to parse activeLoans: {}", activeLoans, e);
      return Collections.emptyList();
    }
  }

  public List<ActiveLimitLine> getParsedActiveLimitLine() {
    if (activeLimitLine == null || activeLimitLine.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(activeLimitLine, new TypeReference<List<ActiveLimitLine>>() {});
    } catch (JsonProcessingException e) {
      log.error("Failed to parse activeLimitLine: {}", activeLimitLine, e);
      return Collections.emptyList();
    }
  }

  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class NpaLoan {
    private String loanId;
  }

  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ActiveLoan {
    private String clientId;
    private String loanId;
    private String losProductKey;
    private int maxDpd;
  }

  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ActiveLimitLine {
    private String limitId;
    private String lineId;
    private String losProductKey;
    private int maxDpd;
  }
}
