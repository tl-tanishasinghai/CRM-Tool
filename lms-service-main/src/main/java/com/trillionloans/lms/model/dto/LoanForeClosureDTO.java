package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Loan foreClosure data")
public class LoanForeClosureDTO {
  private String note;
  private int preClosureReasonId;

  @NotBlank(message = "[loanForeClosure] transactionAmount is required")
  @Pattern(
      regexp = "\\d+(\\.\\d{1,2})?",
      message =
          "[loanForeClosure] only digits(or proper decimal value) allowed in transactionAmount")
  @Size(
      max = 10,
      message =
          "[loanForeClosure] value exceeds maximum allowed digits(10) for transactionAmount field")
  private String transactionAmount;

  private String transactionDate;
  private int paymentTypeId;
  private String interestWaiverAmount;
  private String receiptNumber;
  private List<Object> chargeDiscountDetails;
  private List<Object> waiveCharges;
}
