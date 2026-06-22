package com.trillionloans.customer_portal.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileNumberAttributes {

  private String leadId;
  private List<String> loanAccountNumbers;
  private List<String> loanApplicationIds;
}
