package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class M2pUcicUpdateDTO {
  private String ucic;
  private String locale;
  private String dateFormat;
}
