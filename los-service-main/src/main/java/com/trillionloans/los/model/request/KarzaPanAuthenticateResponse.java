package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
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
public class KarzaPanAuthenticateResponse {

  @JsonProperty("status-code")
  @SerializedName("status-code")
  private int statusCode;

  @JsonProperty("request_id")
  @SerializedName("request_id")
  private String requestId;

  private Result result;

  private ClientData clientData;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class Result {
    private String name;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ClientData {
    private String caseId;
  }
}
