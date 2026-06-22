package com.trillionloans.los.model.request;

import com.trillionloans.los.model.dto.AssociationsDTO;
import com.trillionloans.los.model.dto.LoanApplicationTermsDTO;
import com.trillionloans.los.model.dto.LoanChargesDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Loan creation request body")
public class LoanApplication {
  private Integer loanPurposeId;
  private Integer loanOfficerId;
  private Integer sourcingChannelId;

  @NotNull(message = "[loanApplication] amount is required")
  @PositiveOrZero(message = "[loanApplication] amount cannot be negative")
  private Double amount;

  private String losProductKey;
  private AssociationsDTO associations;

  @Valid
  @NotNull(message = "[loanApplication] leadApplicationTerms is required")
  private LoanApplicationTermsDTO leadApplicationTerms;

  @Valid private List<LoanChargesDTO> charges;

  @Size(max = 100, message = "[loanApplication] externalId should be under 100 characters")
  //  @Pattern(
  //      regexp = "^[A-Za-z0-9 _:-]+$",
  //      message = "[loanApplication] externalId contains invalid characters")
  private String externalId;

  private Boolean isTopup;
  private List<Integer> loanIdToClose;

  @Schema(
      description = "Miscellaneous details as key-value pairs",
      example = "{\"detail1\": \"value1\", \"detail2\": \"value2\"}")
  private Map<String, String> miscellaneousDetails;
}
