package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AutoDisbursalRequest {

  @JsonProperty("leadId")
  private String systemExternalId;

  @JsonProperty("partnerId")
  private String partnerId;

  @JsonProperty("paymentProvider")
  private String paymentProvider;

  @JsonProperty("paymentMode")
  private String paymentMode;

  @JsonProperty("amount")
  private Double amount;

  @JsonProperty("sourceAccountId")
  private String sourceAccountId;

  @JsonProperty("initiatedBy")
  private String initiatedBy;

  @JsonProperty("approvedBy")
  private String approvedBy;

  @JsonProperty("beneficiaryDetails")
  private BeneficiaryDetailsDTO beneficiaryDetails;

  @JsonProperty("productCode")
  private String productCode;

  @Getter
  @Setter
  @Builder
  public static class BeneficiaryDetailsDTO {

    @JsonProperty("bankAccountId")
    private String bankAccountId;

    @JsonProperty("accountHolderName")
    private String accountHolderName;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountType")
    private String accountType;

    @JsonProperty("bankIfscCode")
    private String bankIfscCode;
  }
}
