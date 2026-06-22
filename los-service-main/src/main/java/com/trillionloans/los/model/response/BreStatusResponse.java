package com.trillionloans.los.model.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BreStatusResponse {
  private boolean active;
  private String status;
  private String stage;
  private String breResult;
  private String reasons;
  private List<Map<String, Object>> data;
}
