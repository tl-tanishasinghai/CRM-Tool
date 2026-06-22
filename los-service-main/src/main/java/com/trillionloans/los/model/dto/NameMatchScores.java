package com.trillionloans.los.model.dto;

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
public class NameMatchScores {
  private Double nameMatchScoreFromKarza;
  private Double nameMatchScoreFromTrillion;
  private Double finalNameMatchScore;
  private String finalNameMatchStatus;
}
