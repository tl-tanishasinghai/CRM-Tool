package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ClientDependentDTO {

  @JsonProperty("dependent_name")
  String dependentName;

  @JsonProperty("creation_date")
  String creationDate;

  @JsonProperty("relationship")
  String relationship;
}
