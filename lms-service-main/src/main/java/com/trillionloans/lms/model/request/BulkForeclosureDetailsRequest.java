package com.trillionloans.lms.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
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
public class BulkForeclosureDetailsRequest {

  @NotNull(message = "Transaction IDs list cannot be null")
  @Size(min = 1, max = 100, message = "Transaction IDs list must contain 1-100 items")
  private List<String> transactionIds;

  private String transactionDate;
}
