package com.trillionloans.lms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = MaxDigitsValidator.class)
public @interface MaxDigits {
  String message() default "Value exceeds maximum allowed digits";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int value();
}
