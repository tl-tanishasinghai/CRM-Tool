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
public class FaceMatchScores {
  private Double faceMatchScoreFromKarza;
  private Double faceMatchScoreFromTrillion;
  private Double finalFaceMatchScore;
  private String finalFaceMatchStatus;
}
