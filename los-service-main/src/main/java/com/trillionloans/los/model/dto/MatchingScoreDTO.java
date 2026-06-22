package com.trillionloans.los.model.dto;

import com.trillionloans.los.constant.KycValidationVendors;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchingScoreDTO {

  private Map<KycValidationVendors, MatchingScore> matchingScores;

  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MatchingScore {
    private Double score;
    private KycValidationVendors vendor;
  }
}
