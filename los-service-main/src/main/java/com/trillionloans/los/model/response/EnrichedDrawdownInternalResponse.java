package com.trillionloans.los.model.response;

import com.trillionloans.los.model.response.m2p.M2PDrawdownResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedDrawdownInternalResponse {
  private DrawdownInternalResponse drawdownInternalResponse;
  private M2PDrawdownResponse m2PDrawdownResponse;
}
