package com.trillionloans.los.model.dto.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.trillionloans.los.model.dto.GetDocketDetailsResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocDetailRequest {
  private GetDocketDetailsResponseDto docketDetailsResponseDto;
  private JsonNode repaymentSchedule;
}
