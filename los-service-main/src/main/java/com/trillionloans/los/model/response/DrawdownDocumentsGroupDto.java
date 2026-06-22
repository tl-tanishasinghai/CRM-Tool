package com.trillionloans.los.model.response;

import java.util.List;
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
public class DrawdownDocumentsGroupDto {

  private Long drawdownId;

  /** Drawdown-level agreement / attachment references. */
  private List<StoredDrawdownDocumentDto> drawdownDocuments;

  /** Invoice attachment references for invoices linked to this drawdown. */
  private List<InvoiceDrawdownDocumentsGroupDto> invoices;
}
