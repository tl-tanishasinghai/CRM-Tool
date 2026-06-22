package com.trillionloans.customer_portal.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TotalFileSizeValidator.class)
public @interface ValidTotalFileSize {
  String message() default "Total file size should not exceed 10MB";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int maxSizeInMB() default 10;
}
