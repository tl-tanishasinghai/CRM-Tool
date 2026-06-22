package com.trillionloans.los.model.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.trillionloans.los.validation.CustomBooleanDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@Valid
public class ConsentRequest {

  @NotBlank(message = "[ConsentRequest] consentKey is required")
  private String consentKey;

  private String ipAddress;

  @JsonDeserialize(using = CustomBooleanDeserializer.class)
  private Boolean isAccepted;

  private String additionalDetails;

  private String dateTime;
}
