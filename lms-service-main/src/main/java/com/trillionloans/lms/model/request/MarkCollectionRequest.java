package com.trillionloans.lms.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.trillionloans.lms.validation.MaxDigits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Mark Collection request body")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarkCollectionRequest {
  @MaxDigits(
      value = 8,
      message =
          "[MarkCollection] Value exceeds maximum allowed digits(8) for transactionAmount field")
  private Double transactionAmount;

  private String dateFormat;
  private String timeFormat;
  private String locale;

  @NotNull(message = "[MarkCollection] paymentTypeId is required")
  private Long paymentTypeId;

  @Size(
      max = 1000,
      message = "[MarkCollection] Value exceeds maximum allowed character(1000) for note field")
  private String note;

  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[MarkCollection] Invalid transactionDate. Use dd-MM-yyyy format")
  @NotBlank(message = "[MarkCollection] transactionDate is required")
  private String transactionDate;

  @NotBlank(
      message = "[MarkCollection] receiptNumber is mandatory field and cannot be empty or null!")
  private String receiptNumber;

  private String externalId;

  private String uniqueIdentifier;
}
