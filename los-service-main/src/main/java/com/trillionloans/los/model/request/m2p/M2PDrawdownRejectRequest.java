package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for M2P drawdown rejection API. Endpoint: POST
 * {BaseUrl}/{lineId}/transactions/{transactionId}/reject
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class M2PDrawdownRejectRequest {

  /** Transaction time in epoch milliseconds */
  private Long transactionTime;

  /** Notes/reason for the rejection */
  private String notes;
}
