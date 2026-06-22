package com.trillionloans.lms.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Waive Charge request body")
public class WaiveChargeRequest {
  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[WaiveChargeRequest] Invalid chargeWaiverDate. Use dd-mm-yyyy format")
  private String chargeWaiverDate;

  private String locale;
  private String dateFormat;
}
