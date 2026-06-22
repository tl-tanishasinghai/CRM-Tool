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
public class PartnershipFormDTO {

  private Integer id;

  @Size(
      max = 255,
      message = "[partnershipFormDetails] partnershipType cannot exceed 255 characters")
  @NotBlank(message = "[partnershipFormDetails] partnershipType is required")
  private String partnershipType;

  @Size(max = 255, message = "[partnershipFormDetails] firstName cannot exceed 255 characters")
  @NotBlank(message = "[partnershipFormDetails] firstName is required")
  @Pattern(
      regexp = "^[A-Za-z\\s]+$",
      message = "[partnershipFormDetails] firstName must contain only letters")
  private String firstName;

  @Size(max = 255, message = "[partnershipFormDetails] lastName cannot exceed 255 characters")
  @NotBlank(message = "[partnershipFormDetails] lastName is required")
  @Pattern(
      regexp = "^[A-Za-z\\s]+$",
      message = "[partnershipFormDetails] lastName must contain only letters")
  private String lastName;

  @Pattern(
      regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      message = "[partnershipFormDetails] Provide correct email format")
  @Size(max = 320, message = "[partnershipFormDetails] email cannot exceed 320 characters")
  @NotBlank(message = "[partnershipFormDetails]  email is required")
  private String email;

  @Size(max = 20, message = "[partnershipFormDetails] phone number cannot exceed 20 characters")
  @NotBlank(message = "[partnershipFormDetails]  phone number is required")
  @Pattern(
      regexp = "^[0-9]{10}$",
      message =
          "[partnershipFormDetails]  phone number field must be a valid 10-digit phone number")
  private String phoneNumber;

  @Size(
      max = 255,
      message = "[partnershipFormDetails] organizationName cannot exceed 255 characters")
  @NotBlank(message = "[partnershipFormDetails] organizationName is required")
  @Pattern(
      regexp = "^[a-zA-Z0-9 .-]+$",
      message =
          "[partnershipFormDetails] organizationName must contain only letters, numbers, spaces and"
              + " .  - ")
  private String organizationName;

  @Size(
      max = 255,
      message = "[partnershipFormDetails] designationName cannot exceed 255 characters")
  @NotBlank(message = "[partnershipFormDetails] designationName is required")
  @Pattern(
      regexp = "^[a-zA-Z0-9 .-]+$",
      message =
          "[partnershipFormDetails] designationName must contain only letters, numbers, spaces and"
              + " .  - ")
  private String designation;

  @NotNull(message = "[partnershipFormDetails] consent is required")
  @JsonDeserialize(using = CustomBooleanDeserializer.class)
  private Boolean consent;
}
