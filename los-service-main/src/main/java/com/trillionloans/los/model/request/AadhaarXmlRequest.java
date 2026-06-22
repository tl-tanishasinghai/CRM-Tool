package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Aadhaar XMl request body")
public class AadhaarXmlRequest {
  @NotBlank(message = "[AadhaarXmlRequest] requestString is required")
  private String requestString;
}
