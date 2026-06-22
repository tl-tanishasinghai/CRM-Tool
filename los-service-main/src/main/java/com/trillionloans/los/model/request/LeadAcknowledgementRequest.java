package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Lead Acknowledgement request body")
public class LeadAcknowledgementRequest {

  @Size(
      max = 200,
      message = "[LeadAcknowledgementRequest] processName should be under 200 characters")
  private String processName;
}
