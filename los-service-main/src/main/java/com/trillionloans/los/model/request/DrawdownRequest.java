package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawdownRequest {

  private String anchorId;

  private String partnerId;

  /**
   * Client-provided idempotency key. When provided, duplicate requests return the existing
   * drawdown.
   */
  private String externalId;

  private List<InvoiceData> invoiceData;

  @NotNull(message = "Drawdown data is mandatory")
  @Valid
  private DrawdownData drawdownData;
}
