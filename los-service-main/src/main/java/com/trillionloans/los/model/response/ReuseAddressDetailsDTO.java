package com.trillionloans.los.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ReuseAddressDetailsDTO {

  private String addressType;
  private String addressLine1;
  private String addressLine2;
  private String pincode;
  private String city;
}
