package com.trillionloans.los.model.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.trillionloans.los.validation.CustomBooleanDeserializer;
import com.trillionloans.los.validation.MaxDigits;
import com.trillionloans.los.validation.ValidDouble;
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
@Schema(description = "Save Charge request")
public class SaveChargeRequest {
  @NotNull(message = "[SaveChargeRequest] chargeId is required")
  @Positive(message = "[SaveChargeRequest] chargeId should be an positive integer")
  private Integer chargeId;

  @NotNull(message = "[SaveChargeRequest] amount is required")
  @MaxDigits(
      value = 10,
      message = "[SaveChargeRequest] Value exceeds maximum allowed digits(10) for amount field")
  @ValidDouble(
      message =
          "[SaveChargeRequest] amount should be a number and only 2 digits permissible after"
              + " decimal",
      decimalDigits = 2)
  @PositiveOrZero(message = "[SaveChargeRequest] amount cannot be negative")
  private Double amount;

  @JsonDeserialize(using = CustomBooleanDeserializer.class)
  private Boolean isAmountNonEditable;

  @JsonDeserialize(using = CustomBooleanDeserializer.class)
  private Boolean isMandatory;
}
