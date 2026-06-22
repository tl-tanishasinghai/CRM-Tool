package com.trillionloans.los.model.request;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * generic request for reverse feed rejection. used for both line-based products (drawdown
 * rejection) and non-line-based products (loan rejection).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReverseFeedRejectionRequest {

  /** transaction date from the reverse feed file */
  private LocalDateTime transactionDate;

  /** rejection notes/reason */
  private String rejectionNotes;
}
