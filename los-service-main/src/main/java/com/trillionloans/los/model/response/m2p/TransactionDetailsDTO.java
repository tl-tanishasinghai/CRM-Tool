package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDetailsDTO {

  private Long transactionTime;

  private String transactionIdentifier;

  private String productName;

  private BigDecimal amount;

  private Long loanId;

  private String loanAccountNo;

  private BigDecimal emiAmount;

  private BigDecimal totalOutstanding;

  private String status;

  private String termFrequencyPeriodEnum;

  private Integer termFrequency;

  @JsonProperty("isNpa")
  private Boolean isNpa;

  private String purchaseTransactionIdentifier;

  private String externalId;
}
