package com.trillionloans.los.model.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class NexusRiskBulkRequest {
  private List<String> loanApplicationIds;
}
