package com.trillionloans.los.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KarzaFaceSimilarityResponse {
  private String requestId;
  private Result result;
  private Integer statusCode;
  private ClientData clientData;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class Result {
    private String match;
    private Double matchScore;
    private String reviewNeeded;
    private Double confidence;
    private FaceProperties faceProperties;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class FaceProperties {
    private ImageFaceData image1;
    private ImageFaceData image2;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ImageFaceData {
    private Integer numberOfFaces;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ClientData {
    private String caseId;
  }
}
