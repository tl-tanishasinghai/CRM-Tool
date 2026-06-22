package com.trillionloans.los.model.response.creditline;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawdownBreResponse {
  private BigDecimal limit;
  private Integer tenure;
  private BigDecimal pf;
  private BigDecimal roi;
  private String date;
  private Eligibility status;

  public enum Eligibility {
    ELIGIBLE,
    NOT_ELIGIBLE
  }
}
