package com.trillionloans.los.model.response.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class M2pClientCreationResponseDTO {
  private Integer clientId;
}
