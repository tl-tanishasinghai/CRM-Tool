package com.trillionloans.los.model.request.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.trillionloans.los.constant.CustomerAccountType;
import com.trillionloans.los.constant.MandateAuthMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MandateRegistrationDigioRequest {
  @JsonProperty("auth_mode")
  @SerializedName("auth_mode")
  private MandateAuthMode authMode;

  @NotBlank(message = "[MandateRegistrationDigioRequest] corporateConfigId is required")
  @JsonProperty("corporate_config_id")
  @SerializedName("corporate_config_id")
  private String corporateConfigId;

  @JsonProperty("customer_identifier")
  @SerializedName("customer_identifier")
  private String customerIdentifier;

  @NotBlank(message = "mandateType is required")
  @JsonProperty("mandate_type")
  @SerializedName("mandate_type")
  private String mandateType;

  @JsonProperty("notify_customer")
  @SerializedName("notify_customer")
  private Boolean notifyCustomer;

  @Valid
  @NotNull(message = "[MandateRegistrationDigioRequest] mandateData is required")
  @JsonProperty("mandate_data")
  @SerializedName("mandate_data")
  private MandateData mandateData;

  @JsonProperty("generate_access_token")
  @SerializedName("generate_access_token")
  private Boolean generateAccessToken;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MandateData {

    @NotBlank(message = "[MandateRegistrationDigioRequest] customerAccountNumber is required")
    @JsonProperty("customer_account_number")
    @SerializedName("customer_account_number")
    private String customerAccountNumber;

    @NotNull(message = "[MandateRegistrationDigioRequest] customerAccountType is required")
    @JsonProperty("customer_account_type")
    @SerializedName("customer_account_type")
    private CustomerAccountType customerAccountType;

    @NotBlank(message = "[MandateRegistrationDigioRequest] customerName is required")
    @JsonProperty("customer_name")
    @SerializedName("customer_name")
    private String customerName;

    @Size(
        max = 20,
        message =
            "[MandateRegistrationDigioRequest] customerRefNumber must not exceed 20 characters")
    @Pattern(
        regexp = "^[0-9A-Za-z]*$",
        message = "customerRefNumber must match the pattern ^[0-9A-Za-z]*$")
    @JsonProperty("customer_ref_number")
    @SerializedName("customer_ref_number")
    private String customerRefNumber;

    @JsonProperty("destination_bank_id")
    @SerializedName("destination_bank_id")
    private String destinationBankId;

    @JsonProperty("destination_bank_name")
    @SerializedName("destination_bank_name")
    private String destinationBankName;

    @NotBlank(message = "[MandateRegistrationDigioRequest] firstCollectionDate is required")
    @JsonProperty("first_collection_date")
    @SerializedName("first_collection_date")
    private String firstCollectionDate;

    @JsonProperty("final_collection_date")
    @SerializedName("final_collection_date")
    private String finalCollectionDate;

    @JsonProperty("frequency")
    @SerializedName("frequency")
    private String frequency;

    @JsonProperty("instrument_type")
    @SerializedName("instrument_type")
    private String instrumentType;

    @NotNull(message = "[MandateRegistrationDigioRequest] isRecurring is required")
    @JsonProperty("is_recurring")
    @SerializedName("is_recurring")
    private Boolean isRecurring;

    @NotBlank(message = "[MandateRegistrationDigioRequest] managementCategory is required")
    @JsonProperty("management_category")
    @SerializedName("management_category")
    private String managementCategory;

    @NotNull(message = "[MandateRegistrationDigioRequest] maximumAmount is required")
    @DecimalMin(
        value = "1.0",
        message = "[MandateRegistrationDigioRequest] maximumAmount must be at least 1")
    @DecimalMax(
        value = "10000000.0",
        message = "[MandateRegistrationDigioRequest] maximumAmount must not exceed 10000000")
    @JsonProperty("maximum_amount")
    @SerializedName("maximum_amount")
    private Double maximumAmount;

    @JsonProperty("customer_mobile")
    @SerializedName("customer_mobile")
    private String customerMobile;

    @JsonProperty("customer_email")
    @SerializedName("customer_email")
    private String customerEmail;
  }
}
