package com.trillionloans.los.util;

import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;

public class WebClientUtil {
  public WebClientParameters getWebClientParameters(
      String partnerCode,
      String loggerHeader,
      Integer retryCount,
      Boolean logRequired,
      Boolean responseLogRequired,
      EventContext eventContext) {
    return WebClientParameters.builder()
        .partnerName(partnerCode)
        .loggerHeader(loggerHeader)
        .retryCount(retryCount == null ? 0 : retryCount)
        .logRequired(logRequired != null && logRequired)
        .responseLogRequired(responseLogRequired != null && responseLogRequired)
        .eventContext(eventContext)
        .build();
  }

  public WebClientParameters getWebClientParameters(
      String partnerCode,
      String loggerHeader,
      Integer retryCount,
      Boolean logRequired,
      Boolean responseLogRequired,
      Integer timeout,
      EventContext eventContext) {
    return WebClientParameters.builder()
        .partnerName(partnerCode)
        .loggerHeader(loggerHeader)
        .retryCount(retryCount == null ? 0 : retryCount)
        .logRequired(logRequired != null && logRequired)
        .responseLogRequired(responseLogRequired != null && responseLogRequired)
        .timeout(timeout)
        .eventContext(eventContext)
        .build();
  }
}
