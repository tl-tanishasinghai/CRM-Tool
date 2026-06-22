package com.trillionloans.customer_portal.util;

import static com.trillionloans.customer_portal.constant.StringConstants.TRACE_ID;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

@Slf4j
public class LogUtil {
  private LogUtil() {
    throw new IllegalStateException();
  }

  public static String getTraceId(HttpHeaders headers) {
    List<String> requestIdHeaders = headers.get(TRACE_ID);
    return requestIdHeaders == null || requestIdHeaders.isEmpty()
        ? UUID.randomUUID().toString().substring(0, 8)
        : requestIdHeaders.get(0);
  }
}
