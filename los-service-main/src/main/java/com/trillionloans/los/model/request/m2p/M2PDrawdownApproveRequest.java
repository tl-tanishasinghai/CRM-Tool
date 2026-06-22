package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for M2P drawdown approval API. Endpoint: POST
 * {BaseUrl}/{lineId}/transactions/{transactionId}/approve
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class M2PDrawdownApproveRequest {

  /** Transaction time in epoch milliseconds */
  private Long transactionTime;

  /** Notes for the approval */
  private String notes;

  /** Payment details for the transaction */
  private PaymentDetails paymentDetails;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PaymentDetails {
    /** Payment type (e.g., "online transfer") */
    private String paymentType;

    /** Payment reference number */
    private String referenceNumber;
  }
}
