package com.trillionloans.los.model.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class WebClientParameters {
  private String loggerHeader;
  private String partnerName;
  private Integer retryCount;
  private Boolean logRequired;
  private Boolean responseLogRequired;
  private Integer timeout;
  private EventContext eventContext;
}
