package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Data Transfer Object (DTO) representing the response containing bank details in the M2P
 * process. This class encapsulates information related to a bank account, including its attributes
 * and status.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class M2pBankDetailsResponseDTO {

  /** The unique identifier for the bank account. */
  @JsonProperty("id")
  @SerializedName("id")
  private Long bankId;

  /** The name of the account holder associated with the bank account. */
  @JsonProperty("name")
  @SerializedName("name")
  private String accountHolderName;

  /** The bank account number. */
  @JsonProperty("accountNumber")
  @SerializedName("accountNumber")
  private String accountNumber;

  /** The type of the bank account, represented by an AccountType object. */
  @JsonProperty("accountType")
  @SerializedName("accountType")
  private AccountType accountType;

  /** The IFSC code associated with the bank account. */
  @JsonProperty("ifscCode")
  @SerializedName("ifscCode")
  private String ifscCode;

  /** The name of the bank where the account is held. */
  @JsonProperty("bankName")
  @SerializedName("bankName")
  private String bankName;

  /** The current status of the bank account, represented by a Status object. */
  @JsonProperty("status")
  @SerializedName("status")
  private Status status;

  /** The city where the bank is located. */
  @JsonProperty("bankCity")
  @SerializedName("bankCity")
  private String bankCity;

  /** The name of the bank branch where the account is held. */
  @JsonProperty("branchName")
  @SerializedName("branchName")
  private String branchName;

  /** Indicates whether the bank account is verified. */
  @JsonProperty("isVerified")
  @SerializedName("isVerified")
  private boolean isVerified;

  /** The unique identifier for the association of the bank account. */
  @JsonProperty("bankAccountAssociationId")
  @SerializedName("bankAccountAssociationId")
  private long bankAccountAssociationId;

  /** Indicates whether this bank account is the primary account. */
  @JsonProperty("isPrimaryAccount")
  @SerializedName("isPrimaryAccount")
  private boolean isPrimaryAccount;

  /** Represents the type of bank account. */
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AccountType {

    /** The unique identifier for the account type. */
    @JsonProperty("id")
    @SerializedName("id")
    private long id;

    /** The code representing the account type. */
    @JsonProperty("code")
    @SerializedName("code")
    private String code;

    /** The value or name associated with the account type. */
    @JsonProperty("value")
    @SerializedName("value")
    private String value;

    /** The system code associated with the account type. */
    @JsonProperty("systemCode")
    @SerializedName("systemCode")
    private String systemCode;
  }

  /** Represents the status of the bank account. */
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Status {

    /** The unique identifier for the status. */
    @JsonProperty("id")
    private long id;

    /** The code representing the status. */
    @JsonProperty("code")
    private String code;

    /** The value or name associated with the status. */
    @JsonProperty("value")
    private String value;
  }
}
