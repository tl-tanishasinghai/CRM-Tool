package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(content = JsonInclude.Include.NON_NULL)
public class AdvancedReportRequestDTO {
  private Map<String, Object> displayParams;
  private Map<String, Object> reportParams;
  private String reportName;
}
