package com.trillionloans.los.model.request;

import com.trillionloans.los.constant.CustomerAccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Mandate registration request body")
public class MandateRegistrationRequest {

  @Size(
      max = 18,
      message =
          "[MandateRegistrationRequest] Value exceeds maximum allowed characters(18) for"
              + " customerAccountNumber field")
  @NotBlank(message = "[MandateRegistrationRequest] accountNumber is required")
  @Pattern(
      regexp = "^[a-zA-Z0-9]*$",
      message =
          "[MandateRegistrationRequest] accountNumber contains invalid characters. Only letters and"
              + " numbers are allowed")
  private String accountNumber;

  @NotNull(message = "[MandateRegistrationRequest] accountType is required")
  private CustomerAccountType accountType;

  @NotBlank(message = "[MandateRegistrationRequest] ifsc is required")
  @Size(
      min = 11,
      max = 11,
      message = "[MandateRegistrationRequest] IFSC code must be exactly 11 characters")
  @Pattern(
      regexp = "^[a-zA-Z]{4}0[a-zA-Z0-9]{6}$",
      message =
          "[MandateRegistrationRequest] Invalid IFSC format. Must be 11 characters: first 4"
              + " letters, followed by 0, followed by 6 alphanumeric characters")
  private String ifsc;

  @Size(
      max = 140,
      message =
          "[MandateRegistrationRequest] Value exceeds maximum allowed characters(140) for"
              + " destinationBankName field")
  @NotBlank(message = "[MANDATE_REGISTRATION_DETAILS] bankName is required")
  @Pattern(
      regexp = "^[a-zA-Z0-9\\s]*$",
      message =
          "[MANDATE_REGISTRATION_DETAILS] bankName contains invalid characters. Only letters,"
              + " numbers and spaces are allowed")
  private String bankName;
}
