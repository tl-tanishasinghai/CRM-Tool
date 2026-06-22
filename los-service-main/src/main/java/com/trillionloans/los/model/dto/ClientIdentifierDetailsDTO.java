package com.trillionloans.los.model.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ClientIdentifierDetailsDTO {
  private String documentType;
  private String documentKey;

  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[clientIdentifierDetails] Invalid issueDate. Use dd-mm-yyyy format")
  private String issueDate;

  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[clientIdentifierDetails] Invalid expiryDate. Use dd-mm-yyyy format")
  private String expiryDate;
}
