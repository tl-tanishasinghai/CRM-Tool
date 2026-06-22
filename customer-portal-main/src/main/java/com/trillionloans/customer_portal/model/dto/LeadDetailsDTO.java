package com.trillionloans.customer_portal.model.dto;

import java.math.BigInteger;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeadDetailsDTO {
  private BigInteger leadId;
  private String name;
  private String dateOfBirth;
  private Integer age;
  private String address;
  private String email;
  private String mobileNo;
  private String ucic;
  private String panNumber;
  private List<String> loanAccounts;
}
