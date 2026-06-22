package com.trillionloans.lms.model.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
  private String ipAddress;

  @NotBlank(message = "[ClientConsent] consentKey is required")
  private String consentKey;

  @NotNull(message = "[ClientConsent] consentStatus is required")
  private Boolean consentStatus;
}
