package com.trillionloans.los.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class EnumValidatorImpl implements ConstraintValidator<EnumValidator, String> {
  private EnumValidator annotation;

  @Override
  public void initialize(EnumValidator annotation) {
    this.annotation = annotation;
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) return true;

    return Arrays.stream(annotation.enumClass().getEnumConstants())
        .anyMatch(e -> ((Enum<?>) e).name().equalsIgnoreCase(value));
  }
}
