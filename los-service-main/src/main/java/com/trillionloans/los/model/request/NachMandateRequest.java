package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Nach mandate request")
public class NachMandateRequest {
  private String status;

  @Pattern(
      regexp = "^[a-zA-Z0-9@]+$",
      message = "[nachMandate] only alphabets, numbers, and @ is allowed in umrn")
  private String umrn;

  private String bankAccountType;

  private String bankAccountHolderName;

  @Pattern(
      regexp = "^[a-zA-Z .&()'-]+$",
      message = "[nachMandate] bankName contains invalid characters")
  private String bankName;

  @Pattern(
      regexp = "^[a-zA-Z0-9, .()\\[\\]\\-/&_]+$",
      message = "[nachMandate] branchName contains invalid characters")
  private String branchName;

  @Pattern(
      regexp = "^[0-9a-zA-Z ]+$",
      message = "[nachMandate] accountNumber contains invalid characters")
  private String bankAccountNumber;

  @Pattern(regexp = "^\\d{9}$", message = "[nachMandate] micr number must be exactly 9 digits")
  private String micr;

  @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "[nachMandate] invalid ifsc code")
  private String ifsc;

  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[nachMandate] Invalid mandateRegistrationRequestedDate. Use dd-mm-yyyy format")
  private String mandateRegistrationRequestedDate;

  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[nachMandate] Invalid periodStartDate. Use dd-mm-yyyy format")
  private String periodStartDate;

  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[nachMandate] Invalid periodEndDate. Use dd-mm-yyyy format")
  private String periodEndDate;

  private Boolean periodUntilCancelled;
  private String debitTypeEnum;
  private String debitFrequencyEnum;

  @Positive(message = "[nachMandate] amount must be greater than zero")
  private Double amount;

  private String externalReferenceNumber;

  @NotEmpty(message = "[nachMandate] mode is required")
  private String mode;
}
