package com.trillionloans.los.model.request.m2p;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_KEY_CB;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Request body for disbursement callback")
public class M2pDisbursementCallBackRequest {
  private Integer loanApplicationId;

  @JsonProperty("AdditionalUTR")
  @SerializedName("AdditionalUTR")
  private String additionalUtr;

  private String disbursementDate;
  private Integer netDisbursement;
  private Integer lanID;
  private Integer approvedAmount;
  private String receiptNumber;
  private String status;
  private String repaymentStartingDate;

  @JsonProperty("insuranceDetails")
  @SerializedName("insuranceDetails")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private InsuranceDetails insuranceDetails;

  @JsonProperty(PRODUCT_KEY_CB)
  @SerializedName(PRODUCT_KEY_CB)
  private String productCode;

  public String getStatus() {
    return StringUtils.isNotEmpty(status) ? status : "";
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class InsuranceDetails {

    @JsonProperty("insurancePolicyNumber")
    @SerializedName("insurancePolicyNumber")
    private String insurancePolicyNumber;

    @JsonProperty("insuranceStatus")
    @SerializedName("insuranceStatus")
    private String insuranceStatus;

    @JsonProperty("insuranceDocURL")
    @SerializedName("insuranceDocURL")
    private String insuranceDocURL;

    @JsonProperty("insuranceDocId")
    @SerializedName("insuranceDocId")
    private Integer insuranceDocId;
  }
}
