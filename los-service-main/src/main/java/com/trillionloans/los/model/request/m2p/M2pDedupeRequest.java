package com.trillionloans.los.model.request.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class M2pDedupeRequest {
  private ClientData clientData;
  private List<ClientIdentifierData> clientIdentifierData;

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  public static class ClientData {
    private String mobileNo;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  public static class ClientIdentifierData {
    private Integer documentTypeId;
    private String documentKey;
  }
}
