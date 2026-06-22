package com.trillionloans.los.model.request.creditline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawdownAlternateData {
  private String pincode;
  private Integer businessVintage;
  private String retailerName;
  private String distributorName;
  private String typeOfGoods;
  private String invoiceAttachment;
}
