package com.trillionloans.los.model.dto.internal;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RuleEvaluationResultDTO {
  private Map<String, Object> results = new HashMap<>();

  public void setResult(String key, Object value) {
    results.put(key, value);
  }

  public Object getResult(String key) {
    return results.get(key);
  }
}
