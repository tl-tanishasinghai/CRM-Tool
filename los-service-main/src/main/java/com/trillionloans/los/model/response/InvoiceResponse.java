package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class InvoiceResponse {
  private Long id;
  private BigDecimal amount;
  private String invoiceNumber;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate invoiceDate;

  private Object metadata;
}
