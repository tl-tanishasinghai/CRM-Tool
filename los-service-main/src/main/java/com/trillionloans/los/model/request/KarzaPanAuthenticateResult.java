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
public class KarzaPanAuthenticateResult {
  private KarzaInternalStatusEnum internalStatusDesc;
  private KarzaPanAuthenticateResponse vendorResponse;
  private EvaluationResult evaluationResult;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class EvaluationResult {
    private KarzaPanAuthenticateResult.Status status; // APPROVED, REJECTED
    private String rejectionReasons;
  }

  public enum Status {
    APPROVED,
    REJECTED,
    MANUAL_REVIEW
  }

  @Override
  public String toString() {
    return "KarzaPanAuthenticateResult{"
        + "internalStatusDesc="
        + (internalStatusDesc != null ? internalStatusDesc.name() : null)
        + ", vendorResponse="
        + (vendorResponse != null ? vendorResponse.toString() : null)
        + '}';
  }
}
