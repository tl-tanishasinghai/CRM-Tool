package com.trillionloans.los.model.dto;

import com.trillionloans.los.model.dto.internal.ProductControl;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BreRequest {
  private Object requestBody;
  private List<ProductControl.Flow> breFlows;
  private String productCode;
  private String partnerCode;
}
