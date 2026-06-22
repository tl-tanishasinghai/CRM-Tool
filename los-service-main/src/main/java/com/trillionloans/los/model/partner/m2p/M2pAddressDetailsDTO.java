package com.trillionloans.los.model.partner.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class M2pAddressDetailsDTO {
  private List<String> addressType;
  private String addressLineOne;
  private String addressLineTwo;
  private String landmark;
  private String postalCode;
  private String ownershipType;
}
