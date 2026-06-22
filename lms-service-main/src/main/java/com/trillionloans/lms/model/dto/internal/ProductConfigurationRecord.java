package com.trillionloans.lms.model.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ProductConfigurationRecord {
  private ProductControl productControl;
  private String partnerCode;
}
