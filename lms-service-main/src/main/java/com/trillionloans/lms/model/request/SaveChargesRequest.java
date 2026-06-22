package com.trillionloans.lms.model.request;

import com.trillionloans.lms.validation.MaxDigits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Save Penalty request body")
public class SaveChargesRequest {

  @NotNull(message = "[SaveCharges] amount is required")
  @MaxDigits(
      value = 8,
      message = "[SaveCharges] Value exceeds maximum allowed(8) digits for amount field")
  private Double amount;

  @NotNull(message = "[SaveCharges] chargeId is required")
  private Integer chargeId;

  private String dateFormat;
  private String locale;

  @NotEmpty(message = "[SaveCharges] dueDate is required")
  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[SaveChargesRequest] Invalid dueDate. Use dd-mm-yyyy format")
  private String dueDate;

  @Size(
      max = 100,
      message = "[SaveCharges] externalId exceeds the maximum allowed character limit (100)")
  @Pattern(
      regexp = "^(?!\\s*$).+",
      message = "[SaveCharges] externalId must not be blank if provided")
  private String externalId;

  public void setExternalId(String externalId) {
    this.externalId = externalId != null ? externalId.trim() : null;
  }
}
