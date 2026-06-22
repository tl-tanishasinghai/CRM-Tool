package com.trillionloans.lms.config;

import static com.trillionloans.lms.util.FileValidatorUtil.hasValidExtensionAndContentType;
import static com.trillionloans.lms.util.RequestValidation.containsUnsafePatterns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

/**
 * Annotation to validate the input for security checks. Ensures that any input passed to the
 * annotated method or parameter does not contain unsafe content.
 */
@Constraint(validatedBy = SecureInputValidator.class)
@Target({ElementType.METHOD, ElementType.PARAMETER}) // Allow on parameters and methods
@Retention(RetentionPolicy.RUNTIME)
public @interface SecureInput {
  String message() default "[ERROR][SECURITY ALERT] Input validation failed";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

/** Validator class to check if an input object contains any unsafe content. */
@Slf4j
@Component
@AllArgsConstructor
class SecureInputValidator implements ConstraintValidator<SecureInput, Object> {

  private final ObjectMapper objectMapper;

  /**
   * Initializes the validator. This method is empty because no specific initialization is required.
   *
   * @param constraintAnnotation the annotation instance for a given constraint declaration
   */
  @Override
  public void initialize(SecureInput constraintAnnotation) {
    // this is empty, removed code smell here
  }

  /**
   * Validates the input object by checking its content for unsafe patterns.
   *
   * @param obj the object to validate
   * @param context context in which the constraint is evaluated
   * @return {@code true} if the input is valid, {@code false} otherwise
   */
  @Override
  public boolean isValid(Object obj, ConstraintValidatorContext context) {
    if (obj == null) return false; // Consider null objects as valid
    if (obj instanceof FilePart file) {
      return hasValidExtensionAndContentType(file);
    }
    JsonNode jsonNode = objectMapper.valueToTree(obj);
    return !containsUnsafeContent(jsonNode);
  }

  /**
   * Checks if the provided JSON node contains unsafe content.
   *
   * @param jsonNode the JSON node to check
   * @return {@code true} if unsafe content is found, {@code false} otherwise
   */
  private boolean containsUnsafeContent(JsonNode jsonNode) {
    return checkNode(jsonNode);
  }

  /**
   * Recursively checks each node in a JSON structure for unsafe content.
   *
   * @param jsonNode the JSON node to evaluate
   * @return {@code true} if any node contains unsafe content, {@code false} otherwise
   */
  private boolean checkNode(JsonNode jsonNode) {
    if (jsonNode.isObject()) {
      // Iterate through fields of an object
      for (JsonNode value : jsonNode) {
        if (checkNode(value)) {
          return true;
        }
      }
    } else if (jsonNode.isArray()) {
      // Iterate through elements of an array
      for (JsonNode element : jsonNode) {
        if (checkNode(element)) {
          return true;
        }
      }
    } else if (jsonNode.isTextual()) {
      // If the node is text and contains unsafe content
      String input = jsonNode.asText();
      if (containsUnsafePatterns(input)) {
        logUnsafeContent(input);
        return true;
      }
    }
    return false;
  }

  /**
   * Logs a warning message when unsafe content is detected.
   *
   * @param input the unsafe content to log
   */
  private void logUnsafeContent(String input) {
    log.warn("[SECURITY ALERT] Unsafe input detected: {}", input);
  }
}
