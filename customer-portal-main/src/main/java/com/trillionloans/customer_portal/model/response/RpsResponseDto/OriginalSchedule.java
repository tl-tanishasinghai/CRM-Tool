package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OriginalSchedule {
  private Integer loanTermInDays;
  private List<PeriodsItem> periods;
}
