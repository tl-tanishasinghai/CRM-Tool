package com.trillionloans.los.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class DoubleValidator implements ConstraintValidator<ValidDouble, Double> {
  private int decimalDigits;

  @Override
  public void initialize(ValidDouble constraintAnnotation) {
    decimalDigits = constraintAnnotation.decimalDigits();
  }

  @Override
  public boolean isValid(Double value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    BigDecimal bigDecimalValue = new BigDecimal(value.toString());
    String[] parts = bigDecimalValue.toPlainString().split("\\.");
    return parts.length < 2 || parts[1].length() <= decimalDigits;
  }
}
