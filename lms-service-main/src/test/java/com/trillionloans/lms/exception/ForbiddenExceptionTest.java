package com.trillionloans.lms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

class ForbiddenExceptionTest {
  /**
   * Method under test: {@link ForbiddenException#ForbiddenException(String, Object,
   * HttpStatusCode)}
   */
  @Test
  void testConstructor() {
    ForbiddenException actualForbiddenException =
        new ForbiddenException("An error occurred", "Client Response", null);

    assertEquals("An error occurred", actualForbiddenException.getLocalizedMessage());
    assertEquals("An error occurred", actualForbiddenException.getMessage());
    assertEquals("Client Response", actualForbiddenException.getClientResponse());
    assertNull(actualForbiddenException.getUrl());
    assertNull(actualForbiddenException.getCause());
    assertNull(actualForbiddenException.getHttpStatusCode());
    assertEquals(0, actualForbiddenException.getSuppressed().length);
  }
}
