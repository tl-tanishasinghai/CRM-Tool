package com.trillionloans.lms.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {MaxDigitsValidator.class})
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MaxDigitsValidatorTest {
  @Autowired private MaxDigitsValidator maxDigitsValidator;

  /** Method under test: {@link MaxDigitsValidator#initialize(MaxDigits)} */
  @Test
  void testInitialize() {
    MaxDigits constraintAnnotation = mock(MaxDigits.class);
    when(constraintAnnotation.value()).thenReturn(42);

    maxDigitsValidator.initialize(constraintAnnotation);

    verify(constraintAnnotation).value();
  }

  /** Method under test: {@link MaxDigitsValidator#isValid(Double, ConstraintValidatorContext)} */
  @Test
  void testIsValid() {
    ClockProvider clockProvider = mock(ClockProvider.class);

    assertFalse(
        maxDigitsValidator.isValid(
            10.0d,
            new ConstraintValidatorContextImpl(
                clockProvider,
                PathImpl.createRootPath(),
                null,
                "Constraint Validator Payload",
                ExpressionLanguageFeatureLevel.DEFAULT,
                ExpressionLanguageFeatureLevel.DEFAULT)));
  }

  /** Method under test: {@link MaxDigitsValidator#isValid(Double, ConstraintValidatorContext)} */
  @Test
  void testIsValid2() {
    ClockProvider clockProvider = mock(ClockProvider.class);

    assertTrue(
        maxDigitsValidator.isValid(
            null,
            new ConstraintValidatorContextImpl(
                clockProvider,
                PathImpl.createRootPath(),
                null,
                "Constraint Validator Payload",
                ExpressionLanguageFeatureLevel.DEFAULT,
                ExpressionLanguageFeatureLevel.DEFAULT)));
  }
}
