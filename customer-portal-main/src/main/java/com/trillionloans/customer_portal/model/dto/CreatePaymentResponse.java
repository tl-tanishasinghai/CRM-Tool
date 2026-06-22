package com.trillionloans.customer_portal.model.dto;

import com.trillionloans.customer_portal.constant.CollectionStatus;
import com.trillionloans.customer_portal.constant.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentResponse {
  private Double amount;

  private String url;

  private String returnUrl;

  private String paymentId;

  private String paymentLinkStatus;

  private String linkCreatedAt;

  private String linkExpiryTime;

  private CollectionStatus collectionStatus;

  private Long collectionId;
}