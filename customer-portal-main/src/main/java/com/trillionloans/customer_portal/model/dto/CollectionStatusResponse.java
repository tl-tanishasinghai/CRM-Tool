package com.trillionloans.customer_portal.model.dto;

import com.trillionloans.customer_portal.constant.CollectionStatus;
import com.trillionloans.customer_portal.constant.CollectionType;
import com.trillionloans.customer_portal.constant.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CollectionStatusResponse {
  private Long collectionId;
  private String loanId;
  private CollectionType collectionType;
  private CollectionStatus collectionStatus;
  private String collectionCompletedAt;
  private String paymentId;
  private PaymentStatus paymentStatus;
  private String paymentCompletedAt;
}