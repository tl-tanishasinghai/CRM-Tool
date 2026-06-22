package com.trillionloans.los.model.request;

import com.trillionloans.los.validation.MaxDigits;
import com.trillionloans.los.validation.ValidDouble;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Top up data table request")
public class TopupDataRequest {

  @Size(
      max = 100,
      message =
          "[TopupDataRequest] Value exceeds maximum allowed characters(100) for sourcingChannel"
              + " field")
  @NotBlank(message = "[TopupDataRequest] sourcingChannel is required")
  @Pattern(regexp = "^[A-Za-z0-9 _(),@-]+$", message = "invalid characters in sourcing channel")
  private String sourcingChannel;

  @Size(
      max = 100,
      message =
          "[TopupDataRequest] Value exceeds maximum allowed characters(100) for topupId field")
  @NotBlank(message = "[TopupDataRequest] topupId is required")
  @Pattern(regexp = "^[A-Za-z0-9]+$", message = "only alphabets and numbers are allowed")
  private String topupId;

  @PositiveOrZero(message = "[TopupDataRequest] amount cannot be negative")
  @MaxDigits(
      value = 10,
      message =
          "[TopupDataRequest] Value exceeds maximum allowed digits(10) for outstandingAmount field")
  @NotNull(message = "[TopupDataRequest] outstandingAmount is required")
  @ValidDouble(
      message =
          "[TopupDataRequest] outstandingAmount should be a number and only 2 digits permissible"
              + " after decimal",
      decimalDigits = 2)
  private Double outstandingAmount;
}
