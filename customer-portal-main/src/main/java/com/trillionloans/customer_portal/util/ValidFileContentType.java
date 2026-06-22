package com.trillionloans.customer_portal.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidFileContentTypeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFileContentType {
  String message() default "Invalid file type";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
