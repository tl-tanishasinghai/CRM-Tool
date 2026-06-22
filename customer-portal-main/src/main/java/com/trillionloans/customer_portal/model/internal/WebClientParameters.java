package com.trillionloans.customer_portal.model.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class WebClientParameters {
  private String loggerHeader;
  private String partnerName;
  private Integer retryCount;
  private Boolean requestLogRequired;
  private Boolean responseLogRequired;
  private Boolean retryOn5xxOnly;
}
