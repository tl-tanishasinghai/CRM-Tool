package com.trillionloans.los.model.response;

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
public class MessageResponseDTO {
  private String developerMessage;
  private String defaultUserMessage;
}
