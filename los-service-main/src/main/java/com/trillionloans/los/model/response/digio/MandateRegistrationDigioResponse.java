package com.trillionloans.los.model.response.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.trillionloans.los.constant.MandateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MandateRegistrationDigioResponse {

  private String id;

  @JsonProperty("mandate_id")
  @SerializedName("mandate_id")
  private String mandateId;

  private String state;

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

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class BankDetails {
    @JsonProperty("shared_with_bank")
    @SerializedName("shared_with_bank")
    private String sharedWithBank;

    @JsonProperty("bank_name")
    @SerializedName("bank_name")
    private String bankName;

    private String state;
  }

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SubUser {
    private String id;

    @JsonProperty("identifier_value")
    @SerializedName("identifier_value")
    private String identifierValue;

    private String identifier;

    @JsonProperty("email_id")
    @SerializedName("email_id")
    private String emailId;

    private String mobile;
  }

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ServiceProviderDetails {
    @JsonProperty("service_provider_name")
    @SerializedName("service_provider_name")
    private String serviceProviderName;

    @JsonProperty("service_provider_utility_code")
    @SerializedName("service_provider_utility_code")
    private String serviceProviderUtilityCode;
  }

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
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

    private String id;

    @JsonProperty("valid_till")
    @SerializedName("valid_till")
    private String validTill;
  }
}
