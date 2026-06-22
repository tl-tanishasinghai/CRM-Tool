package com.trillionloans.los.model.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticAddressSimilarityRequest {
  private String address1;
  private String address2;
}
