package com.trillionloans.los.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KarzaFaceSimilarityRequest {

  private String image1B64;
  private String image2B64;
  private String url1;
  private String url2;

  private Boolean getNumberOfFaces;

  private ClientData clientData;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ClientData {
    private String caseId;
  }
}
