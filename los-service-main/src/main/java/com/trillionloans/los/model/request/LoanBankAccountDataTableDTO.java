package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) representing the bank account information related to a loan
 * application. This class is used to transfer bank account data in a structured format.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanBankAccountDataTableDTO {

  /** The bank account number of the borrower. */
  @JsonProperty("bank_account_number")
  @SerializedName("bank_account_number")
  private String bankAccountNumber;

  /** The unique identifier for the bank, typically assigned by the regulatory authority. */
  @JsonProperty("bank_id")
  @SerializedName("bank_id")
  private String bankId;

  /** The Indian Financial System Code (IFSC) for the bank, used for electronic funds transfer. */
  @JsonProperty("ifsc_code")
  @SerializedName("ifsc_code")
  private String ifscCode;

  /** The name of the account holder for the bank account. */
  @JsonProperty("account_holder_name")
  @SerializedName("account_holder_name")
  private String accountHolderName;

  /** The name of the bank where the account is held. */
  @JsonProperty("bank_name")
  @SerializedName("bank_name")
  private String bankName;

  /** The type of bank account (e.g., Savings, Current). */
  @JsonProperty("account_type")
  @SerializedName("account_type")
  private String accountType;

  /** The type of beneficiary for the account (e.g., Self, Merchant). */
  @JsonProperty("beneficiary_type")
  @SerializedName("beneficiary_type")
  private String beneficiaryType;

  /** The bank verification status (e.g., Yes, No). */
  @JsonProperty("bank_verified")
  @SerializedName("bank_verified")
  private String bankVerified;
}
