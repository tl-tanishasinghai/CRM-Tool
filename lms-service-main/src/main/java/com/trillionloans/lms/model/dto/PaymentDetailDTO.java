package com.trillionloans.lms.model.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PaymentDetailDTO {
  private long paymentTypeId;

  @Size(
      max = 20,
      message =
          "[PaymentDetail] Value exceeds maximum allowed characters(20) for checkNumber field")
  private String checkNumber;

  @Size(
      max = 20,
      message =
          "[PaymentDetail] Value exceeds maximum allowed characters(20) for routingCode field")
  private String routingCode;

  @Size(
      max = 20,
      message =
          "[PaymentDetail] Value exceeds maximum allowed characters(20) for receiptNumber field")
  private String receiptNumber;

  @Size(
      max = 20,
      message = "[PaymentDetail] Value exceeds maximum allowed characters(20) for bankNumber field")
  private String bankNumber;

  private String remark;
}
