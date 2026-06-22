package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Timeline {
  private List<Integer> expectedDisbursementDate;
}
