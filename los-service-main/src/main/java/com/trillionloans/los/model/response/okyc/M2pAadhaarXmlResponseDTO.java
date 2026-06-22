package com.trillionloans.los.model.response.okyc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class M2pAadhaarXmlResponseDTO {
  private String source;
  private String status;
}
