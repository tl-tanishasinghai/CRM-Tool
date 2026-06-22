package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing additional details data for a loan in the collection
 * module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Loan Additional Details Data DTO for collection module")
public class LoanAdditionalDetailsDataDTO {

  private Long id;
  private Long loanId;
  private boolean isFldg;
  private GeneralResponseDTO additionalInterestComputationType;
  private int courseOnInterestPayment;
  private int additionalGraceOnInterestPayment;
  private int courseOnPrincipalPayment;
  private int additionalGraceOnPrincipalPayment;
}
