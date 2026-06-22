package com.trillionloans.lms.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class M2pLanDetails {
  private Long loanId;
  private Long clientId;
  private Long status;
  private String losProductKey;
  private Long leadId;
  private String name;
  private String mobileNumber;
}
