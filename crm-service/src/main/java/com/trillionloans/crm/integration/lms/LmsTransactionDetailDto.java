package com.trillionloans.crm.integration.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LmsTransactionDetailDto(List<Transaction> transactions) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Transaction(
      Double amount,
      List<Integer> date,
      Type type,
      Boolean manuallyReversed,
      TxnValueDateStatus txnValueDateStatus) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Type(@JsonProperty("id") Long id) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TxnValueDateStatus(String value) {}
}
