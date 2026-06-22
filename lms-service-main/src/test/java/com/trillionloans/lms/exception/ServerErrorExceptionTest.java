package com.trillionloans.lms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

class ServerErrorExceptionTest {
  /**
   * Method under test: {@link ServerErrorException#ServerErrorException(String, Object,
   * HttpStatusCode, String)}
   */
  @Test
  void testConstructor() {
    ServerErrorException actualServerErrorException =
        new ServerErrorException(
            "An error occurred", "Client Response", null, "https://example.org/example");

    assertEquals("An error occurred", actualServerErrorException.getLocalizedMessage());
    assertEquals("An error occurred", actualServerErrorException.getMessage());
    assertEquals("Client Response", actualServerErrorException.getClientResponse());
    assertEquals("https://example.org/example", actualServerErrorException.getUrl());
    assertNull(actualServerErrorException.getCause());
    assertNull(actualServerErrorException.getHttpStatusCode());
    assertEquals(0, actualServerErrorException.getSuppressed().length);
  }
}
