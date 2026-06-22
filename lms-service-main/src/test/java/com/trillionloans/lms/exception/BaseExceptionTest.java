package com.trillionloans.lms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

class BaseExceptionTest {
  /** Method under test: {@link BaseException#BaseException(String, Object, HttpStatusCode)} */
  @Test
  void testNewBaseException() {
    BaseException actualBaseException =
        new BaseException("An error occurred", "Client Response", null);
    assertEquals("An error occurred", actualBaseException.getLocalizedMessage());
    assertEquals("An error occurred", actualBaseException.getMessage());
    assertEquals("Client Response", actualBaseException.getClientResponse());
    assertNull(actualBaseException.getUrl());
    assertNull(actualBaseException.getCause());
    assertNull(actualBaseException.getHttpStatusCode());
    assertEquals(0, actualBaseException.getSuppressed().length);
  }
}
