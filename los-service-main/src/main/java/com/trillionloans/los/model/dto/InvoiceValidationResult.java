package com.trillionloans.los.model.dto;

import com.trillionloans.los.model.entity.Invoice;
import com.trillionloans.los.model.request.InvoiceData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class InvoiceValidationResult {
  private Invoice invoiceEntity;
  private boolean exists;
  private boolean hasUsedInDrawdownBefore;

  /** Index of the corresponding invoice in the original request list. */
  private int invoiceIndex;

  private InvoiceData invoiceData;
}
