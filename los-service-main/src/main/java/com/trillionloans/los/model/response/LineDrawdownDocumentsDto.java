package com.trillionloans.los.model.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for {@code GET /api/v1/drawdown/{lineId}/documents}. Nested DTOs only; no entity types
 * are exposed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineDrawdownDocumentsDto {

  private String lineId;

  /** One entry when {@code drawdownId} query param is set; otherwise all drawdowns on the line. */
  private List<DrawdownDocumentsGroupDto> drawdowns;
}
