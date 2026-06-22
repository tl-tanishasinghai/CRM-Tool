package com.trillionloans.los.model;

import com.trillionloans.los.model.response.DOBWaterfallResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DOBWaterfallResult {
  private DOBWaterfallResponse response;
  private EvaluationResult evaluationResult;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class EvaluationResult {
    private DOBWaterfallResult.Status evaluationSatus; // APPROVED, REJECTED, SERVICE_UNAVAILABLE
    private String rejectionReason;
  }

  public enum Status {
    APPROVED,
    REJECTED,
    SERVICE_UNAVAILABLE
  }
}
