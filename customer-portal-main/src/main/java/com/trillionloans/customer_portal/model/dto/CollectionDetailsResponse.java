package com.trillionloans.customer_portal.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CollectionDetailsResponse {
  private String losProductKey;
  private Boolean isDirectPaymentEnabled;
  private Double netForeclosureAmount;
  private Double currentDueAmount;
  private Double nextDueAmount;
  private List<String> enabledCollections;
}