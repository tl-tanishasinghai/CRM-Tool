package com.trillionloans.los.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class LogUtilTest {
  /** Method under test: {@link LogUtil#getTraceId(HttpHeaders)} */
  @Test
  void testGetTraceId() {
    HashMap<String, List<String>> map = new HashMap<>();
    map.computeIfPresent("foo", mock(BiFunction.class));

    HttpHeaders headers = new HttpHeaders();
    headers.add("traceId", "42");
    headers.putAll(map);

    assertEquals("42", LogUtil.getTraceId(headers));
  }
}
