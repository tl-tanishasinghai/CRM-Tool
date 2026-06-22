package com.trillionloans.lms.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RequestValidationTest {

  @ParameterizedTest
  @MethodSource("provideTestCases")
  void testContainsUnsafePatterns(String input, boolean expectedResult, String description) {
    boolean result = RequestValidation.containsUnsafePatterns(input);
    assertEquals(expectedResult, result, description);
  }

  private static Stream<Arguments> provideTestCases() {
    return Stream.of(
        Arguments.of("<div>Test</div>", true, "Expected to detect HTML tag pattern."),
        Arguments.of("<script>alert('test');</script>", true, "Expected to detect script pattern."),
        Arguments.of(
            "<img src='test.jpg' onerror='alert(1)' />",
            true,
            "Expected to detect JavaScript event pattern."),
        Arguments.of("Test <input> & data", true, "Expected to detect special characters."),
        Arguments.of(
            "This is a safe input string.", false, "Did not expect to detect any unsafe pattern."));
  }
}
