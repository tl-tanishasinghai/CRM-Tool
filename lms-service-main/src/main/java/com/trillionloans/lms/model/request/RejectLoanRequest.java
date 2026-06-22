package com.trillionloans.lms.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Loan reject request body")
public class RejectLoanRequest {
  private String rejectedOnDate;
  private String locale;
  private String dateFormat;
}
