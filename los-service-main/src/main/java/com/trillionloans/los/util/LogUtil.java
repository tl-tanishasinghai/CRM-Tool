package com.trillionloans.los.util;

import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

  public static void logWithContext(
      reactor.util.context.ContextView contextView, Runnable loggingAction) {
    String traceId = contextView.getOrDefault(TRACE_ID, null);

    try {
      if (traceId != null) {
        MDC.put(TRACE_ID, traceId);
      }
      loggingAction.run();
    } finally {
      if (traceId != null) {
        MDC.remove(TRACE_ID);
      }
    }
  }
}
