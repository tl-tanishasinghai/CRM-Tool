package com.trillionloans.los.model.dto;

import com.trillionloans.los.constant.AadhaarXMLType;
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
public class AadhaarXmlValidationDTO {
  private AadhaarXmlDetailsDTO clientXmlDetailsDTO;
  private String leadId;
  private String loanId;
  private AadhaarXMLType aadhaarXMLType;
}
