package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskOperationTableUpdateRequest {
  @JsonProperty("product_code")
  @SerializedName("product_code")
  private String productCode;

  @JsonProperty("risk_segment")
  @SerializedName("risk_segment")
  private String riskSegment;

  private String locale;

  @JsonProperty("created_at")
  @SerializedName("created_at")
  private String createdAt;

  @JsonProperty("updated_at")
  @SerializedName("updated_at")
  private String updatedAt;

  private final String dateFormat = "MMM dd, yyyy HH:mm";
}
