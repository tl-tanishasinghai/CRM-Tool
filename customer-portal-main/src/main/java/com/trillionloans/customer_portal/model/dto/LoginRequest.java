package com.trillionloans.customer_portal.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
  @NotBlank(message = "Mobile number is required")
  private String mobileNumber;

  @NotBlank(message = "Date of Birth is required")
  private String dateOfBirth;
}
