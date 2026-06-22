package com.trillionloans.lms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("credit_line_mark_repayment_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreditLineMarkRepaymentRecord {

  @Id private Long id;

  @Column("line_id")
  private String lineId;

  @Column("drawdown_transaction_id")
  private String emiTransactionId;

  @Column("amount")
  private BigDecimal amount;

  @Column("transaction_id")
  private String transactionId;

  @Column("payment_type_id")
  private Integer paymentTypeId;

  @Column("reference_number")
  private String referenceNumber;

  @Column("transaction_time")
  private Long transactionTime;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
