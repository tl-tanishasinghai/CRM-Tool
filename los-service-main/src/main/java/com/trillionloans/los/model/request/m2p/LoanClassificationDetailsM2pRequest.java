package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trillionloans.los.constant.DocumentEvaluationStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat datatable row for M2P loan classification (one POST body per evaluation/document line). Must
 * match partner JSON shape: no nested {@code classification_details} array.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanClassificationDetailsM2pRequest {

  private static final String DEFAULT_LOCALE = "en";
  private static final String DEFAULT_DATE_FORMAT_PATTERN = "MMM dd, yyyy HH:mm";
  private static final DateTimeFormatter STAMP_FORMATTER =
      DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT_PATTERN, Locale.ENGLISH);

  @JsonProperty("lead_id")
  private Long leadId;

  @JsonProperty("application_business_name")
  private String applicationBusinessName;

  @JsonProperty("application_business_address")
  private String applicationBusinessAddress;

  @JsonProperty("document_name")
  private String documentName;

  @JsonProperty("document_business_name")
  private String documentBusinessName;

  @JsonProperty("document_business_address")
  private String documentBusinessAddress;

  @JsonProperty("document_number")
  private String documentNumber;

  @JsonProperty("name_match_score")
  private String nameMatchScore;

  @JsonProperty("address_match_score")
  private String addressMatchScore;

  @JsonProperty("is_uploaded")
  private String isUploaded;

  @JsonProperty("is_eligible")
  private String isEligible;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("updated_at")
  private String updatedAt;

  @JsonProperty("locale")
  private String locale;

  /** Partner expects camelCase {@code dateFormat} in JSON. */
  @JsonProperty("dateFormat")
  private String dateFormat;

  /**
   * Builds one M2P payload from a reporting line. Returns {@code null} when {@code lead_id} cannot
   * be parsed as a number (M2P expects numeric {@code lead_id}).
   */
  public static LoanClassificationDetailsM2pRequest fromClassificationLine(
      String loanApplicationId, LoanClassificationDetailsM2pLine line) {
    Long leadId = parseLeadId(loanApplicationId);
    if (leadId == null) {
      return null;
    }
    String stamp = LocalDateTime.now().format(STAMP_FORMATTER);
    return LoanClassificationDetailsM2pRequest.builder()
        .leadId(leadId)
        .applicationBusinessName(emptyIfNull(line.getApplicationBusinessName()))
        .applicationBusinessAddress(emptyIfNull(line.getApplicationBusinessAddress()))
        .documentName(emptyIfNull(line.getDocumentName()))
        .documentBusinessName(emptyIfNull(line.getDocumentBusinessName()))
        .documentBusinessAddress(emptyIfNull(line.getDocumentBusinessAddress()))
        .documentNumber(emptyIfNull(line.getDocumentNumber()))
        .nameMatchScore(bigDecimalToString(line.getNameMatchScore()))
        .addressMatchScore(bigDecimalToString(line.getAddressMatchScore()))
        .isUploaded(booleanToString(line.getIsUploaded()))
        .isEligible(evaluationStatusToM2pIsEligible(line.getIsEligible()))
        .createdAt(stamp)
        .updatedAt(stamp)
        .locale(DEFAULT_LOCALE)
        .dateFormat(DEFAULT_DATE_FORMAT_PATTERN)
        .build();
  }

  private static Long parseLeadId(String loanApplicationId) {
    if (loanApplicationId == null || loanApplicationId.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(loanApplicationId.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String emptyIfNull(String s) {
    return s == null ? "" : s;
  }

  private static String bigDecimalToString(java.math.BigDecimal v) {
    return v == null ? "" : v.toPlainString();
  }

  private static String booleanToString(Boolean v) {
    return v == null ? "" : Boolean.toString(v);
  }

  /**
   * {@code business_loan_document_evaluation.evaluation_status} → M2P {@code is_eligible} string
   * booleans.
   */
  private static String evaluationStatusToM2pIsEligible(String evaluationStatus) {
    if (evaluationStatus == null || evaluationStatus.isBlank()) {
      return "";
    }
    String s = evaluationStatus.trim();
    if (DocumentEvaluationStatus.QUALIFIED.name().equals(s)) {
      return "true";
    }
    if (DocumentEvaluationStatus.NOT_QUALIFIED.name().equals(s)) {
      return "false";
    }
    return "";
  }
}
