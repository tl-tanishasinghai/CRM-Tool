package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class M2PApplicationTypeFundlyDTO {

  @JsonProperty("application_type")
  private String applicationType;
}
