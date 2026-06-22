package com.trillionloans.los.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NsdlPanEvaluationResult {
  private final boolean hardReject;
  private final boolean softReject;
  private final List<String> hardRejections;
  private final List<String> softRejections;
}
