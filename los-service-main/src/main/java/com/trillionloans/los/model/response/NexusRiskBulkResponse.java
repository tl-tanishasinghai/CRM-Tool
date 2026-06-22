package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NexusRiskBulkResponse {
  @JsonIgnore private Map<String, Object> riskAttributes = new HashMap<>();

  @JsonAnySetter
  public void add(String key, Object value) {
    this.riskAttributes.put(key, value);
  }
}
