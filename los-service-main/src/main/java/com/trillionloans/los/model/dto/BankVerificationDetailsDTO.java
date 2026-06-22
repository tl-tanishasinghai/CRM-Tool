package com.trillionloans.los.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Bank Verification request body")
public class BankVerificationDetailsDTO {

  @NotBlank(message = "accountType is required")
  private String accountType;

  @NotBlank(message = "name is required")
  private String name;

  @Size(min = 1, max = 25, message = "account number should not be greater than 25 digits")
  private String accountNumber;

  @NotBlank(message = "ifscCode is required")
  private String ifscCode;

  @NotBlank(message = "bankAccountId is required")
  private String bankAccountId;
}
