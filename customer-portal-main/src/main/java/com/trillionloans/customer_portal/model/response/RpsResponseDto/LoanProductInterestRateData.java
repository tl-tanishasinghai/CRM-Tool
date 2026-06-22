package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanProductInterestRateData {
  private Double maxInterestRatePerPeriod;
  private Double minInterestRatePerPeriod;
}
