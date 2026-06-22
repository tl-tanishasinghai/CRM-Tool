package com.trillionloans.lms.model.request;

import com.trillionloans.lms.model.dto.PaymentDetailDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Collect charge request body")
public class CollectChargeRequest {
  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[CollectChargeRequest] Invalid transactionDate. Use dd-mm-yyyy format")
  private String transactionDate;

  private Double transactionAmount;
  @Valid private PaymentDetailDTO paymentDetail;
  private String locale;
  private String dateFormat;
}
