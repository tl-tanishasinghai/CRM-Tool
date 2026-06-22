package com.trillionloans.lms.util;

import com.trillionloans.lms.model.dto.internal.WebClientParameters;

public class WebClientUtil {

  public WebClientParameters getWebClientParameters(
      String partnerCode,
      String loggerHeader,
      Integer retryCount,
      Boolean logRequired,
      Integer timeout) {
    return WebClientParameters.builder()
        .partnerName(partnerCode)
        .loggerHeader(loggerHeader)
        .retryCount(retryCount == null ? 0 : retryCount)
        .logRequired(logRequired != null && logRequired)
        .timeout(timeout)
        .build();
  }
}
