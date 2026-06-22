package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trillionloans.los.model.entity.Drawdown;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawdownInternalResponse {
  private String anchorId;
  private String partnerId;

  private String drawdownId;
  private Drawdown drawdown;

  private List<String> invoiceIds;
  private List<InvoiceResponse> invoices;
}
