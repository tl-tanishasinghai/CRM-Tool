package com.trillionloans.los.model.response.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.trillionloans.los.constant.MandateAuthSubType;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO matching Digio API response as shown in the below attached documentation. <br>
 * <a href="https://documentation.digio.in/digicollect/nach/nach_registration/live_banks/">DIGIO
 * DOC</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandateLiveBanksDigioResponse {

  @JsonProperty("id")
  @SerializedName("id")
  private String id;

  @JsonProperty("name")
  @SerializedName("name")
  private String name;

  @JsonProperty("ifsc_prefix")
  @SerializedName("ifsc_prefix")
  private String ifscPrefix;

  @JsonProperty("active")
  @SerializedName("active")
  private Boolean active;

  @JsonProperty("primary_bank")
  @SerializedName("primary_bank")
  private Boolean primaryBank;

  @JsonProperty("routing_code")
  @SerializedName("routing_code")
  private String routingCode;

  @JsonProperty("esign_mandate")
  @SerializedName("esign_mandate")
  private Boolean esignMandate;

  @JsonProperty("api_mandate")
  @SerializedName("api_mandate")
  private Boolean apiMandate;

  @JsonProperty("physical_mandate")
  @SerializedName("physical_mandate")
  private Boolean physicalMandate;

  @JsonProperty("allowed_auth_sub_type")
  @SerializedName("allowed_auth_sub_type")
  private Set<MandateAuthSubType> allowedAuthSubType;

  @JsonProperty("api_mandate_net_banking")
  @SerializedName("api_mandate_net_banking")
  private Boolean apiMandateNetBanking;

  @JsonProperty("api_mandate_debit_card")
  @SerializedName("api_mandate_debit_card")
  private Boolean apiMandateDebitCard;

  @JsonProperty("api_mandate_aadhaar_card")
  @SerializedName("api_mandate_aadhaar_card")
  private Boolean apiMandateAadhaarCard;

  @JsonProperty("bank_short_code")
  @SerializedName("bank_short_code")
  private String bankShortCode;

  @JsonProperty("bank_short_code_debit")
  @SerializedName("bank_short_code_debit")
  private String bankShortCodeDebit;

  @JsonProperty("penny_less")
  @SerializedName("penny_less")
  private Boolean pennyLess;
}
