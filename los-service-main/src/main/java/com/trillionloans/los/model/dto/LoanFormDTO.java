package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.trillionloans.los.validation.CustomBooleanDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class LoanFormDTO {

  private Integer id;

  @Size(max = 255, message = "[loanFormDetails] firstName cannot exceed 255 characters")
  @NotBlank(message = "[loanFormDetails] firstName is required")
  @Pattern(
      regexp = "^[A-Za-z\\s]+$",
      message = "[loanFormDetails] firstName must contain only letters")
  private String firstName;

  @Size(max = 255, message = "[loanFormDetails] lastName cannot exceed 255 characters")
  @NotBlank(message = "[loanFormDetails] lastName is required")
  @Pattern(
      regexp = "^[A-Za-z\\s]+$",
      message = "[loanFormDetails] lastName must contain only letters")
  private String lastName;

  @Pattern(
      regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      message = "[loanFormDetails] Provide correct email format")
  @Size(max = 320, message = "[loanFormDetails] email cannot exceed 320 characters")
  @NotBlank(message = "[loanFormDetails] email is required")
  private String email;

  @Size(max = 20, message = "[loanFormDetails] phone number cannot exceed 20 characters")
  @NotBlank(message = "[loanFormDetails]  phone number is required")
  @Pattern(
      regexp = "^[0-9]{10}$",
      message = "[loanFormDetails]  phone number field must be a valid 10-digit phone number")
  private String phoneNumber;

  @Size(max = 255, message = "[loanFormDetails]  loanType cannot exceed 255 characters")
  @NotBlank(message = "[loanFormDetails]  loanType is required")
  @Pattern(
      regexp = "^[A-Za-z0-9\\s-]+$",
      message = "[loanFormDetails] loanType must contain only letters")
  private String loanType;

  @NotNull(message = "[loanFormDetails] consent is required")
  @JsonDeserialize(using = CustomBooleanDeserializer.class)
  private Boolean consent;
}
