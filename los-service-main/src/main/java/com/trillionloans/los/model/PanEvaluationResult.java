package com.trillionloans.los.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PanEvaluationResult {
  private final boolean valid;
  private final List<String> rejectionReasons;
}
