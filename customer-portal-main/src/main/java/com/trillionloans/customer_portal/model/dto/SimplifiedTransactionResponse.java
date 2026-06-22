package com.trillionloans.customer_portal.model.dto;

import java.util.List;
import lombok.*;

@Builder
@Data
public class SimplifiedTransactionResponse {

  private List<Transaction> transactions;
  private LatestCollectionResponse latestCollectionDetails;

  @Builder
  @Data
  public static class Transaction {
    private Double amount;
    private String date;
    private String value;
  }
}
