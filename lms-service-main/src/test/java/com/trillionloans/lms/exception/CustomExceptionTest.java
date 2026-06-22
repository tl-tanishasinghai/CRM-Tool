package com.trillionloans.lms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

class CustomExceptionTest {
  /** Method under test: {@link CustomException#CustomException(String, Object, HttpStatusCode)} */
  @Test
  void testConstructor() {
    CustomException actualCustomException =
        new CustomException("An error occurred", "Client Response", null);

    assertEquals("An error occurred", actualCustomException.getLocalizedMessage());
    assertEquals("An error occurred", actualCustomException.getMessage());
    assertEquals("Client Response", actualCustomException.getClientResponse());
    assertNull(actualCustomException.getCause());
    assertNull(actualCustomException.getHttpStatusCode());
    assertEquals(0, actualCustomException.getSuppressed().length);
  }
}
