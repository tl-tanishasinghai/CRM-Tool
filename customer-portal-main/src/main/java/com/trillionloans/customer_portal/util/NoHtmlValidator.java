package com.trillionloans.customer_portal.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {
  private static final String HTML_PATTERN = ".*<[^>]+>.*";

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) return true;
    return !value.matches(HTML_PATTERN);
  }
}
