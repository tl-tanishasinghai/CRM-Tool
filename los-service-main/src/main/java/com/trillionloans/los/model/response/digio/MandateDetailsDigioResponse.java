package com.trillionloans.los.model.response.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.trillionloans.los.constant.MandateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO matching Digio API response as shown in the below attached documentation. <br>
 * <a
 * href="https://documentation.digio.in/digicollect/nach/nach_registration/get_mandate_details">DIGIO
 * DOC</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandateDetailsDigioResponse {

  @JsonProperty("id")
  @SerializedName("id")
  private String id;

  @JsonProperty("mandate_id")
  @SerializedName("mandate_id")
  private String mandateId;

  @JsonProperty("state")
  @SerializedName("state")
  private String state;

  @JsonProperty("type")
  @SerializedName("type")
  private MandateType type;

  @JsonProperty("bank_details")
  @SerializedName("bank_details")
  private BankDetails bankDetails;

  @JsonProperty("sub_user")
  @SerializedName("sub_user")
  private SubUser subUser;

  @JsonProperty("created_at")
  @SerializedName("created_at")
  private String createdAt;

  @JsonProperty("mode")
  @SerializedName("mode")
  private String mode;

  @JsonProperty("service_provider_details")
  @SerializedName("service_provider_details")
  private ServiceProviderDetails serviceProviderDetails;

  @JsonProperty("access_token")
  @SerializedName("access_token")
  private AccessToken accessToken;

  @JsonProperty("authentication_url")
  @SerializedName("authentication_url")
  private String authenticationUrl;

  @JsonProperty("ack_report")
  @SerializedName("ack_report")
  private AckReport ackReport;

  @JsonProperty("res_report")
  @SerializedName("res_report")
  private ResReport resReport;

  @JsonProperty("mandate_details")
  @SerializedName("mandate_details")
  private MandateDetails mandateDetails;

  @JsonProperty("npci_auth_failed_error")
  @SerializedName("npci_auth_failed_error")
  private String npciAuthFailedError;

  @JsonProperty("npci_auth_reject_reason")
  @SerializedName("npci_auth_reject_reason")
  private String npciAuthRejectReason;

  @JsonProperty("auth_sub_mode")
  @SerializedName("auth_sub_mode")
  private String authSubMode;

  @JsonProperty("umrn")
  @SerializedName("umrn")
  private String umrn;

  // ----- nested DTOs -----

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class BankDetails {
    @JsonProperty("shared_with_bank")
    @SerializedName("shared_with_bank")
    private String sharedWithBank;

    @JsonProperty("shared_at")
    @SerializedName("shared_at")
    private String sharedAt;

    @JsonProperty("bank_name")
    @SerializedName("bank_name")
    private String bankName;

    @JsonProperty("state")
    @SerializedName("state")
    private String state;

    @JsonProperty("authenticated_at")
    @SerializedName("authenticated_at")
    private String authenticatedAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SubUser {
    @JsonProperty("id")
    @SerializedName("id")
    private String id;

    @JsonProperty("identifier_value")
    @SerializedName("identifier_value")
    private String identifierValue;

    @JsonProperty("identifier")
    @SerializedName("identifier")
    private String identifier;

    @JsonProperty("email_id")
    @SerializedName("email_id")
    private String emailId;

    @JsonProperty("mobile")
    @SerializedName("mobile")
    private String mobile;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ServiceProviderDetails {
    @JsonProperty("service_provider_name")
    @SerializedName("service_provider_name")
    private String serviceProviderName;

    @JsonProperty("service_provider_utility_code")
    @SerializedName("service_provider_utility_code")
    private String serviceProviderUtilityCode;

    @JsonProperty("is_enabled_for_aadhaar_auth")
    @SerializedName("is_enabled_for_aadhaar_auth")
    private Boolean isEnabledForAadhaarAuth;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccessToken {
    @JsonProperty("created_at")
    @SerializedName("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    @SerializedName("updated_at")
    private String updatedAt;

    @JsonProperty("entity_id")
    @SerializedName("entity_id")
    private String entityId;

    @JsonProperty("id")
    @SerializedName("id")
    private String id;

    @JsonProperty("valid_till")
    @SerializedName("valid_till")
    private String validTill;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AckReport {

    @JsonProperty("id")
    @SerializedName("id")
    private String id;

    @JsonProperty("umrn")
    @SerializedName("umrn")
    private String umrn;

    @JsonProperty("message_id")
    @SerializedName("message_id")
    private String messageId;

    @JsonProperty("original_message_id")
    @SerializedName("original_message_id")
    private String originalMessageId;

    @JsonProperty("enach_id")
    @SerializedName("enach_id")
    private String enachId;

    @JsonProperty("accepted")
    @SerializedName("accepted")
    private Boolean accepted;

    @JsonProperty("generated_at")
    @SerializedName("generated_at")
    private String generatedAt;

    @JsonProperty("file_name")
    @SerializedName("file_name")
    private String fileName;

    @JsonProperty("reject_code")
    @SerializedName("reject_code")
    private String rejectCode;

    @JsonProperty("reject_reason")
    @SerializedName("reject_reason")
    private String rejectReason;

    @JsonProperty("created_at")
    @SerializedName("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    @SerializedName("updated_at")
    private String updatedAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ResReport {
    @JsonProperty("id")
    @SerializedName("id")
    private String id;

    @JsonProperty("umrn")
    @SerializedName("umrn")
    private String umrn;

    @JsonProperty("message_id")
    @SerializedName("message_id")
    private String messageId;

    @JsonProperty("original_message_id")
    @SerializedName("original_message_id")
    private String originalMessageId;

    @JsonProperty("enach_id")
    @SerializedName("enach_id")
    private String enachId;

    @JsonProperty("accepted")
    @SerializedName("accepted")
    private Boolean accepted;

    @JsonProperty("generated_at")
    @SerializedName("generated_at")
    private Boolean generatedAt;

    @JsonProperty("file_name")
    @SerializedName("file_name")
    private String fileName;

    @JsonProperty("reject_code")
    @SerializedName("reject_code")
    private String rejectCode;

    @JsonProperty("reject_reason")
    @SerializedName("reject_reason")
    private String rejectReason;

    @JsonProperty("dest_bank_name")
    @SerializedName("dest_bank_name")
    private String destBankName;

    @JsonProperty("dest_bank_id")
    @SerializedName("dest_bank_id")
    private String destBankId;

    @JsonProperty("created_at")
    @SerializedName("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    @SerializedName("updated_at")
    private String updatedAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MandateDetails {
    @JsonProperty("file_name")
    @SerializedName("file_name")
    private String fileName;

    @JsonProperty("customer_identifier")
    @SerializedName("customer_identifier")
    private String customerIdentifier;

    @JsonProperty("customer_name")
    @SerializedName("customer_name")
    private String customerName;

    @JsonProperty("customer_ref_number")
    @SerializedName("customer_ref_number")
    private String customerRefNumber;

    @JsonProperty("scheme_ref_number")
    @SerializedName("scheme_ref_number")
    private String schemeRefNumber;

    @JsonProperty("customer_mobile")
    @SerializedName("customer_mobile")
    private String customerMobile;

    @JsonProperty("customer_email")
    @SerializedName("customer_email")
    private String customerEmail;

    @JsonProperty("auth_type")
    @SerializedName("auth_type")
    private String authType;

    @JsonProperty("authentication_time")
    @SerializedName("authentication_time")
    private String authenticationTime;

    @JsonProperty("is_recurring")
    @SerializedName("is_recurring")
    private Boolean isrecurring;

    @JsonProperty("frequency")
    @SerializedName("frequency")
    private String frequency;

    @JsonProperty("first_collection_date")
    @SerializedName("first_collection_date")
    private String firstCollectionDate;

    @JsonProperty("final_collection_date")
    @SerializedName("final_collection_date")
    private String finalCollectionDate;

    @JsonProperty("collection_amount")
    @SerializedName("collection_amount")
    private Double collectionAmount;

    @JsonProperty("maximum_amount")
    @SerializedName("maximum_amount")
    private Double maximumAmount;

    @JsonProperty("customer_account_number")
    @SerializedName("customer_account_number")
    private String customerAccountNumber;

    @JsonProperty("customer_account_type")
    @SerializedName("customer_account_type")
    private String customerAccountType;

    @JsonProperty("destination_bank_id")
    @SerializedName("destination_bank_id")
    private String destinationBankId;

    @JsonProperty("destination_bank_name")
    @SerializedName("destination_bank_name")
    private String destinationBankName;

    @JsonProperty("sponsor_bank_name")
    @SerializedName("sponsor_bank_name")
    private String sponsorBankName;

    @JsonProperty("npci_txn_id")
    @SerializedName("npci_txn_id")
    private String npciTxnId;
  }
}
