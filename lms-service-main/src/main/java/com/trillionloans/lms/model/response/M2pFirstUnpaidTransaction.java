package com.trillionloans.lms.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class M2pFirstUnpaidTransaction {
  private Long loanId;
  private String dueDate;
  private Long installmentNo;
  private Double totalOutstanding;
}
