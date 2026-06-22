package com.trillionloans.lms.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PartRepaymentRequestDTO {
  @NotNull(message = "[PartRepaymentRequest] amount is mandatory field and cannot be null!")
  private Double amount;

  @NotBlank(
      message =
          "[PartRepaymentRequest] partPaymentDate is mandatory field and cannot be empty or null!")
  private String partPaymentDate;

  @Size(max = 200, message = "[PartRepaymentRequest] Notes cannot exceed 200 characters")
  private String notes;

  @NotNull(message = "[PartRepaymentRequest] paymentDetails is mandatory field and cannot be null!")
  @Valid
  private PaymentDetails paymentDetails;

  @Size(
      max = 100,
      message =
          "[PartRepaymentRequest] Value exceeds maximum allowed character(100) for externalId"
              + " field")
  private String externalId;

  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @Setter
  @Builder
  public static class PaymentDetails {
    private int paymentTypeId;

    @NotBlank(
        message =
            "[PartRepaymentRequest] receiptNumber is mandatory field and cannot be empty or null!")
    private String receiptNumber;
  }
}
