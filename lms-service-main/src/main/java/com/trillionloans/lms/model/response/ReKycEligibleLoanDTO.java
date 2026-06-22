package com.trillionloans.lms.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReKycEligibleLoanDTO {
  @JsonProperty("client_id")
  private String clientId;

  @JsonProperty("lan_id")
  private String lanId;

  @JsonProperty("display_name")
  private String clientName;

  @JsonProperty("mobile_no")
  private String mobileNo;

  @JsonProperty("product_id")
  private Integer productId;

  @JsonProperty("disbursedon_date")
  @JsonFormat(pattern = "MMM d, yyyy", locale = "en")
  private LocalDate disbursalDate;

  @JsonProperty("kyc_due_date")
  @JsonFormat(pattern = "MMM d, yyyy", locale = "en")
  private LocalDate kycDueDate;

  @JsonProperty("risk_category")
  private String riskCategory;

  @JsonProperty("dpd_days")
  private Integer dpdDays;

  @JsonProperty("days_diff")
  private Long daysDiff;
}
