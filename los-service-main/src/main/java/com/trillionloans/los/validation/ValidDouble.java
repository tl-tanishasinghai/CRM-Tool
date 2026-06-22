package com.trillionloans.los.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DoubleValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDouble {
  String message() default "Invalid double value";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int decimalDigits() default 2; // new parameter
}
