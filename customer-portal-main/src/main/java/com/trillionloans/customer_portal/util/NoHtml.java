package com.trillionloans.customer_portal.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoHtmlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {
  String message() default "HTML or script tags are not allowed";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
