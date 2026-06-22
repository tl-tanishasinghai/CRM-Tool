package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.trillionloans.los.validation.CustomBooleanDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Consent request body")
public class M2pConsentRequest {
  private String consentKey;
  private String ipAddress;

  @JsonDeserialize(using = CustomBooleanDeserializer.class)
  private Boolean isAccepted;

  private String additionalDetails;
}
