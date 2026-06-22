package com.trillionloans.los.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditLinePartnerEntity {
  private String leadId;
  private String partnerId;
  private String lineId;
}
