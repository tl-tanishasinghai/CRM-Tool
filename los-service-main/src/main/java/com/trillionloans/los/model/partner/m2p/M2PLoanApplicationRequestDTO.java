package com.trillionloans.los.model.partner.m2p;

import com.trillionloans.los.model.dto.LoanChargesDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class M2PLoanApplicationRequestDTO {
  private final Integer loanOfficerId;
  private final Double amount;
  private final String losProductKey;
  private Integer sourcingChannelId;
  private M2pLoanApplicationTermsDTO leadApplicationTerms;
  private List<LoanChargesDTO> charges;
  private String externalIdOne;
  private M2pLoanApplicationAssociationsDTO associations;
}
