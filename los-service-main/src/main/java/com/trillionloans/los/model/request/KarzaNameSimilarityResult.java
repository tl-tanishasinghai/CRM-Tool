package com.trillionloans.los.model.request;

import com.trillionloans.los.mapper.KarzaInternalStatusEnum;
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
public class KarzaNameSimilarityResult {
  private String status;
  private KarzaInternalStatusEnum statusDesc;
  private KarzaNameSimilarityResult.EvaluationResult evaluationResult;
  private KarzaNameSimilarityResponse vendorResponse;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class EvaluationResult {
    private KarzaNameSimilarityResult.Status
        evaluationSatus; // APPROVED, REJECTED, SERVICE_UNAVAILABLE
    private String rejectionReason;
  }

  public enum Status {
    APPROVED,
    REJECTED,
    SERVICE_UNAVAILABLE
  }

  @Override
  public String toString() {
    return "KarzaNameSimilarityResult{"
        + "statusDesc="
        + (statusDesc != null ? statusDesc.name() : null)
        + ", vendorResponse="
        + (vendorResponse != null ? vendorResponse.toString() : null)
        + '}';
  }
}
