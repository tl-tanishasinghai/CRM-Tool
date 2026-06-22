package com.trillionloans.los.validation;

import com.trillionloans.los.model.request.LoanApplication;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestValidation {
  private RequestValidation() {
    throw new IllegalStateException("request validation utility class");
  }

  public static void validateM2pTopUpLoanApplicationRequest(LoanApplication loanApplication) {
    if (loanApplication.getLoanIdToClose() == null
        || loanApplication.getLoanIdToClose().isEmpty()) {
      throw new IllegalArgumentException("[loanApplication] LoanIdToClose is required");
    }
  }

  // Patterns to identify potentially unsafe content in JSON fields and paths
  private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
  private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script.*?>.*?</script.*?>");
  private static final Pattern JS_EVENT_PATTERN =
      Pattern.compile("(?i)onerror|onload|onclick|onmouseover");
  private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[<>]");

  public static boolean containsUnsafePatterns(String input) {
    if (HTML_TAG_PATTERN.matcher(input).find()) {
      log.warn("HTML tag pattern detected in input: {}", input);
      return true;
    }
    if (SCRIPT_PATTERN.matcher(input).find()) {
      log.warn("Script pattern detected in input: {}", input);
      return true;
    }
    if (JS_EVENT_PATTERN.matcher(input).find()) {
      log.warn("JavaScript event pattern detected in input: {}", input);
      return true;
    }
    if (SPECIAL_CHAR_PATTERN.matcher(input).find()) {
      log.warn("Special character pattern detected in input: {}", input);
      return true;
    }
    return false;
  }
}
