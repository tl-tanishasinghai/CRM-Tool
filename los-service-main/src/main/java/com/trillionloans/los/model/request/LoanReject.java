package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Loan rejection request body")
public class LoanReject {
  @NotNull(message = "[rejectLoan] reasonCode is required")
  private Integer reasonCode;

  @Size(max = 100, message = "[rejectLoan] description should be under 100 characters")
  private String description;
}
