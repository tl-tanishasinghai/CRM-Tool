package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Currency {
  private String displayLabel;
  private String code;
  private Double decimalPlaces;
  private String nameCode;
  private Double inMultiplesOf;
  private String name;
}
