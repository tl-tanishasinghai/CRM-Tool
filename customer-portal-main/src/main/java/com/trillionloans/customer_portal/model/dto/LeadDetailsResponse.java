package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigInteger;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadDetailsResponse {
  private BigInteger leadId;
  private String name;
  private String dateOfBirth;
  private String mobileNo;
  private String email;
  private String ucic;
  private String landmark;
  private String address;
  private String panNumber;
  private String loanAccounts;
}
