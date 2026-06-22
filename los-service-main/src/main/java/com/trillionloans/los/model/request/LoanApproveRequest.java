package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Loan Approve request body")
public class LoanApproveRequest {
  @NotBlank(message = "[loanApproveRequest] approvedDate is required")
  @Pattern(
      regexp = "^(0[1-9]|[1-2]\\d|3[0-1])-(0[1-9]|1[0-2])-\\d{4}$",
      message = "[loanApproveRequest] Invalid approvedDate. Use dd-mm-yyyy format")
  private String approvedDate;
}
