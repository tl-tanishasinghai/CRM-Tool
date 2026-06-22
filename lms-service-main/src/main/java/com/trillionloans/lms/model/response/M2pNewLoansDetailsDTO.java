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
public class M2pNewLoansDetailsDTO {
  private String customerName;
  private String mobileNo;
}
