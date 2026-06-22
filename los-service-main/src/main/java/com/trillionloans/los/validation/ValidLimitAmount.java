package com.trillionloans.los.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LimitAmountValidator.class)
public @interface ValidLimitAmount {
  String message() default "[merchant] Invalid limitAmount value";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

class LimitAmountValidator implements ConstraintValidator<ValidLimitAmount, String> {

  @Override
  public void initialize(ValidLimitAmount constraintAnnotation) {}

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    try {
      // split the value into integral and decimal parts
      String[] parts = value.split("\\.");
      // check if the integral part has more than 12 digits
      if (parts[0].length() > 12) {
        return false;
      }
      // check if the decimal part has more than 2 digits
      return parts.length == 1 || parts[1].length() <= 2;
      // if the value passed all checks, it's valid
    } catch (NumberFormatException e) {
      // if the string cannot be parsed into a BigDecimal, it's invalid
      return false;
    }
  }
}
