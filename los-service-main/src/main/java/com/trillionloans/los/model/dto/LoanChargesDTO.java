package com.trillionloans.los.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Charges for a loan application")
public class LoanChargesDTO {
  @NotNull(message = "[charges] chargeId is required")
  @Positive(message = "[charges] chargeId should be an positive integer")
  private Integer chargeId;

  @PositiveOrZero(message = "[charges] amount cannot be negative")
  private Double amount;

  private Boolean isAmountNonEditable;
  private Boolean isMandatory;
}
