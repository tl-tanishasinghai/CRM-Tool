package com.trillionloans.los.model.response.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class M2pDedupeResponseDTO {
  private List<ClientDedupeItemDTO> clientDedupeResponseList;

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  public static class ClientDedupeItemDTO {
    private Integer id;
    private String displayName;
  }
}
