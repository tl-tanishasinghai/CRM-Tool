package com.trillionloans.lms.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDetailResponse {

  private Status status;
  private String clientId;
  private List<Transaction> transactions;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Transaction {
    private Long id;
    private PaymentDetailData paymentDetailData;
    private Double amount;
    private String externalId;
    private Type type;
    private List<Integer> date;
    private Boolean manuallyReversed;
    private TxnValueDateStatus txnValueDateStatus;
  }

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PaymentDetailData {
    private Long id;
    private String receiptNumber;
  }

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Status {
    private int id;
    private String code;
    private String value;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Type {
    @JsonProperty("id")
    private Long id;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TxnValueDateStatus {
    private String value;
  }
}
