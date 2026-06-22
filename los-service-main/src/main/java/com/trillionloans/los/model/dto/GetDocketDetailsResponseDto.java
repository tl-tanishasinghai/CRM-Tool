package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class GetDocketDetailsResponseDto {
  private String clientId;
  private String productId;
  private String applicantName;
  private String applicantAddress;
  private String pan;
  private String mobileNo;
  private Double processingFees;
}
