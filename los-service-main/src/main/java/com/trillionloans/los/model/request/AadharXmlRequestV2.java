package com.trillionloans.los.model.request;

import com.trillionloans.los.constant.AadhaarXMLType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AadharXmlRequestV2 {

  @NotBlank(message = "[AadhaarXmlRequest] requestString is required")
  private String requestString;

  @NotNull(message = "[AadhaarXmlRequest] AadharXmlType is required")
  private AadhaarXMLType aadhaarXMLType;
}
