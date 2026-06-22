package com.trillionloans.los.model.response.ckyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class KarzaCkycSearchResponse {

  private String requestId;
  private String statusCode;
  private Result result;

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Data
  public static class Result {
    private String name;
    private String fatherName;
  }
}
