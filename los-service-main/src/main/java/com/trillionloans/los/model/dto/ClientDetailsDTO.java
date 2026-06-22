package com.trillionloans.los.model.dto;

import com.trillionloans.los.constant.Gender;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ClientDetailsDTO {
  @NotEmpty(message = "[clientDetails] firstName is required")
  private String firstName;

  private String middleName;

  @NotEmpty(message = "[clientDetails] lastName is required")
  @Pattern(
      regexp = "^[a-zA-Z]+$",
      message = "[clientDetails] the parameter lastName is invalid format")
  private String lastName;

  private Gender gender;

  @NotEmpty(message = "[clientDetails] dateOfBirth is required")
  private String dateOfBirth;

  @Pattern(
      regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      message = "[clientDetails] provide correct email format")
  private String email;

  @NotEmpty(message = "[clientDetails] mobileNo is required")
  private String mobileNo;

  private String alternateMobileNo;
  private String education;

  @Size(max = 100, message = "[loanApplication] externalId should be under 100 characters")
  private String externalId;
}
