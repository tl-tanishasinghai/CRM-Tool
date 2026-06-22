package com.trillionloans.los.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("drawdown_invoice_mappings")
public class DrawdownInvoiceMapping {

  @Id private Long id;

  @Column("drawdown_id")
  private Long drawdownId;

  @Column("invoice_id")
  private Long invoiceId;
}
