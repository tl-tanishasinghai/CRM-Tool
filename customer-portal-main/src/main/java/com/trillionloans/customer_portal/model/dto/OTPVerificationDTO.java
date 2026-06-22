package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OTPVerificationDTO {
  private String mobileNumber;
  private String dateOfBirth;
  private String panLast4Digits;
  private String otp;
}
