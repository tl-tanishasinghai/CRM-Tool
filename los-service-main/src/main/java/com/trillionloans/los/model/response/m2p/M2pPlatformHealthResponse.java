package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class M2pPlatformHealthResponse {

  private M2pPlatformHealthResult result;
  private Integer code;
  private String status;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class M2pPlatformHealthResult {
    private String serverStatus;
    private Map<String, String> tenantStatus;
    private String redisStatus;

  }
}
