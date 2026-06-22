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
public class LatestCollectionResponse {
  private Long latestCollectionId;
  private String amount;
  private PaymentStatus paymentStatus;
  private CollectionStatus collectionStatus;
}