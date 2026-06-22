package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class M2pClientIdentifierDetailsDTO {
  private String documentType;
  private String documentKey;
  private String issueDate;
  private String expiryDate;
}
