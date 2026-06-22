package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DobCheckContext {
  private String applicationDOB;
  private String applicationPanDOB;
  private String aadhaarXmlParsedDOB;
}
