package com.trillionloans.los.model.response.m2p;

import com.trillionloans.los.model.dto.LoanApplicationTermsDTO;
import com.trillionloans.los.model.dto.LoanChargesDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class M2PTopUpLoanApplicationRequestDTO {

  @NotNull(message = "[loanApplication] clientId is required")
  private Integer clientId;

  private String submittedOnDate;
  private String locale;
  private String dateFormat;
  private Boolean isTopup;

  @Valid
  @NotEmpty(message = "[loanApplication] loanIdToClose is required")
  private List<Integer> loanIdToClose;

  @NotNull(message = "[loanApplication] amount is required")
  private Double amount;

  @Valid private List<LoanChargesDTO> charges;
  private String loanSubType;

  @NotNull(message = "[loanApplication] losProductKey is required")
  private String losProductKey;

  @Valid
  @NotNull(message = "[loanApplication] leadApplicationTerms is required")
  private LoanApplicationTermsDTO leadApplicationTerms;
}
