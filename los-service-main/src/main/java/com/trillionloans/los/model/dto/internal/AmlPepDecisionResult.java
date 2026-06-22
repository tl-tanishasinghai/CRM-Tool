package com.trillionloans.los.model.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AmlPepDecisionResult {
  public enum DecisionOutcome {
    PASS,
    MANUAL_REVIEW,
    REJECT
  }

  private DecisionOutcome decision;
  private String reasonDescription;
}
