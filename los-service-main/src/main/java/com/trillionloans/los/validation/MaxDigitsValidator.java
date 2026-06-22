package com.trillionloans.los.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxDigitsValidator implements ConstraintValidator<MaxDigits, Number> {
  private int maxDigits;

  @Override
  public void initialize(MaxDigits constraintAnnotation) {
    maxDigits = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(Number value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // null values are considered valid
    }
    long longValue = value.longValue(); // Convert to long to count digits before decimal
    int digitCount = String.valueOf(Math.abs(longValue)).length();
    return digitCount <= maxDigits;
  }
}
