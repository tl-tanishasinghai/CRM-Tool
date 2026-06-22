package com.trillionloans.los.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PanAadhaarLinkStatusDataTableDTO {
  private String pan;
  private String adhaar;
  private Boolean linked;
  private String locale;
  private String dateFormat;
}
