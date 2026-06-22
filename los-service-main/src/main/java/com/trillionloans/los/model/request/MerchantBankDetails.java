package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** DTO class for the merchant bank account details that are fetched from LSP. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Merchant/anchor bank account details request body")
public class MerchantBankDetails {

  /** The type of the bank account (e.g., savings, current). */
  @Min(value = 0, message = "[merchantBank] accountTypeId must be a positive integer")
  @Max(
      value = 9999,
      message = "[merchantBank] Value exceeds maximum allowed digits(4) for accountTypeId")
  private Integer accountTypeId;

  /** The name of the account holder or merchant. */
  @Size(max = 150, message = "[merchantBank] name should be under 150 characters")
  private String name;

  /** The bank account number. */
  @Size(max = 50, message = "[merchantBank] accountNumber should be under 50 characters")
  private String accountNumber;

  /** The IFSC (Indian Financial System Code) for electronic funds transfer. */
  private String ifscCode;

  /** The MICR (Magnetic Ink Character Recognition) code used for processing cheques. */
  @Size(max = 20, message = "[merchantBank] micrCode should be under 20 characters")
  private String micrCode;

  /** The mobile number associated with the bank account. */
  @Size(max = 30, message = "[merchantBank] mobileNumber should be under 30 characters")
  private String mobileNumber;

  /** The email address associated with the bank account. */
  @Size(max = 50, message = "[merchantBank] email should be under 50 characters")
  private String email;

  /**
   * Indicates whether penny drop verification is required for the account. True if penny drop is to
   * be done, otherwise false.
   */
  private Boolean doPennyDrop;
}
