package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerformanceDataDTO {
  private String totalOutstanding;
  private String maxDpd;
  private Integer maxDpdOnActiveLoans;
  private Integer dpd30PlusCountOnClosedLoans;
  private Object loans;
}
