package com.trillionloans.lms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

class ClientSideExceptionTest {
  /**
   * Method under test: {@link ClientSideException#ClientSideException(String, Object,
   * HttpStatusCode, String)}
   */
  @Test
  void testNewClientSideException() {
    ClientSideException actualClientSideException =
        new ClientSideException(
            "An error occurred", "Client Response", null, "https://example.org/example");

    assertEquals("An error occurred", actualClientSideException.getLocalizedMessage());
    assertEquals("An error occurred", actualClientSideException.getMessage());
    assertEquals("Client Response", actualClientSideException.getClientResponse());
    assertEquals("https://example.org/example", actualClientSideException.getUrl());
    assertNull(actualClientSideException.getCause());
    assertNull(actualClientSideException.getHttpStatusCode());
    assertEquals(0, actualClientSideException.getSuppressed().length);
  }
}
