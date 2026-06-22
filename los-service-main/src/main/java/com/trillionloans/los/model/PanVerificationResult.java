package com.trillionloans.los.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PanVerificationResult {

  private String responseCode;
  private String responseCodeDesc;
  private List<VerificationResult> panVerificationResults;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class VerificationResult {
    private EvaluationResult evaluationResult;
    private Record vendorResponse;
  }

  /** Represents the evaluation outcome of a PAN verification. */
  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class EvaluationResult {
    private Status status; // APPROVED, REJECTED
    private String rejectionMessage;
    private List<String> rejectionReasons;
  }

  public enum Status {
    APPROVED, // ALL PASS
    REJECTED, // Hard REJECT
    SOFT_REJECTED, // Few PASS Few FAILED
    MANUAL_REVIEW // Service Failure
  }

  /** Represents one PAN verification record from OPV API response. */
  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class Record {
    private String pan;
    private String panStatus;
    private String panStatusDesc;
    private String nameMatch;
    private String dobMatch;
    private String fathersNameMatch;
    private String seedingStatus;
    private String seedingStatusDesc;
  }

  public String toMaskedLogString() {
    StringBuilder sb = new StringBuilder();

    Optional.ofNullable(panVerificationResults)
        .ifPresent(
            results ->
                results.stream()
                    .filter(Objects::nonNull)
                    .forEach(
                        vr -> {
                          EvaluationResult eval = vr.getEvaluationResult();
                          Record rec = vr.getVendorResponse();

                          appendField(sb, "status", eval, EvaluationResult::getStatus);
                          appendField(
                              sb,
                              "rejectionReasons",
                              eval,
                              e ->
                                  Optional.ofNullable(e.getRejectionReasons())
                                      .map(Object::toString)
                                      .orElse(StringUtils.EMPTY));
                          appendField(
                              sb,
                              "pan",
                              rec,
                              r ->
                                  Optional.ofNullable(r.getPan())
                                      .map(this::maskPan)
                                      .orElse(StringUtils.EMPTY));
                          appendField(sb, "panStatus", rec, Record::getPanStatus);
                          appendField(sb, "panStatusDesc", rec, Record::getPanStatusDesc);
                          appendField(sb, "nameMatch", rec, Record::getNameMatch);
                          appendField(sb, "dobMatch", rec, Record::getDobMatch);
                          appendField(sb, "fathersNameMatch", rec, Record::getFathersNameMatch);
                          appendField(sb, "seedingStatus", rec, Record::getSeedingStatus);
                        }));

    return sb.toString();
  }

  private <T> void appendField(
      StringBuilder sb, String fieldName, T source, Function<T, Object> extractor) {
    sb.append(fieldName)
        .append("=")
        .append(
            Optional.ofNullable(source)
                .map(extractor)
                .map(Object::toString)
                .orElse(StringUtils.EMPTY))
        .append(", ");
  }

  private String maskPan(String pan) {
    if (Objects.isNull(pan) || pan.length() < 5) {
      return StringUtils.EMPTY;
    }
    int visible = 5;
    return "X".repeat(pan.length() - visible) + pan.substring(pan.length() - visible);
  }
}
