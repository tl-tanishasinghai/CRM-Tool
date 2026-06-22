package com.trillionloans.customer_portal.model.dto;

import com.trillionloans.customer_portal.constant.ConsentKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class ClientConsentDTO {

  @NotBlank(message = "[ClientConsent] ipAddress is required")
  @Pattern(
          regexp = "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$",
          message = "[ClientConsent] ipAddress is invalid"
  )
  private String ipAddress;

  private ConsentKey consentKey;

  @NotNull(message = "[ClientConsent] consentStatus is required")
  private Boolean consentStatus;
}
