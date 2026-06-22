package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class VerifyOTPResponse {
  private String token;
  private String expiry;
  private Long leadId;
}
