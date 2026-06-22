package com.trillionloans.los.model.request.ckyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class KarzaCkycSearchRequest {

  private String pan;
  @Builder.Default private String fatherName = "Y";
  @Builder.Default private String consent = "Y";
}
