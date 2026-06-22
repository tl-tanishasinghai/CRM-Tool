package com.trillionloans.los.model.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MessagePayloadObject {
  private String externalId;
  private Map<String, Object> request;
  private Map<String, Object> response;
}
