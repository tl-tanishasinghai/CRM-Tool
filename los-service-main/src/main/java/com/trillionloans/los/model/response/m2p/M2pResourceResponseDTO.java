package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class M2pResourceResponseDTO {

  private Long resourceId;
  private String resourceIdentifier;
}
