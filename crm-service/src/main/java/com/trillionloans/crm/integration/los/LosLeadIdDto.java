package com.trillionloans.crm.integration.los;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LosLeadIdDto(
    @JsonProperty("id") Long leadId, @JsonProperty("entityId") Long entityId) {

  public String resolvedLeadId() {
    if (leadId != null) {
      return String.valueOf(leadId);
    }
    if (entityId != null) {
      return String.valueOf(entityId);
    }
    return null;
  }
}
