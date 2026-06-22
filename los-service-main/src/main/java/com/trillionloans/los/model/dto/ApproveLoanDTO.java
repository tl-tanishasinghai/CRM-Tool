package com.trillionloans.los.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Approve Loan DTO")
public class ApproveLoanDTO {
  private String approvedDate;
  private String dateFormat;
}
