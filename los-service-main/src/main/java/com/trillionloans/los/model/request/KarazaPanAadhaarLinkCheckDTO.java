package com.trillionloans.los.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KarazaPanAadhaarLinkCheckDTO {
  private String pan;
  private String aadhaarNo;
  private String consent;
}
