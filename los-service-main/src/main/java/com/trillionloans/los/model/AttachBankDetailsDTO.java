package com.trillionloans.los.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trillionloans.los.constant.BankAccountType;
import com.trillionloans.los.constant.BeneficaryType;
import com.trillionloans.los.validation.EnumValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * Data Transfer Object (DTO) representing bank details for attachment. This DTO encapsulates
 * information related to a bank account for loan processing.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachBankDetailsDTO {

  /** Account number of the bank account. */
  @NotBlank(message = "[BankDetails] accountNumber cannot be NULL or empty!")
  @Length(
      message = "[BankDetails] accountNumber cannot be less than 8 or greater than 25 letters!",
      min = 8,
      max = 25)
  @Pattern(
      regexp = "^[0-9a-zA-Z ]+$",
      message = "[BankDetails] accountNumber contains invalid characters!")
  private String accountNumber;

  /** IFSC code of the bank associated with the account. */
  @NotBlank(message = "[BankDetails] ifscCode cannot be NULL or empty!")
  @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "[BankDetails] Invalid IFSC code format")
  private String ifscCode;

  /** Name of the account holder as registered with the bank. */
  @NotBlank(message = "[BankDetails] accountHolderName cannot be NULL or empty!")
  private String accountHolderName;

  /** Name of the bank where the account is held. */
  @NotBlank(message = "[BankDetails] bankName cannot be NULL or empty!")
  @Pattern(
      regexp = "^[a-zA-Z .&()'-]+$",
      message = "[BankDetails] Bank name contains invalid characters")
  @Size(
      min = 3,
      max = 100,
      message = "[BankDetails] Bank name must be between 3 and 100 characters")
  private String bankName;

  /** Type of bank account (e.g., Savings, Current). */
  @EnumValidator(
      enumClass = BankAccountType.class,
      message = "[BankDetails] Invalid bankAccountType")
  @NotNull(message = "[BankDetails] accountType cannot be NULL!")
  private String bankAccountType;

  /** Type of beneficiary (e.g., Self, Merchant). */
  @EnumValidator(
      enumClass = BeneficaryType.class,
      message = "[BankDetails] Invalid beneficiaryType")
  @NotNull(message = "[BankDetails] beneficiaryType cannot be NULL!")
  private String beneficiaryType;
}
