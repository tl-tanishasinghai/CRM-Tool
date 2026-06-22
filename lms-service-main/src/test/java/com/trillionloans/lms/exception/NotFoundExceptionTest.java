package com.trillionloans.lms.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {NotFoundException.class, String.class})
@ExtendWith(SpringExtension.class)
class NotFoundExceptionTest {
  @Autowired private NotFoundException notFoundException;

  /** Method under test: {@link NotFoundException#NotFoundException(String, String)} */
  @Test
  void testConstructor() {
    NotFoundException actualNotFoundException =
        new NotFoundException("An error occurred", "https://example.org/example");

    assertEquals(
        "404 NOT_FOUND \"An error occurred\"", actualNotFoundException.getLocalizedMessage());
    ProblemDetail body = actualNotFoundException.getBody();
    assertEquals("An error occurred", body.getDetail());
    assertEquals("An error occurred", actualNotFoundException.getReason());
    assertEquals("https://example.org/example", actualNotFoundException.getUrl());
    assertEquals(
        "problemDetail.com.trillionloans.lms.exception.NotFoundException",
        actualNotFoundException.getDetailMessageCode());
    assertNull(actualNotFoundException.getDetailMessageArguments());
    assertEquals(404, body.getStatus());
  }
}
