package com.trillionloans.los.model.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocIdDTO {
  @JsonProperty("id")
  String id;
}
