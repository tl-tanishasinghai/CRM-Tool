package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.DIGIO_AML_NAME_MATCH_DEFAULT_VALUE;
import static com.trillionloans.los.constant.StringConstants.DIGIO_PEP_NAME_MATCH_THRESHOLD;
import static com.trillionloans.los.constant.StringConstants.FAIL;
import static com.trillionloans.los.constant.StringConstants.PAN;
import static com.trillionloans.los.constant.StringConstants.PASS;
import static com.trillionloans.los.constant.StringConstants.PEP;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trillionloans.los.model.request.digio.AmlPepCheckRequest;
import com.trillionloans.los.model.response.digio.AmlPepCheckResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AmlPepProcessor {

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AmlPepMatchDetails {
    // Common fields
    private String serviceStatus;
    // PEP fields
    private String pepMatch; // PEP match flag
    // AML fields
    private Double amlFuzzyMatchScore; // AML flag
    private String amlStatus;

    private AmlPepCheckResponse response;
    private AmlPepCheckRequest request;
  }

  public static AmlPepMatchDetails processDetailedResponse(
      AmlPepCheckResponse response, AmlPepCheckRequest request) {

    List<AmlPepCheckResponse.AlertDetail> allAlerts = new ArrayList<>();

    AmlPepMatchDetails result = new AmlPepMatchDetails();

    result.setResponse(response);
    result.setRequest(request);

    if (response.getHigh_alert_details() != null) {
      allAlerts.addAll(response.getHigh_alert_details());
    }
    if (response.getMedium_alert_details() != null) {
      allAlerts.addAll(response.getMedium_alert_details());
    }
    if (response.getLow_alert_details() != null) {
      allAlerts.addAll(response.getLow_alert_details());
    }

    Optional<AmlPepCheckResponse.AlertDetail> pepAlert =
        allAlerts.stream()
            .filter(alert -> PEP.equalsIgnoreCase(alert.getSource_name()))
            .max(Comparator.comparingDouble(AmlPepCheckResponse.AlertDetail::getFuzzy_match_score));

    Optional<AmlPepCheckResponse.AlertDetail> amlAlert =
        allAlerts.stream()
            .filter(alert -> !PAN.equalsIgnoreCase(alert.getSource_name()))
            .filter(alert -> !PEP.equalsIgnoreCase(alert.getSource_name()))
            .max(Comparator.comparingDouble(AmlPepCheckResponse.AlertDetail::getFuzzy_match_score));

    result.setServiceStatus(SUCCESS);

    // -------------------------
    // PEP LOGIC (TOP PRIORITY)
    // -------------------------
    if (pepAlert.isPresent()) {

      double pepScore = pepAlert.get().getFuzzy_match_score();

      if (pepScore >= DIGIO_PEP_NAME_MATCH_THRESHOLD) {
        result.setPepMatch(FAIL);
      } else {
        result.setPepMatch(PASS);
      }

      result.setAmlFuzzyMatchScore(
          amlAlert
              .map(AmlPepCheckResponse.AlertDetail::getFuzzy_match_score)
              .orElse(DIGIO_AML_NAME_MATCH_DEFAULT_VALUE));

      return result;
    }

    // -------------------------
    // AML LOGIC
    // -------------------------
    if (amlAlert.isPresent()) {
      result.setPepMatch(PASS);
      result.setAmlFuzzyMatchScore(amlAlert.get().getFuzzy_match_score());
      return result;
    }

    // -------------------------
    // NO ALERTS
    // -------------------------
    result.setPepMatch(PASS);
    result.setAmlFuzzyMatchScore(DIGIO_AML_NAME_MATCH_DEFAULT_VALUE);

    return result;
  }
}
