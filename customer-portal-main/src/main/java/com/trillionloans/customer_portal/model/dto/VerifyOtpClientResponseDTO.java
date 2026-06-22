package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyOtpClientResponseDTO {
  private Long leadId;
}
