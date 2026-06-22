package com.trillionloans.customer_portal.model.dto;

import com.trillionloans.customer_portal.constant.CollectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CreatePaymentRequest {
  @NotNull(message = "[CreatePayment] collectionType is required")
  private CollectionType collectionType;

  @Pattern(
    regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
    message = "[CreatePayment] Invalid nextDueDate. Use dd-mm-yyyy format")
  private String nextDueDate;

  @NotBlank(message = "[CreatePayment] productCode is required")
  private String productCode;
}