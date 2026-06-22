package com.trillionloans.customer_portal.constant;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Enum representing various path variable keys used for validation in protected routes.
 *
 * <p>Each enum constant defines: - A logical key name used in path variable matching - A regex
 * pattern that specifies valid formats for the corresponding value
 *
 * <p>This enum is used during request filtering to validate dynamic path segments such as loan
 * account numbers and id, lead IDs, and mobile numbers.
 *
 * <p><b>Note:</b> If a new path variable is introduced in any protected API route, it must be added
 * here to ensure proper authorization and validation.
 */
@Getter
public enum ValidationKey {
  LEAD_ID("leadId", "\\d+"),
  LOAN_ACCOUNT_NUMBER("loanAccountNumber", "\\d+"),
  LOAN_APPLICATION_ID("loanApplicationId", "\\d+"),
  MOBILE_NUMBER("mobileNumber", "\\d{10}");

  private final String key;
  private final Pattern pattern;

  ValidationKey(String key, String regex) {
    this.key = key;
    this.pattern = Pattern.compile(regex);
  }

  public boolean isValid(String value) {
    return pattern.matcher(value).matches();
  }

  public static ValidationKey fromKey(String key) {
    return Arrays.stream(values())
        .filter(v -> v.key.equalsIgnoreCase(key))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported key: " + key));
  }

  public static Optional<ValidationKey> fromOptionalKey(String key) {
    return Arrays.stream(values()).filter(v -> v.key.equalsIgnoreCase(key)).findFirst();
  }
}
