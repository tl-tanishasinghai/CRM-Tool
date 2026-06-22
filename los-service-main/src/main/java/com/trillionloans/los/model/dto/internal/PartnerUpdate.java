package com.trillionloans.los.model.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "update partner details")
public class PartnerUpdate {

  private String partnerName;
  private String productCode;
  private String productName;
  private String productType;
  private String status;
}
